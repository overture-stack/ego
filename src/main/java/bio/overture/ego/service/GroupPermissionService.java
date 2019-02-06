package bio.overture.ego.service;

import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static java.util.UUID.fromString;

import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.repository.GroupPermissionRepository;
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
public class GroupPermissionService extends AbstractPermissionService<GroupPermission> {

  /** Dependencies */
  private final GroupPermissionRepository repository;

  @Autowired
  public GroupPermissionService(@NonNull GroupPermissionRepository repository) {
    super(GroupPermission.class, repository);
    this.repository = repository;
  }

  @SneakyThrows
  public GroupPermission findByPolicyAndGroup(@NonNull String policyId, @NonNull String groupId) {
    val opt = repository.findByPolicy_IdAndOwner_id(fromString(policyId), fromString(groupId));

    return opt.orElseThrow(() -> new NotFoundException("Permission cannot be found."));
  }

  public void deleteByPolicyAndGroup(@NonNull String policyId, @NonNull String groupId) {
    val perm = findByPolicyAndGroup(policyId, groupId);
    delete(perm.getId());
  }

  public List<GroupPermission> findAllByPolicy(@NonNull String policyId) {
    return ImmutableList.copyOf(repository.findAllByPolicy_Id(fromString(policyId)));
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
