package org.overture.ego.service;

import org.springframework.data.repository.PagingAndSortingRepository;

import javax.persistence.EntityNotFoundException;
import java.util.Optional;

public abstract class BaseService<T, E> {

  protected T getById(PagingAndSortingRepository<T, E> repository, E id){
    Optional<T> entity = repository.findById(id);
    // TODO @AlexLepsa - replace with return entity.orElseThrow...
    entity.orElseThrow(EntityNotFoundException::new);
    return entity.get();
  }
}
