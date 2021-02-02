package bio.overture.ego.service;

import static bio.overture.ego.repository.queryspecification.GroupPermissionSpecification.buildFilterAndQuerySpecification;
import static bio.overture.ego.repository.queryspecification.GroupPermissionSpecification.buildFilterSpecification;
import static bio.overture.ego.utils.CollectionUtils.mapToImmutableSet;
import static java.util.stream.Collectors.toUnmodifiableList;

import bio.overture.ego.event.token.ApiKeyEventsPublisher;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.join.UserGroup;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.GroupPermissionRepository;
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

@Slf4j
@Service
public class GroupPermissionService
    extends AbstractNameablePermissionService<Group, GroupPermission> {

  /** Dependencies */
  private final GroupService groupService;

  private final ApiKeyEventsPublisher apiKeyEventsPublisher;

  @Autowired
  public GroupPermissionService(
      @NonNull GroupPermissionRepository repository,
      @NonNull GroupService groupService,
      @NonNull ApiKeyEventsPublisher apiKeyEventsPublisher,
      @NonNull PolicyService policyService) {
    super(Group.class, GroupPermission.class, groupService, policyService, repository);
    this.groupService = groupService;
    this.apiKeyEventsPublisher = apiKeyEventsPublisher;
  }

  /**
   * Decorates the call to addPermissions with the functionality to also cleanup user tokens in the
   * event that the permission added downgrades the available scopes to the users of this group.
   *
   * @param groupId Id of the group who's permissions are being added or updated
   * @param permissionRequests A list of permission changes
   */
  @Override
  public Group addPermissions(
      @NonNull UUID groupId, @NonNull List<PermissionRequest> permissionRequests) {
    val group = super.addPermissions(groupId, permissionRequests);
    val users = mapToImmutableSet(group.getUserGroups(), UserGroup::getUser);
    apiKeyEventsPublisher.requestApiKeyCleanupByUsers(users);
    return group;
  }

  /**
   * Decorates the call to deletePermissions with the functionality to also cleanup user tokens
   *
   * @param groupId Id of the group who's permissions are being deleted
   * @param idsToDelete Ids of the permission to delete
   */
  @Override
  public void deletePermissions(@NonNull UUID groupId, @NonNull Collection<UUID> idsToDelete) {
    super.deletePermissions(groupId, idsToDelete);
    val group = groupService.getWithRelationships(groupId);
    val users = mapToImmutableSet(group.getUserGroups(), UserGroup::getUser);
    apiKeyEventsPublisher.requestApiKeyCleanupByUsers(users);
  }

  @Override
  protected Collection<GroupPermission> getPermissionsFromOwner(@NonNull Group owner) {
    return owner.getPermissions();
  }

  @Override
  protected Collection<GroupPermission> getPermissionsFromPolicy(@NonNull Policy policy) {
    return policy.getGroupPermissions();
  }

  public Page<PolicyResponse> listGroupPermissionsByPolicy(
      @NonNull UUID policyId, List<SearchFilter> filters, @NonNull Pageable pageable) {

    val groupPermissions =
        (Page<GroupPermission>)
            getRepository().findAll(buildFilterSpecification(policyId, filters), pageable);

    val responses =
        groupPermissions.stream().map(this::convertToPolicyResponse).collect(toUnmodifiableList());

    return new PageImpl<>(responses, pageable, groupPermissions.getTotalElements());
  }

  public Page<PolicyResponse> findGroupPermissionsByPolicy(
      @NonNull UUID policyId,
      List<SearchFilter> filters,
      String query,
      @NonNull Pageable pageable) {
    val groupPermissions =
        (Page<GroupPermission>)
            getRepository()
                .findAll(buildFilterAndQuerySpecification(policyId, filters, query), pageable);

    val responses =
        groupPermissions.stream().map(this::convertToPolicyResponse).collect(toUnmodifiableList());

    return new PageImpl<>(responses, pageable, groupPermissions.getTotalElements());
  }
}
