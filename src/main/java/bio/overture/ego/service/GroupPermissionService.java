package bio.overture.ego.service;

import bio.overture.ego.event.token.TokenEventsPublisher;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.repository.GroupPermissionRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GroupPermissionService extends AbstractPermissionService<Group, GroupPermission> {

  /** Dependencies */
  private final GroupService groupService;

  private final TokenEventsPublisher tokenEventsPublisher;

  @Autowired
  public GroupPermissionService(
      @NonNull GroupPermissionRepository repository,
      @NonNull GroupService groupService,
      @NonNull TokenEventsPublisher tokenEventsPublisher,
      @NonNull PolicyService policyService) {
    super(Group.class, GroupPermission.class, groupService, policyService, repository);
    this.groupService = groupService;
    this.tokenEventsPublisher = tokenEventsPublisher;
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
    tokenEventsPublisher.requestTokenCleanupByUsers(group.getUsers());
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
    val group = getOwnerWithRelationships(groupId);
    tokenEventsPublisher.requestTokenCleanupByUsers(group.getUsers());
  }

  @Override
  protected Collection<GroupPermission> getPermissionsFromOwner(@NonNull Group owner) {
    return owner.getPermissions();
  }

  @Override
  protected Collection<GroupPermission> getPermissionsFromPolicy(@NonNull Policy policy) {
    return policy.getGroupPermissions();
  }

  @Override
  public Group getOwnerWithRelationships(@NonNull UUID ownerId) {
    return groupService.getGroupWithRelationships(ownerId);
  }
}
