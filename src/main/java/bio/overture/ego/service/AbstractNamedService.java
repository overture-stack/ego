package bio.overture.ego.service;

import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;

import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.repository.NamedRepository;
import java.util.Optional;
import lombok.NonNull;
import lombok.val;

public abstract class AbstractNamedService<T extends Identifiable<ID>, ID>
    extends AbstractBaseService<T, ID> implements NamedService<T, ID> {

  private final NamedRepository<T, ID> namedRepository;

  public AbstractNamedService(
      @NonNull Class<T> entityType, @NonNull NamedRepository<T, ID> repository) {
    super(entityType, repository);
    this.namedRepository = repository;
  }

  @Override
  public Optional<T> findByName(@NonNull String name) {
    return namedRepository.findByName(name);
  }

  @Override
  public T getByName(@NonNull String name) {
    val result = findByName(name);
    checkNotFound(
        result.isPresent(),
        "The '%s' entity with name '%s' was not found",
        getEntityTypeName(),
        name);
    return result.get();
  }
}
