package bio.overture.ego.service;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.CrudRepository;

@RequiredArgsConstructor
public class BaseServiceImpl<T, ID, R extends CrudRepository<T, ID>> implements BaseService<T, ID, R> {

  @NonNull private final Class<T> entityType;
  @Getter @NonNull private final R repository;


  @Override
  public String getEntityTypeName() {
    return entityType.getSimpleName();
  }

}
