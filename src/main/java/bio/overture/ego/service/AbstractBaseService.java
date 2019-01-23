package bio.overture.ego.service;

import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.repository.BaseRepository;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Joiners.COMMA;

/**
 * Base implementation
 *
 * @param <T>
 */
@RequiredArgsConstructor
public abstract class AbstractBaseService<T extends Identifiable<ID>, ID>
    implements BaseService<T, ID> {

  @NonNull private final Class<T> entityType;
  @Getter @NonNull private final BaseRepository<T, ID> repository;

  @Override
  public String getEntityTypeName() {
    return entityType.getSimpleName();
  }

  @Override
  public Optional<T> findById(@NonNull ID id) {
    return getRepository().findById(id);
  }

  @Override
  public boolean isExist(@NonNull ID id) {
    return getRepository().existsById(id);
  }

  @Override
  public void delete(@NonNull ID id) {
    checkExistence(id);
    getRepository().deleteById(id);
  }

  @Override
  public Set<T> getMany(@NonNull List<ID> ids) {
    val entities = repository.findAllByIdIn(ids);
    val nonExistingEntities =
        entities
            .stream()
            .map(Identifiable::getId)
            .filter(x -> !isExist(x))
            .collect(toImmutableSet());
    checkNotFound(
        nonExistingEntities.isEmpty(),
        "Entities of type '%s' were not found for the following ids: %s",
        getEntityTypeName(),
        COMMA.join(nonExistingEntities));
    return entities;
  }
}
