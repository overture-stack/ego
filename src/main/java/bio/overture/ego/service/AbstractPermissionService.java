package bio.overture.ego.service;

import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.entity.AbstractPermission;
import bio.overture.ego.repository.PermissionRepository;
import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static bio.overture.ego.model.dto.Scope.createScope;
import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static java.util.UUID.fromString;

@Slf4j
@Transactional
public abstract class AbstractPermissionService<T extends AbstractPermission>
    extends AbstractBaseService<T, UUID> {

  private final PermissionRepository<T> permissionRepository;
  public AbstractPermissionService(Class<T> entityType, PermissionRepository<T> repository) {
    super(entityType, repository);
    this.permissionRepository = repository;
  }

  public T create(@NonNull T entity) {
    return getRepository().save(entity);
  }

  @Deprecated
  public T get(@NonNull String entityId) {
    return getById(fromString(entityId));
  }

  public T update(@NonNull T updatedEntity) {
    val entity = getById(updatedEntity.getId());
    entity.setAccessLevel(updatedEntity.getAccessLevel());
    entity.setPolicy(updatedEntity.getPolicy());
    getRepository().save(entity);
    return updatedEntity;
  }

  public static Scope buildScope(@NonNull AbstractPermission permission) {
    return createScope(permission.getPolicy(), permission.getAccessLevel());
  }

  public void delete(@NonNull String entityId) {
    delete(fromString(entityId));
  }

  public List<PolicyResponse> findByPolicy(UUID policyId){
    val permissions = ImmutableList.copyOf(permissionRepository.findAllByPolicy_Id(policyId));
    return mapToList(permissions, this::convertToPolicyResponse);
  }

  public abstract PolicyResponse convertToPolicyResponse(T t);
}
