package bio.overture.ego.service;

import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.repository.BaseRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.val;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static bio.overture.ego.model.exceptions.NotFoundException.checkExists;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Converters.convertToUUIDList;
import static bio.overture.ego.utils.Joiners.COMMA;
import static java.util.UUID.fromString;

/**
 * Base implementation
 * @param <T>
 */
@Builder
public abstract class AbstractBaseService<T extends Identifiable<UUID>> implements BaseService<T, String> {

  @NonNull private final Class<T> entityType;
  @Getter @NonNull private final BaseRepository<T, UUID> repository;

  public AbstractBaseService(Class<T> entityType, BaseRepository<T, UUID> repository) {
    this.entityType = entityType;
    this.repository = repository;
  }

  @Override
  public String getEntityTypeName() {
    return entityType.getSimpleName();
  }

  @Override
  public Optional<T> findById(@NonNull String id) {
    return getRepository().findById(fromString(id));
  }

  @Override
  public boolean isExist(String id) {
    return getRepository().existsById(fromString(id));
  }

  @Override
  public void delete(String id) {
    checkExistence(id);
    getRepository().deleteById(fromString(id));
  }

  @Override
  public Set<T> getMany(List<String> ids) {
    val entities = repository.findAllByIdIn(convertToUUIDList(ids));
    val nonExistingEntities =
        entities
            .stream()
            .map(Identifiable::getId)
            .map(UUID::toString)
            .filter(x -> !isExist(x))
            .collect(toImmutableSet());
    checkExists(
        nonExistingEntities.isEmpty(),
        "Entities of type '%s' were not found for the following ids: %s",
        getEntityTypeName(),
        COMMA.join(nonExistingEntities));
    return entities;
  }
}
