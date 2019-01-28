package bio.overture.ego.service;

import static bio.overture.ego.model.dto.Scope.createScope;
import static java.util.UUID.fromString;

import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.entity.AbstractPermission;
import bio.overture.ego.repository.BaseRepository;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
public abstract class AbstractPermissionService<T extends AbstractPermission>
    extends AbstractBaseService<T, UUID> {

  public AbstractPermissionService(Class<T> entityType, BaseRepository<T, UUID> repository) {
    super(entityType, repository);
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

  public abstract List<T> findAllByPolicy(String policyId);

  public abstract List<PolicyResponse> findByPolicy(String policyId);

  public abstract PolicyResponse getPolicyResponse(T t);
}
