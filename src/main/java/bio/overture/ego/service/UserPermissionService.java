package bio.overture.ego.service;

import static bio.overture.ego.repository.queryspecification.UserPermissionSpecification.withPolicy;
import static bio.overture.ego.repository.queryspecification.UserPermissionSpecification.withUser;
import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static java.util.UUID.fromString;
import static org.springframework.data.jpa.domain.Specifications.where;

import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.repository.UserPermissionRepository;
import java.util.List;
import javax.persistence.EntityNotFoundException;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class UserPermissionService extends AbstractPermissionService<UserPermission> {

  public UserPermissionService(UserPermissionRepository userPermissionRepository) {
    super(UserPermission.class, userPermissionRepository);
  }

  public List<UserPermission> findAllByPolicy(@NonNull String policyId) {
    return getRepository().findAll(where(withPolicy(fromString(policyId))));
  }

  @SneakyThrows
  public UserPermission findByPolicyAndUser(@NonNull String policyId, @NonNull String userId) {
    val opt =
        getRepository()
            .findOne(where(withPolicy(fromString(policyId))).and(withUser(fromString(userId))));

    return (UserPermission)
        opt.orElseThrow(() -> new EntityNotFoundException("Permission cannot be found."));
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
