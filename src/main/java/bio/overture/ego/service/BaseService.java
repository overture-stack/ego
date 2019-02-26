package bio.overture.ego.service;

import bio.overture.ego.model.exceptions.NotFoundException;
import lombok.NonNull;
import lombok.val;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Joiners.COMMA;
import static java.lang.String.format;

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

  Set<T> getMany(Collection<ID> ids);

  default void checkExistence(@NonNull Collection<ID> ids){
    val missingIds = ids.stream()
        .filter(x -> !isExist(x))
        .collect(toImmutableSet());
    checkNotFound(missingIds.isEmpty(),
        "The following '%s' entity ids do no exist: %s",
        getEntityTypeName(), COMMA.join(missingIds));
  }

  default void checkExistence(@NonNull ID id) {
    checkNotFound(
        isExist(id), "The '%s' entity with id '%s' does not exist", getEntityTypeName(), id);
  }
}
