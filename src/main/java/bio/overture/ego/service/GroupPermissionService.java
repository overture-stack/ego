package bio.overture.ego.service;

import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static java.util.UUID.fromString;
import static org.springframework.data.jpa.domain.Specifications.where;

import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.repository.BaseRepository;
import bio.overture.ego.repository.queryspecification.GroupPermissionSpecification;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
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

  public List<GroupPermission> findAllByPolicy(@NonNull String policyId) {
    return getRepository()
        .findAll(where(GroupPermissionSpecification.withPolicy(fromString(policyId))));
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
