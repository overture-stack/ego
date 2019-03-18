package bio.overture.ego.service;

import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.repository.UserPermissionRepository;
import java.util.Collection;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class UserPermissionService extends AbstractPermissionService<User, UserPermission> {

  /** Dependencies */
  private final UserService userService;

  @Autowired
  public UserPermissionService(
      @NonNull UserPermissionRepository repository,
      @NonNull UserService userService,
      @NonNull PolicyService policyService) {
    super(User.class, UserPermission.class, userService, policyService, repository);
    this.userService = userService;
  }

  @Override
  protected Collection<UserPermission> getPermissionsFromOwner(@NonNull User owner) {
    return owner.getUserPermissions();
  }

  @Override
  protected Collection<UserPermission> getPermissionsFromPolicy(@NonNull Policy policy) {
    return policy.getUserPermissions();
  }

  @Override
  public User getOwnerWithRelationships(@NonNull UUID ownerId) {
    return userService.getById(ownerId);
  }
}
