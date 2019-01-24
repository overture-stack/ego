package bio.overture.ego.service;

import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static java.lang.String.format;

import bio.overture.ego.model.exceptions.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.NonNull;
import lombok.val;

public interface BaseService<T, ID> {

  String getEntityTypeName();

  default T getById(@NonNull ID id) {
    val entity = findById(id);
    return entity.orElseThrow(
        () ->
            new NotFoundException(
                format(
                    "The '%s' entity with id '%s' does not exist",
                    getEntityTypeName(), id.toString())));
  }

  Optional<T> findById(ID id);

  boolean isExist(ID id);

  void delete(ID id);

  Set<T> getMany(List<ID> ids);

  default void checkExistence(@NonNull ID id) {
    checkNotFound(
        isExist(id), "The '%s' entity with id '%s' does not exist", getEntityTypeName(), id);
  }
}
