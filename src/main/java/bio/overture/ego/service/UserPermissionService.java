package bio.overture.ego.service;

import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static java.util.UUID.fromString;

import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.repository.UserPermissionRepository;
import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

  public List<UserPermission> findAllByPolicy(@NonNull String policyId) {
    return ImmutableList.copyOf(repository.findAllByPolicy_Id(fromString(policyId)));
  }

  @SneakyThrows
  public UserPermission findByPolicyAndUser(@NonNull String policyId, @NonNull String userId) {
    val opt = repository.findByPolicy_IdAndOwner_id(fromString(policyId), fromString(userId));

    return opt.orElseThrow(() -> new NotFoundException("Permission cannot be found."));
  }

  public void deleteByPolicyAndUser(@NonNull String policyId, @NonNull String userId) {
    val perm = findByPolicyAndUser(policyId, userId);
    delete(perm.getId());
  }

  public List<PolicyResponse> findByPolicy(@NonNull String policyId) {
    val userPermissions = findAllByPolicy(policyId);
    return mapToList(userPermissions, this::getPolicyResponse);
  }

  public PolicyResponse getPolicyResponse(@NonNull UserPermission userPermission) {
    val name = userPermission.getOwner().getName();
    val id = userPermission.getOwner().getId().toString();
    val mask = userPermission.getAccessLevel();
    return PolicyResponse.builder().name(name).id(id).mask(mask).build();
  }
}
