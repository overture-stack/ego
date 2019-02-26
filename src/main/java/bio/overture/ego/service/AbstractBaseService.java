package bio.overture.ego.service;

import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.repository.BaseRepository;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Joiners.COMMA;
import static com.google.common.collect.Sets.difference;

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
  public Set<T> getMany(@NonNull Collection<ID> ids) {
    val entities = repository.findAllByIdIn(ImmutableList.copyOf(ids));

    val requestedIds = ImmutableSet.copyOf(ids);
    val existingIds = entities.stream()
        .map(Identifiable::getId)
        .collect(toImmutableSet());
    val nonExistingIds = difference(requestedIds, existingIds);

    checkNotFound(
        nonExistingIds.isEmpty(),
        "Entities of entityType '%s' were not found for the following ids: %s",
        getEntityTypeName(),
        COMMA.join(nonExistingIds));
    return entities;
  }


}
