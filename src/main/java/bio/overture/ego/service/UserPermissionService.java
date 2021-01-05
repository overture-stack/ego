package bio.overture.ego.service;

import static bio.overture.ego.repository.queryspecification.UserPermissionSpecification.buildFilterAndQuerySpecification;
import static bio.overture.ego.repository.queryspecification.UserPermissionSpecification.buildFilterSpecification;
import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableSet;

import bio.overture.ego.event.token.ApiKeyEventsPublisher;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.dto.ResolvedPermissionResponse;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.model.join.UserGroup;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.UserPermissionRepository;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class UserPermissionService extends AbstractPermissionService<User, UserPermission> {

  /** Dependencies */
  private final UserService userService;

  private final ApiKeyEventsPublisher apiKeyEventsPublisher;

  @Autowired
  public UserPermissionService(
      @NonNull UserPermissionRepository repository,
      @NonNull UserService userService,
      @NonNull ApiKeyEventsPublisher apiKeyEventsPublisher,
      @NonNull PolicyService policyService) {
    super(User.class, UserPermission.class, userService, policyService, repository);
    this.userService = userService;
    this.apiKeyEventsPublisher = apiKeyEventsPublisher;
  }

  protected PolicyResponse convertToPolicyResponse(@NonNull UserPermission userPermission) {
    val id = userPermission.getOwner().getId().toString();
    val mask = userPermission.getAccessLevel();
    // setting name value to user.id because email is no longer a unique identifier
    // this is more secure and consistent for tracking user permissions
    return PolicyResponse.builder().name(id).id(id).mask(mask).build();
  }

  public Page<PolicyResponse> listUserPermissionsByPolicy(
      @NonNull UUID policyId, List<SearchFilter> filters, @NonNull Pageable pageable) {
    // Note: Since userPermissions needs users and policies fetched,
    // cannot just do a join, need to do a fetch AND join,
    // otherwise will experience N+1 query problem

    val userPermissions =
        (Page<UserPermission>)
            getRepository().findAll(buildFilterSpecification(policyId, filters), pageable);

    val responses =
        userPermissions.stream().map(this::convertToPolicyResponse).collect(toUnmodifiableList());

    return new PageImpl<>(responses, pageable, userPermissions.getTotalElements());
  }

  public Page<PolicyResponse> findUserPermissionsByPolicy(
      @NonNull UUID policyId,
      List<SearchFilter> filters,
      String query,
      @NonNull Pageable pageable) {

    // Note: Since userPermissions needs users and policies fetched,
    // cannot just do a join, need to do a fetch AND join,
    // otherwise will experience N+1 query problem
    val userPermissions =
        (Page<UserPermission>)
            getRepository()
                .findAll(buildFilterAndQuerySpecification(policyId, filters, query), pageable);

    val responses =
        userPermissions.stream().map(this::convertToPolicyResponse).collect(toUnmodifiableList());

    return new PageImpl<>(responses, pageable, userPermissions.getTotalElements());
  }

  /**
   * Decorates the call to addPermissions with the functionality to also cleanup user tokens in the
   * event that the permission added downgrades the available scopes to the user.
   *
   * @param userId Id of the user who's permissions are being added or updated
   * @param permissionRequests A list of permission changes
   */
  @Override
  public User addPermissions(
      @NonNull UUID userId, @NonNull List<PermissionRequest> permissionRequests) {
    val user = super.addPermissions(userId, permissionRequests);
    apiKeyEventsPublisher.requestApiKeyCleanupByUsers(ImmutableSet.of(userService.getById(userId)));
    return user;
  }

  /**
   * Decorates the call to deletePermissions with the functionality to also cleanup user tokens
   *
   * @param userId Id of the user who's permissions are being deleted
   * @param idsToDelete Ids of the permission to delete
   */
  @Override
  public void deletePermissions(@NonNull UUID userId, @NonNull Collection<UUID> idsToDelete) {
    super.deletePermissions(userId, idsToDelete);
    apiKeyEventsPublisher.requestApiKeyCleanupByUsers(ImmutableSet.of(userService.getById(userId)));
  }

  @Override
  protected Collection<UserPermission> getPermissionsFromOwner(@NonNull User owner) {
    return owner.getUserPermissions();
  }

  @Override
  protected Collection<UserPermission> getPermissionsFromPolicy(@NonNull Policy policy) {
    return policy.getUserPermissions();
  }

  public List<ResolvedPermissionResponse> getResolvedPermissions(@NonNull UUID userId) {
    val user = userService.getWithRelationships(userId);
    val userPermissions = user.getUserPermissions();
    val groupPermissions =
        mapToList(user.getUserGroups(), UserGroup::getGroup).stream()
            .map(Group::getPermissions)
            .flatMap(Collection::stream)
            .collect(toUnmodifiableSet());
    return getResolvedPermissionsResponse(userPermissions, groupPermissions);
  }
}
