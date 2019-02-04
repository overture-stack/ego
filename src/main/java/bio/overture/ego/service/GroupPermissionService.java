package bio.overture.ego.service;

import static bio.overture.ego.repository.queryspecification.GroupPermissionSpecification.withGroup;
import static bio.overture.ego.repository.queryspecification.GroupPermissionSpecification.withPolicy;
import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static java.util.UUID.fromString;
import static org.springframework.data.jpa.domain.Specifications.where;

import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.repository.BaseRepository;
import java.util.List;
import java.util.UUID;
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
public class GroupPermissionService extends AbstractPermissionService<GroupPermission> {

  public GroupPermissionService(BaseRepository<GroupPermission, UUID> repository) {
    super(GroupPermission.class, repository);
  }

  @SneakyThrows
  public GroupPermission findByPolicyAndGroup(@NonNull String policyId, @NonNull String groupId) {
    val opt =
        getRepository()
            .findOne(where(withPolicy(fromString(policyId)).and(withGroup(fromString(groupId)))));

    return (GroupPermission)
        opt.orElseThrow(() -> new EntityNotFoundException("Permission cannot be found."));
  }

  public void deleteByPolicyAndGroup(@NonNull String policyId, @NonNull String groupId) {
    val perm = findByPolicyAndGroup(policyId, groupId);
    delete(perm.getId());
  }

  public void delete(@NonNull String id) {
    getRepository().deleteById(fromString(id));
  }

  public List<GroupPermission> findAllByPolicy(@NonNull String policyId) {
    return getRepository().findAll(where(withPolicy(fromString(policyId))));
  }

  public List<PolicyResponse> findByPolicy(@NonNull String policyId) {
    val permissions = findAllByPolicy(policyId);
    return mapToList(permissions, this::getPolicyResponse);
  }

  public PolicyResponse getPolicyResponse(@NonNull GroupPermission p) {
    val name = p.getOwner().getName();
    val id = p.getOwner().getId().toString();
    val mask = p.getAccessLevel();
    return PolicyResponse.builder().name(name).id(id).mask(mask).build();
  }
}
