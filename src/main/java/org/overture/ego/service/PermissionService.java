package org.overture.ego.service;

import lombok.NonNull;
import org.overture.ego.model.entity.AclPermission;
import org.overture.ego.model.search.SearchFilter;
import org.overture.ego.repository.PermissionRepository;
import org.overture.ego.repository.queryspecification.AclPermissionSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public abstract class PermissionService extends BaseService<AclPermission> {

  private PermissionRepository<AclPermission> repository;

  // Create
  public AclPermission create(@NonNull AclPermission entity) {
    return repository.save(entity);
  }

  // Read
  public AclPermission get(@NonNull String entityId) {
    return getById(repository, Integer.parseInt(entityId));
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
    repository.deleteById(Integer.parseInt(entityId));
  }

}
