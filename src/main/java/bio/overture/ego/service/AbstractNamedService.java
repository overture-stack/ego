package bio.overture.ego.service;

import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.repository.NamedRepository;
import lombok.val;

import java.util.Optional;
import java.util.UUID;

import static bio.overture.ego.model.exceptions.NotFoundException.checkExists;

public abstract class AbstractNamedService<T extends Identifiable<UUID>> extends AbstractBaseService<T>
    implements NamedService<T> {

  private final NamedRepository<T, UUID> namedRepository;

  public AbstractNamedService(Class<T> entityType, NamedRepository<T, UUID> repository) {
    super(entityType, repository);
    this.namedRepository = repository;
  }

  @Override
  public Optional<T> findByName(String name) {
    return namedRepository.findByName(name);
  }

  public T getByName(String name) {
    val result = findByName(name);
    checkExists(
        result.isPresent(),
        "The '%s' entity with name '%s' was not found",
        getEntityTypeName(),
        name);
    return result.get();
  }

}
