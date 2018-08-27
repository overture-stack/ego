package org.overture.ego.service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.overture.ego.model.entity.AclPermission;
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
public abstract class PermissionService extends BaseService<AclPermission, UUID> {

  private PermissionRepository<AclPermission> repository;

  // Create
  public AclPermission create(@NonNull AclPermission entity) {
    return repository.save(entity);
  }

  // Read
  public AclPermission get(@NonNull String entityId) {
    return getById(repository, fromString(entityId));
  }

  public Page<AclPermission> listAclEntities(@NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return repository.findAll(AclPermissionSpecification.filterBy(filters), pageable);
  }

  // Update
  public AclPermission update(@NonNull AclPermission updatedEntity) {
    AclPermission entity = getById(repository, updatedEntity.getId());
    entity.update(updatedEntity);
    repository.save(entity);
    return updatedEntity;
  }

  // Delete
  public void delete(@NonNull String entityId) {
    repository.deleteById(fromString(entityId));
  }

}
