package bio.overture.ego.service;

import bio.overture.ego.model.exceptions.NotFoundException;
import org.springframework.data.repository.PagingAndSortingRepository;
import java.util.Optional;

public abstract class BaseService<T, E> {
  protected T getById(PagingAndSortingRepository<T, E> repository, E id) {
    Optional<T> entity = repository.findById(id);
    return entity.orElseThrow(() -> new NotFoundException(String.format("No result for: %s", id.toString())));
  }
}
