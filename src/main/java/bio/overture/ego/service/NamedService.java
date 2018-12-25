package bio.overture.ego.service;

import java.util.Optional;

public interface NamedService<T> {

  Optional<T> findByName(String name);

}
