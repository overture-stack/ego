package bio.overture.ego.service;

import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.repository.queryspecification.GroupPermissionSpecification;
import bio.overture.ego.repository.queryspecification.UserPermissionSpecification;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static java.util.UUID.fromString;
import static org.springframework.data.jpa.domain.Specifications.where;

@Slf4j
@Service
@Transactional
public class GroupPermissionService extends PermissionService<GroupPermission> {
  public List<GroupPermission> findAllByPolicy(@NonNull String policyId) {
    return getRepository().findAll(
      where(GroupPermissionSpecification.withPolicy(fromString(policyId))));
  }


  public List<UUID> findGroupIdsByPolicy(@NonNull String policyId) {
    val permissions = findAllByPolicy(policyId);
    if (permissions == null) {
      return null;
    }
    return mapToList(permissions,p -> p.getOwner().getId());
  }
}
