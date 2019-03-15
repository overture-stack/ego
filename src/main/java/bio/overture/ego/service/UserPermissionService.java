package bio.overture.ego.service;

import bio.overture.ego.event.CleanupTokenPublisher;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.repository.UserPermissionRepository;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class UserPermissionService extends AbstractPermissionService<User, UserPermission> {

  /** Dependencies */
  private final UserService userService;

  private final CleanupTokenPublisher cleanupTokenPublisher;

  @Autowired
  public UserPermissionService(
      @NonNull UserPermissionRepository repository,
      @NonNull UserService userService,
      @NonNull CleanupTokenPublisher cleanupTokenPublisher,
      @NonNull PolicyService policyService) {
    super(User.class, UserPermission.class, userService, policyService, repository);
    this.userService = userService;
    this.cleanupTokenPublisher = cleanupTokenPublisher;
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
    cleanupTokenPublisher.requestTokenCleanup(ImmutableSet.of(userService.getById(userId)));
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
    cleanupTokenPublisher.requestTokenCleanup(ImmutableSet.of(userService.getById(userId)));
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
