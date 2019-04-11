package bio.overture.ego.service;

import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.repository.BaseRepository;
import bio.overture.ego.repository.queryspecification.builder.AbstractSpecificationBuilder;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.utils.CollectionUtils.difference;
import static bio.overture.ego.utils.Converters.convertToIds;
import static bio.overture.ego.utils.EntityServices.checkEntityExistence;
import static bio.overture.ego.utils.EntityServices.getManyEntities;
import static bio.overture.ego.utils.Joiners.COMMA;

/**
 * Base implementation
 *
 * @param <T>
 */
@RequiredArgsConstructor
public abstract class AbstractBaseService<T extends Identifiable<ID>, ID>
    implements BaseService<T, ID> {

  @Getter @NonNull private final Class<T> entityType;
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
  @SuppressWarnings("unchecked")
  public Page<T> findAll(@NonNull Specification specification, @NonNull Pageable pageable) {
    return getRepository().findAll(specification, pageable);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Page<T> findAll(@NonNull AbstractSpecificationBuilder<T, ID> specificationBuilder, @NonNull Pageable pageable) {
    return getRepository().findAll(specificationBuilder.listAll(), pageable);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<T> getMany(@NonNull Collection<ID> ids, @NonNull AbstractSpecificationBuilder<T, ID> specificationBuilder) {
    val entities = (List<T>)getRepository().findAll(
        specificationBuilder
            .buildByIds(ids));
    val requestedIds = ImmutableSet.copyOf(ids);
    val existingIds = convertToIds(entities);
    val nonExistingIds = difference(requestedIds, existingIds);
    checkNotFound(
        nonExistingIds.isEmpty(),
        "Entities of entityType '%s' were not found for the following ids: %s",
        getEntityTypeName(),
        COMMA.join(nonExistingIds));
    return entities;

  }

  @Override
  public Set<T> getMany(@NonNull Collection<ID> ids) {
    return getManyEntities(entityType, repository, ids);
  }

  @Override
  public void checkExistence(Collection<ID> ids) {
    checkEntityExistence(getEntityType(), getRepository(), ids);
  }

  @Override
  public void checkExistence(ID id) {
    checkEntityExistence(getEntityType(), getRepository(), id);
  }
}
