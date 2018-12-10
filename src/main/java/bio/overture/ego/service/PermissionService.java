package bio.overture.ego.service;

import static java.util.UUID.fromString;

import bio.overture.ego.model.entity.Permission;
import bio.overture.ego.repository.PermissionRepository;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
public abstract class PermissionService<T extends Permission> extends BaseService<T, UUID> {
  @Autowired private PermissionRepository<T> repository;

  protected PermissionRepository<T> getRepository() {
    return repository;
  }
  // Create
  public T create(@NonNull T entity) {
    return repository.save(entity);
  }

  // Read
  public T get(@NonNull String entityId) {
    return getById(repository, fromString(entityId));
  }

  // Update
  public T update(@NonNull T updatedEntity) {
    val entity = getById(repository, updatedEntity.getId());
    entity.update(updatedEntity);
    repository.save(entity);
    return updatedEntity;
  }

  // Delete
  public void delete(@NonNull String entityId) {
    repository.deleteById(fromString(entityId));
  }
}
