package bio.overture.ego.service;

import static bio.overture.ego.utils.EntityServices.checkEntityExistence;
import static bio.overture.ego.utils.EntityServices.getManyEntities;

import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.repository.BaseRepository;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
  public Page<T> findAll(Specification specification, Pageable pageable) {
    return getRepository().findAll(specification, pageable);
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
