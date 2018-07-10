package org.overture.ego.service;

import lombok.NonNull;
import org.overture.ego.repository.PermissionRepository;

import java.util.Optional;

public abstract class PermissionService<T> extends BaseService<T> {

  private PermissionRepository<T> repository;

  // Create
  public T create(@NonNull T entity) {
    return repository.save(entity);
  }

  // Read
  public T get(@NonNull String aclEntityId) {
    return getById(repository, Integer.parseInt(aclEntityId));
  }

  public Optional<T> getBySid(@NonNull int sid) {
    return repository.findOneBySid(sid);
  }
}
