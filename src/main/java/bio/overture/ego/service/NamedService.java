package bio.overture.ego.service;

import bio.overture.ego.repository.NamedRepository;

import java.util.Optional;

public interface NamedService<T, ID> extends BaseService<T, ID> {

  Optional<T> findByName(String name);

  T getByName(String name);
}
