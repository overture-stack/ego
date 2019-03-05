package bio.overture.ego.service;

import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.entity.AbstractPermission;
import bio.overture.ego.model.entity.NameableEntity;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.repository.PermissionRepository;
import bio.overture.ego.utils.PermissionRequestAnalyzer.PermissionAnalysis;
import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static bio.overture.ego.model.dto.Scope.createScope;
import static bio.overture.ego.model.exceptions.MalformedRequestException.checkMalformedRequest;
import static bio.overture.ego.model.exceptions.NotFoundException.buildNotFoundException;
import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.model.exceptions.UniqueViolationException.checkUnique;
import static bio.overture.ego.utils.CollectionUtils.difference;
import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static bio.overture.ego.utils.Joiners.COMMA;
import static bio.overture.ego.utils.PermissionRequestAnalyzer.analyze;
import static com.google.common.collect.Maps.uniqueIndex;
import static com.gs.collections.impl.factory.Sets.intersect;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Transactional
public abstract class AbstractPermissionService<
        O extends NameableEntity<UUID>, P extends AbstractPermission<O>>
    extends AbstractBaseService<P, UUID> {

  private final BaseService<Policy, UUID> policyBaseService;
  private final BaseService<O, UUID> ownerBaseService;
  private final PermissionRepository<O, P> permissionRepository;
  private final Class<O> ownerType;

  public AbstractPermissionService(
      @NonNull Class<O> ownerType,
      @NonNull Class<P> entityType,
      @NonNull BaseService<O, UUID> ownerBaseService,
      @NonNull BaseService<Policy, UUID> policyBaseService,
      @NonNull PermissionRepository<O, P> repository) {
    super(entityType, repository);
    this.permissionRepository = repository;
    this.ownerType = ownerType;
    this.policyBaseService = policyBaseService;
    this.ownerBaseService = ownerBaseService;
  }

  protected abstract Collection<P> getPermissionsForOwner(O owner);

  protected abstract Collection<P> getPermissionsForPolicy(Policy policy);

  public abstract O getOwnerWithRelationships(UUID ownerId);

  public List<PolicyResponse> findByPolicy(UUID policyId) {
    val permissions = ImmutableList.copyOf(permissionRepository.findAllByPolicy_Id(policyId));
    return mapToList(permissions, this::convertToPolicyResponse);
  }

  public Page<P> getPermissions(@NonNull UUID ownerId, @NonNull Pageable pageable) {
    ownerBaseService.checkExistence(ownerId);
    val permissions = ImmutableList.copyOf(permissionRepository.findAllByOwner_Id(ownerId));
    return new PageImpl<>(permissions, pageable, permissions.size());
  }

  public void deleteByPolicyAndOwner(@NonNull UUID policyId, @NonNull UUID ownerId) {
    val perm = getByPolicyAndOwner(policyId, ownerId);
    getRepository().delete(perm);
  }

  public void deletePermissions(
      @NonNull UUID ownerId, @NonNull Collection<UUID> permissionsIdsToDelete) {
    checkMalformedRequest(
        !permissionsIdsToDelete.isEmpty(),
        "Must add at least 1 permission for %s '%s'",
        getOwnerTypeName(),
        ownerId);
    val owner = getOwnerWithRelationships(ownerId);

    val permissions = getPermissionsForOwner(owner);
    val filteredPermissionMap =
        permissions
            .stream()
            .filter(x -> permissionsIdsToDelete.contains(x.getId()))
            .collect(toMap(AbstractPermission::getId, identity()));

    val existingPermissionIds = filteredPermissionMap.keySet();
    val nonExistingPermissionIds = difference(permissionsIdsToDelete, existingPermissionIds);
    checkNotFound(
        nonExistingPermissionIds.isEmpty(),
        "The following %s ids for the %s '%s' were not found",
        getEntityTypeName(),
        getOwnerTypeName(),
        COMMA.join(nonExistingPermissionIds));
    val permissionsToRemove = filteredPermissionMap.values();

    disassociatePermissions(permissionsToRemove);
    getRepository().deleteAll(permissionsToRemove);
  }

  /**
   * Adds permissions for the supplied owner. The input permissionRequests are sanitized and then
   * used to create new permissions and update existing ones.
   *
   * @param ownerId permissionRequests will be applied to the owner with this ownerId
   * @param permissionRequests permission to be created or updated
   * @return owner with new and updated permissions
   */
  public O addPermissions(
      @NonNull UUID ownerId, @NonNull List<PermissionRequest> permissionRequests) {
    checkMalformedRequest(
        !permissionRequests.isEmpty(),
        "Must add at least 1 permission for %s '%s'",
        getOwnerTypeName(),
        ownerId);

    // Check policies all exist
    policyBaseService.checkExistence(mapToSet(permissionRequests, PermissionRequest::getPolicyId));

    val owner = getOwnerWithRelationships(ownerId);

    // Convert the GroupPermission to PermissionRequests since all permission requests apply to the
    // same owner (the group)
    val existingPermissions = getPermissionsForOwner(owner);
    val existingPermissionRequests =
        mapToSet(existingPermissions, AbstractPermissionService::convertToPermissionRequest);
    val permissionAnalysis = analyze(existingPermissionRequests, permissionRequests);

    // Check there are no unresolvable permission requests
    checkUnique(
        permissionAnalysis.getUnresolvableMap().isEmpty(),
        "Found multiple (%s) PermissionRequests with policyIds that have multiple masks: %s",
        permissionAnalysis.getUnresolvableMap().keySet().size(),
        permissionAnalysis.summarizeUnresolvables());

    // Check that are no permission requests that effectively exist
    checkUnique(
        permissionAnalysis.getDuplicates().isEmpty(),
        "The following permissions already exist for %s '%s': ",
        getOwnerTypeName(),
        ownerId,
        COMMA.join(permissionAnalysis.getDuplicates()));

    return createOrUpdatePermissions(owner, permissionAnalysis);
  }

  private P getByPolicyAndOwner(@NonNull UUID policyId, @NonNull UUID ownerId) {
    return permissionRepository
        .findByPolicy_IdAndOwner_id(policyId, ownerId)
        .orElseThrow(
            () ->
                buildNotFoundException(
                    "%s for policy '%s' and owner '%s' cannot be cannot be found",
                    getEntityTypeName(), policyId, ownerId));
  }

  private String getOwnerTypeName() {
    return ownerType.getSimpleName();
  }

  /**
   * Create or Update the permission for the group based on the supplied analysis
   *
   * @param owner with all its relationships loaded
   * @param permissionAnalysis containing pre-sanitized lists of createable and updateable requests
   */
  private O createOrUpdatePermissions(O owner, PermissionAnalysis permissionAnalysis) {
    val updatedGroup = updateGroupPermissions(owner, permissionAnalysis.getUpdateables());
    return createGroupPermissions(updatedGroup, permissionAnalysis.getCreateables());
  }

  /**
   * Update existing Permissions for an owner with different data while maintaining the same
   * relationships
   *
   * @param owner with all its relationships loaded
   */
  private O updateGroupPermissions(
      O owner, Collection<PermissionRequest> updatePermissionRequests) {
    val existingPermissions = getPermissionsForOwner(owner);
    val existingPermissionIndex = uniqueIndex(existingPermissions, x -> x.getPolicy().getId());

    updatePermissionRequests.forEach(
        p -> {
          val policyId = p.getPolicyId();
          val mask = p.getMask();
          checkNotFound(
              existingPermissionIndex.containsKey(policyId),
              "Could not find existing %s with policyId '%s' for %s '%s'",
              getEntityTypeName(),
              policyId,
              getOwnerTypeName(),
              owner.getId());
          val gp = existingPermissionIndex.get(policyId);
          gp.setAccessLevel(mask);
        });
    return owner;
  }

  /**
   * Create new Permissions for the owner
   *
   * @param owner with all its relationships loaded
   */
  private O createGroupPermissions(
      O owner, Collection<PermissionRequest> createablePermissionRequests) {
    val existingPermissions = getPermissionsForOwner(owner);
    val existingPermissionIndex = uniqueIndex(existingPermissions, x -> x.getPolicy().getId());
    val requestedPolicyIds = mapToSet(createablePermissionRequests, PermissionRequest::getPolicyId);

    // Double check the permissions you are creating dont conflict with whats existing
    val redundantPolicyIds = intersect(requestedPolicyIds, existingPermissionIndex.keySet());
    checkUnique(
        redundantPolicyIds.isEmpty(),
        "%ss with the following policyIds could not be created because "
            + "%ss with those policyIds already exist: %s",
        getEntityTypeName(),
        getEntityTypeName(),
        COMMA.join(redundantPolicyIds));

    val requestedPolicyMap =
        uniqueIndex(policyBaseService.getMany(requestedPolicyIds), Policy::getId);
    createablePermissionRequests.forEach(x -> createGroupPermission(requestedPolicyMap, owner, x));
    return owner;
  }

  @SneakyThrows
  private void createGroupPermission(
      Map<UUID, Policy> policyMap, O owner, PermissionRequest request) {
    val gp = getEntityType().newInstance();
    val policy = policyMap.get(request.getPolicyId());
    gp.setAccessLevel(request.getMask());
    associatePermission(owner, gp);
    associatePermission(policy, gp);
  }

  public static Scope buildScope(@NonNull AbstractPermission permission) {
    return createScope(permission.getPolicy(), permission.getAccessLevel());
  }

  private static PermissionRequest convertToPermissionRequest(AbstractPermission p) {
    return PermissionRequest.builder()
        .mask(p.getAccessLevel())
        .policyId(p.getPolicy().getId())
        .build();
  }

  /** Stateless member methods
   * If these stateless member methods were static, their signature would look ugly with all the generic type bounding.
   * In the interest of more readable code, using member methods is a cleaner approach.
   *
   */
  private PolicyResponse convertToPolicyResponse(@NonNull P p) {
    val name = p.getOwner().getName();
    val id = p.getOwner().getId().toString();
    val mask = p.getAccessLevel();
    return PolicyResponse.builder().name(name).id(id).mask(mask).build();
  }

  /**
   * Disassociates group permissions from its parents
   *
   * @param permissions assumed to be loaded with parents
   */
  public void disassociatePermissions(Collection<P> permissions) {
    permissions.forEach(
        x -> {
          val ownerPermissions = getPermissionsForOwner(x.getOwner());
          ownerPermissions.remove(x);
          val policyPermissions = getPermissionsForPolicy(x.getPolicy());
          policyPermissions.remove(x);
          x.setPolicy(null);
          x.setOwner(null);
        });
  }

  public void associatePermission(@NonNull Policy policy, @NonNull P permission) {
    val policyPermissions = getPermissionsForPolicy(policy);
    policyPermissions.add(permission);
    permission.setPolicy(policy);
  }

  public void associatePermission(@NonNull O owner, @NonNull P permission) {
    val ownerPermissions = getPermissionsForOwner(owner);
    ownerPermissions.add(permission);
    permission.setOwner(owner);
  }
}
