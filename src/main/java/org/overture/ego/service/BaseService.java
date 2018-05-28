package org.overture.ego.service;

import org.springframework.data.repository.PagingAndSortingRepository;

import javax.persistence.EntityNotFoundException;
import java.util.Optional;

public abstract class BaseService<T> {

  protected T getById(PagingAndSortingRepository<T, Integer> repository, int id){
    Optional<T> entity = repository.findById(id);
    // TODO @AlexLepsa - replace with return entity.orElseThrow...
    entity.orElseThrow(EntityNotFoundException::new);
    return entity.get();
  }
}
