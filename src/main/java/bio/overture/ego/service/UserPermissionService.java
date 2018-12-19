package bio.overture.ego.service;

import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.repository.queryspecification.UserPermissionSpecification;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static bio.overture.ego.model.exceptions.NotFoundException.checkExists;
import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Converters.convertToUUIDList;
import static bio.overture.ego.utils.Joiners.COMMA;
import static java.util.UUID.fromString;
import static org.springframework.data.jpa.domain.Specifications.where;

@Slf4j
@Service
@Transactional
public class UserPermissionService extends PermissionService<UserPermission> {
  public List<UserPermission> findAllByPolicy(@NonNull String policyId) {
    return getRepository()
        .findAll(where(UserPermissionSpecification.withPolicy(fromString(policyId))));
  }

  public List<PolicyResponse> findByPolicy(@NonNull String policyId) {
    val userPermissions = findAllByPolicy(policyId);
    return mapToList(userPermissions, this::getPolicyResponse);
  }

  public PolicyResponse getPolicyResponse(UserPermission userPermission) {
    val name = userPermission.getOwner().getName();
    val id = userPermission.getOwner().getId().toString();
    val mask = userPermission.getAccessLevel();
    return new PolicyResponse(id, name, mask);
  }

  public Set<UserPermission> getMany(@NonNull Collection<String> permIds) {
    val existingGroups = getRepository().findAllByIdIn(convertToUUIDList(permIds));
    val nonExistingApps = existingGroups.stream()
        .map(UserPermission::getId)
        .filter(x -> !getRepository().existsById(x))
        .collect(toImmutableSet());
    checkExists(nonExistingApps.isEmpty(),
        "The following user permission ids were not found: %s",
        COMMA.join(nonExistingApps));
    return existingGroups;
  }

}
