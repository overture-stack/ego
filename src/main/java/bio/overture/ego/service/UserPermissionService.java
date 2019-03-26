package bio.overture.ego.service;

import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.repository.UserPermissionRepository;
import java.util.Collection;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class UserPermissionService extends AbstractPermissionService<User, UserPermission> {

  @Autowired
  public UserPermissionService(
      @NonNull UserPermissionRepository repository,
      @NonNull UserService userService,
      @NonNull PolicyService policyService) {
    super(User.class, UserPermission.class, userService, policyService, repository);
  }

  @Override
  protected Collection<UserPermission> getPermissionsFromOwner(@NonNull User owner) {
    return owner.getUserPermissions();
  }

  @Override
  protected Collection<UserPermission> getPermissionsFromPolicy(@NonNull Policy policy) {
    return policy.getUserPermissions();
  }
}
