package bio.overture.ego.service;

import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.repository.GroupPermissionRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.UUID;

@Slf4j
@Service
public class GroupPermissionService extends AbstractPermissionService<Group, GroupPermission> {

  /** Dependencies */
  private final GroupService groupService;

  @Autowired
  public GroupPermissionService(
      @NonNull GroupPermissionRepository repository,
      @NonNull GroupService groupService,
      @NonNull PolicyService policyService) {
    super(Group.class, GroupPermission.class, groupService, policyService, repository);
    this.groupService = groupService;
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
