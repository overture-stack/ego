package bio.overture.ego.service;

import java.util.Optional;

public interface NamedService<T, ID> extends BaseService<T, ID> {

  Optional<T> findByName(String name);
  T getByName(String name);

}
