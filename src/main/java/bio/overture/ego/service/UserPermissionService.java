package bio.overture.ego.service;

import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.repository.UserPermissionRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class UserPermissionService extends AbstractPermissionService<UserPermission> {

  @Autowired
  public UserPermissionService(@NonNull UserPermissionRepository repository) {
    super(UserPermission.class, repository);
  }

  @Override
  public PolicyResponse convertToPolicyResponse(@NonNull UserPermission userPermission) {
    val name = userPermission.getOwner().getName();
    val id = userPermission.getOwner().getId().toString();
    val mask = userPermission.getAccessLevel();
    return PolicyResponse.builder().name(name).id(id).mask(mask).build();
  }
}
