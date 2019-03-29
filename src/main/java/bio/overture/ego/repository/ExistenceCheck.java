package bio.overture.ego.repository;

import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Joiners.COMMA;

import bio.overture.ego.model.entity.Identifiable;
import java.util.Collection;
import lombok.NonNull;
import lombok.val;

public class ExistenceCheck {

  public static <T extends Identifiable<ID>, ID> void checkExistence(
      @NonNull BaseRepository<T, ID> repository,
      @NonNull Class<T> entityType,
      @NonNull Collection<ID> ids) {
    val missingIds = ids.stream().filter(x -> !repository.existsById(x)).collect(toImmutableSet());
    checkNotFound(
        missingIds.isEmpty(),
        "The following '%s' entity ids do no exist: %s",
        resolveEntityTypeName(entityType),
        COMMA.join(missingIds));
  }

  public static <T extends Identifiable<ID>, ID> void checkExistence(
      @NonNull BaseRepository<T, ID> repository, @NonNull Class<T> entityType, @NonNull ID id) {
    checkNotFound(
        repository.existsById(id),
        "The '%s' entity with id '%s' does not exist",
        resolveEntityTypeName(entityType),
        id);
  }

  private static String resolveEntityTypeName(Class<?> entityType) {
    return entityType.getSimpleName();
  }
}
