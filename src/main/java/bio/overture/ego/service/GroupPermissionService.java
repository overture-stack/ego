package bio.overture.ego.service;

import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static java.util.UUID.fromString;
import static org.springframework.data.jpa.domain.Specifications.where;

import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.repository.queryspecification.GroupPermissionSpecification;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class GroupPermissionService extends PermissionService<GroupPermission> {
  public List<GroupPermission> findAllByPolicy(@NonNull String policyId) {
    return getRepository()
        .findAll(where(GroupPermissionSpecification.withPolicy(fromString(policyId))));
  }

  public List<PolicyResponse> findByPolicy(@NonNull String policyId) {
    val permissions = findAllByPolicy(policyId);
    return mapToList(permissions, this::getPolicyResponse);
  }

  public PolicyResponse getPolicyResponse(GroupPermission p) {
    val name = p.getOwner().getName();
    val id = p.getOwner().getId().toString();
    val mask = p.getAccessLevel();
    return new PolicyResponse(id, name, mask);
  }
}
