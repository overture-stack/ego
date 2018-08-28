package org.overture.ego.service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.overture.ego.model.entity.Permission;
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
public abstract class PermissionService extends BaseService<Permission, UUID> {

  private PermissionRepository<Permission> repository;

  // Create
  public Permission create(@NonNull Permission entity) {
    return repository.save(entity);
  }

  // Read
  public Permission get(@NonNull String entityId) {
    return getById(repository, fromString(entityId));
  }

  public Page<Permission> listAclEntities(@NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return repository.findAll(AclPermissionSpecification.filterBy(filters), pageable);
  }

  // Update
  public Permission update(@NonNull Permission updatedEntity) {
    Permission entity = getById(repository, updatedEntity.getId());
    entity.update(updatedEntity);
    repository.save(entity);
    return updatedEntity;
  }

  // Delete
  public void delete(@NonNull String entityId) {
    repository.deleteById(fromString(entityId));
  }

}
