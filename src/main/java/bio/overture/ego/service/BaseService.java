package bio.overture.ego.service;

import bio.overture.ego.model.exceptions.NotFoundException;
import lombok.val;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static bio.overture.ego.model.exceptions.NotFoundException.checkExists;
import static java.lang.String.format;

public interface BaseService<T, ID> {

  String getEntityTypeName();

  default T getById(ID id) {
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

  default void checkExistence(ID id) {
    checkExists(
        isExist(id), "The '%s' entity with id '%s' does not exist", getEntityTypeName(), id);
  }
}
