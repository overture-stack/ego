package bio.overture.ego.service;

import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.repository.UserPermissionRepository;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static bio.overture.ego.model.exceptions.NotFoundException.buildNotFoundException;
import static java.util.UUID.fromString;

@Slf4j
@Service
@Transactional
public class UserPermissionService extends AbstractPermissionService<UserPermission> {

  private final UserPermissionRepository repository;

  @Autowired
  public UserPermissionService(UserPermissionRepository repository) {
    super(UserPermission.class, repository);
    this.repository = repository;
  }

  @SneakyThrows
  public UserPermission getByPolicyAndUser(@NonNull String policyId, @NonNull String userId) {
    return repository.findByPolicy_IdAndOwner_id(fromString(policyId), fromString(userId))
    .orElseThrow(() -> buildNotFoundException(
        "%s for policy '%s' and owner '%s' cannot be cannot be found",
        UserPermission.class.getSimpleName(), policyId, userId));
  }

  public void deleteByPolicyAndUser(@NonNull String policyId, @NonNull String userId) {
    val perm = getByPolicyAndUser(policyId, userId);
    delete(perm.getId());
  }

  @Override
  public PolicyResponse convertToPolicyResponse(@NonNull UserPermission userPermission) {
    val name = userPermission.getOwner().getName();
    val id = userPermission.getOwner().getId().toString();
    val mask = userPermission.getAccessLevel();
    return PolicyResponse.builder().name(name).id(id).mask(mask).build();
  }
}
