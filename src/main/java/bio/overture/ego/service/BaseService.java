package bio.overture.ego.service;

import java.util.Optional;
import javax.persistence.EntityNotFoundException;
import org.springframework.data.repository.PagingAndSortingRepository;

public abstract class BaseService<T, E> {

  protected T getById(PagingAndSortingRepository<T, E> repository, E id) {
    Optional<T> entity = repository.findById(id);
    // TODO @AlexLepsa - replace with return policy.orElseThrow...
    entity.orElseThrow(EntityNotFoundException::new);
    return entity.get();
  }
}
