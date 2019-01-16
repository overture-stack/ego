package bio.overture.ego.service;

import static java.util.UUID.fromString;

import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.Permission;
import bio.overture.ego.repository.BaseRepository;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
public abstract class PermissionService<T extends Permission> extends AbstractBaseService<T, UUID> {

  public PermissionService(Class<T> entityType, BaseRepository<T, UUID> repository) {
    super(entityType, repository);
  }

  // Create
  public T create(@NonNull T entity) {
    return getRepository().save(entity);
  }

  // Read
  public T get(@NonNull String entityId) {
    return getById(fromString(entityId));
  }

  // Update
  public T update(@NonNull T updatedEntity) {
    val entity = getById(updatedEntity.getId());
    // [rtisma] TODO: BUG: the update method's implementation is dependent on the supers private
    // members and not the subclasses members
    entity.update(updatedEntity);
    getRepository().save(entity);
    return updatedEntity;
  }

  // Delete
  public void delete(@NonNull String entityId) {
    delete(fromString(entityId));
  }

  public abstract List<T> findAllByPolicy(String policyId);

  public abstract List<PolicyResponse> findByPolicy(String policyId);

  public abstract PolicyResponse getPolicyResponse(T t);

}
