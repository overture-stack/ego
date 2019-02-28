package bio.overture.ego.service;

import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.AbstractPermission;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.repository.GroupPermissionRepository;
import bio.overture.ego.utils.PermissionRequestAnalyzer.PermissionAnalysis;
import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static bio.overture.ego.model.exceptions.MalformedRequestException.checkMalformedRequest;
import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.model.exceptions.UniqueViolationException.checkUnique;
import static bio.overture.ego.utils.CollectionUtils.difference;
import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static bio.overture.ego.utils.Joiners.COMMA;
import static bio.overture.ego.utils.PermissionRequestAnalyzer.analyze;
import static com.google.common.collect.Maps.uniqueIndex;
import static com.gs.collections.impl.factory.Sets.intersect;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
public class GroupPermissionService extends AbstractPermissionService<GroupPermission> {

  /** Dependencies */
  private final GroupService groupService;
  private final PolicyService policyService;

  @Autowired
  public GroupPermissionService(
      @NonNull GroupPermissionRepository repository,
      @NonNull GroupService groupService,
      @NonNull PolicyService policyService) {
    super(GroupPermission.class, repository);
    this.groupService = groupService;
    this.policyService = policyService;
  }

  public void deleteGroupPermissions(
      @NonNull UUID groupId, @NonNull Collection<UUID> permissionsIdsToDelete) {
    checkMalformedRequest(!permissionsIdsToDelete.isEmpty(),
        "Must add at least 1 permission for group '%s'", groupId);
    val group = groupService.getGroupWithRelationships(groupId);

    val filteredPermissionMap = group.getPermissions().stream()
        .filter(x -> permissionsIdsToDelete.contains(x.getId()))
        .collect(toMap(AbstractPermission::getId, identity()));

    val existingPermissionIds = filteredPermissionMap.keySet();
    val nonExistingPermissionIds = difference(permissionsIdsToDelete, existingPermissionIds);
    checkNotFound(nonExistingPermissionIds.isEmpty(),
        "The following GroupPermission ids for the group '%s' were not found",
        COMMA.join(nonExistingPermissionIds));
    val permissionsToRemove = filteredPermissionMap.values();

    disassociateGroupPermissions(permissionsToRemove);
    getRepository().deleteAll(permissionsToRemove);
  }

  /**
   * Adds permissions for the supplied group. The input permissionRequests are sanitized
   * and then used to create new permissions and update existing ones.
   * @param groupId permissionRequests will be applied to the group with this groupId
   * @param permissionRequests permission to be created or updated
   * @return group with new and updated permissions
   */
  public Group addGroupPermissions(
      @NonNull UUID groupId, @NonNull List<PermissionRequest> permissionRequests) {
    checkMalformedRequest(!permissionRequests.isEmpty(),
        "Must add at least 1 permission for group '%s'", groupId);

    // Check policies all exist
    policyService.checkExistence(mapToSet(permissionRequests, PermissionRequest::getPolicyId));

    val group = groupService.getGroupWithRelationships(groupId);

    // Convert the GroupPermission to PermissionRequests since all permission requests apply to the same owner (the group)
    val existingPermissionRequests = mapToSet(group.getPermissions(), GroupPermissionService::convertToPermissionRequest);
    val permissionAnalysis = analyze(existingPermissionRequests, permissionRequests);

    // Check there are no unresolvable permission requests
    checkUnique(permissionAnalysis.getUnresolvableMap().isEmpty(),
        "Found multiple (%s) PermissionRequests with policyIds that have multiple masks: %s",
        permissionAnalysis.getUnresolvableMap().keySet().size(),
        permissionAnalysis.summarizeUnresolvables());

    // Check that are no permission requests that effectively exist
    checkUnique(permissionAnalysis.getDuplicates().isEmpty(),
        "The following permissions already exist for group '%s': ",
        groupId, COMMA.join(permissionAnalysis.getDuplicates()));

    return createOrUpdatePermissions(group, permissionAnalysis);
  }

  public Page<GroupPermission> getGroupPermissions(
      @NonNull UUID groupId, @NonNull Pageable pageable) {
    val groupPermissions = ImmutableList.copyOf(groupService.getGroupWithRelationships(groupId).getPermissions());
    return new PageImpl<>(groupPermissions, pageable, groupPermissions.size());
  }

  @Override
  public PolicyResponse convertToPolicyResponse(@NonNull GroupPermission p) {
    val name = p.getOwner().getName();
    val id = p.getOwner().getId().toString();
    val mask = p.getAccessLevel();
    return PolicyResponse.builder().name(name).id(id).mask(mask).build();
  }

  /**
   * Create or Update the permission for the group based on the supplied analysis
   * @param group with all its relationships loaded
   * @param permissionAnalysis containing pre-sanitized lists of createable and updateable requests
   */
  private Group createOrUpdatePermissions(Group group, PermissionAnalysis permissionAnalysis){
    val updatedGroup = updateGroupPermissions(group, permissionAnalysis.getUpdateables());
    return createGroupPermissions(updatedGroup, permissionAnalysis.getCreateables());
  }

  /**
   * Update existing GroupPermissions for a group with different data while maintaining the same relationships
   * @param group with all its relationships loaded
   */
  private Group updateGroupPermissions(Group group, Collection<PermissionRequest> updatePermissionRequests){
    val existingGroupPermissionMap = uniqueIndex(group.getPermissions(), x -> x.getPolicy().getId());

    updatePermissionRequests.forEach(p -> {
      val policyId = p.getPolicyId();
      val mask = p.getMask();
      checkNotFound(existingGroupPermissionMap.containsKey(policyId),
          "Could not find existing GroupPermission with policyId '%s' for group '%s'",
          policyId, group.getId());
      val gp = existingGroupPermissionMap.get(policyId);
      gp.setAccessLevel(mask);
    });
    return group;
  }

  /**
   * Create new GroupPermissions for a group
   * @param group with all its relationships loaded
   */
  private Group createGroupPermissions(Group group, Collection<PermissionRequest> createablePermissionRequests){
    val existingGroupPermissionMap = uniqueIndex(group.getPermissions(), x -> x.getPolicy().getId());
    val requestedPolicyIds = mapToSet(createablePermissionRequests, PermissionRequest::getPolicyId);

    // Double check the permissions you are creating dont conflict with whats existing
    val redundantPolicyIds = intersect(requestedPolicyIds, existingGroupPermissionMap.keySet());
    checkUnique(redundantPolicyIds.isEmpty(),
        "GroupPermissions with the following policyIds could not be created because "
            + "GroupPermissions with those policyIds already exist: %s",
        COMMA.join(redundantPolicyIds));

    val requestedPolicyMap = uniqueIndex(policyService.getMany(requestedPolicyIds), Policy::getId);
    createablePermissionRequests.forEach(x -> createGroupPermission(requestedPolicyMap, group, x));
    return group;
  }

  private void createGroupPermission(Map<UUID, Policy> policyMap, Group group, PermissionRequest request){
    val gp = new GroupPermission();
    val policy = policyMap.get(request.getPolicyId());
    gp.setAccessLevel(request.getMask());
    associateGroupPermission(group, gp);
    associateGroupPermission(policy, gp);
  }

  /**
   * Disassociates group permissions from its parents
   * @param groupPermissions assumed to be loaded with parents
   */
  private static void disassociateGroupPermissions(Collection<GroupPermission> groupPermissions){
    groupPermissions.forEach( x-> {
          x.getOwner().getPermissions().remove(x);
          x.getPolicy().getGroupPermissions().remove(x);
          x.setPolicy(null);
          x.setOwner(null);
        }
    );
  }

  private static PermissionRequest convertToPermissionRequest(GroupPermission gp){
    return PermissionRequest.builder()
        .mask(gp.getAccessLevel())
        .policyId(gp.getPolicy().getId())
        .build();
  }

  private static void associateGroupPermission(
      @NonNull Policy policy, @NonNull GroupPermission groupPermission) {
    policy.getGroupPermissions().add(groupPermission);
    groupPermission.setPolicy(policy);
  }

  private static void associateGroupPermission(
      @NonNull Group group, @NonNull GroupPermission groupPermission) {
    group.getPermissions().add(groupPermission);
    groupPermission.setOwner(group);
  }

}
