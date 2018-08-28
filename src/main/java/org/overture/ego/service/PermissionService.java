package org.overture.ego.service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.overture.ego.model.entity.Scope;
import org.overture.ego.model.search.SearchFilter;
import org.overture.ego.repository.PermissionRepository;
import org.overture.ego.repository.queryspecification.AclPermissionSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static java.util.UUID.fromString;

@Slf4j
@Transactional
public abstract class PermissionService extends BaseService<Scope, UUID> {

  private PermissionRepository<Scope> repository;

  // Create
  public Scope create(@NonNull Scope entity) {
    return repository.save(entity);
  }

  // Read
  public Scope get(@NonNull String entityId) {
    return getById(repository, fromString(entityId));
  }

  public Page<Scope> listAclEntities(@NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return repository.findAll(AclPermissionSpecification.filterBy(filters), pageable);
  }

  // Update
  public Scope update(@NonNull Scope updatedEntity) {
    Scope entity = getById(repository, updatedEntity.getId());
    entity.update(updatedEntity);
    repository.save(entity);
    return updatedEntity;
  }

  // Delete
  public void delete(@NonNull String entityId) {
    repository.deleteById(fromString(entityId));
  }

}
