package bio.overture.ego.utils;

import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.utils.CollectionUtils.difference;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Joiners.COMMA;
import static lombok.AccessLevel.PRIVATE;

import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.repository.BaseRepository;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;

@NoArgsConstructor(access = PRIVATE)
public class EntityServices {

  public static <T extends Identifiable<ID>, ID> Set<T> getManyEntities(
      @NonNull Class<T> entityType,
      @NonNull BaseRepository<T, ID> repository,
      @NonNull Collection<ID> ids) {
    val entities = repository.findAllByIdIn(ImmutableList.copyOf(ids));

    val requestedIds = ImmutableSet.copyOf(ids);
    val existingIds = entities.stream().map(Identifiable::getId).collect(toImmutableSet());
    val nonExistingIds = difference(requestedIds, existingIds);

    checkNotFound(
        nonExistingIds.isEmpty(),
        "Entities of entityType '%s' were not found for the following ids: %s",
        resolveEntityTypeName(entityType),
        COMMA.join(nonExistingIds));
    return entities;
  }

  public static <T extends Identifiable<ID>, ID> void checkEntityExistence(
      @NonNull Class<T> entityType,
      @NonNull BaseRepository<T, ID> repository,
      @NonNull Collection<ID> ids) {
    val missingIds = ids.stream().filter(x -> !repository.existsById(x)).collect(toImmutableSet());
    checkNotFound(
        missingIds.isEmpty(),
        "The following '%s' entity ids do no exist: %s",
        resolveEntityTypeName(entityType),
        COMMA.join(missingIds));
  }

  public static <T extends Identifiable<ID>, ID> void checkEntityExistence(
      @NonNull Class<T> entityType, @NonNull BaseRepository<T, ID> repository, @NonNull ID id) {
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
