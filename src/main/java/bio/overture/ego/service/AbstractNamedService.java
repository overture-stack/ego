package bio.overture.ego.service;

import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.repository.CommonRepository;
import lombok.val;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static bio.overture.ego.model.exceptions.NotFoundException.checkExists;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Joiners.COMMA;

public abstract class AbstractCommonService<T extends Identifiable<ID> ,ID, R extends CommonRepository<T, ID>>
    implements CommonService<T, ID, R> {

  private final BaseService<T,ID,R> baseService;

  public AbstractCommonService(BaseService<T, ID, R> baseService) {
    this.baseService = baseService;
  }

  /**
   * Convienence constructor to create a CommonService.
   * Would usually be implemented as a static factory method,
   * however there are alot of bounded types, and it is cleaner this way
   */
  public AbstractCommonService(Class<T> entityType, R repository) {
    this(new AbstractBaseService<>(entityType, repository));
  }

  @Override
  public Optional<T> findByName(String name) {
    return getRepository().findByName(name);
  }

  @Override
  public Set<T> getMany(List<ID> ids) {
    val groups = getRepository().findAllByIdIn(ids);
    val nonExistingApps = groups
        .stream()
        .map(Identifiable::getId)
        .filter(x -> !getRepository().existsById(x))
        .collect(toImmutableSet());
    checkExists(
        nonExistingApps.isEmpty(),
        "Entities of type '%s' were not found for the following ids: %s",
        getEntityTypeName(),
        COMMA.join(nonExistingApps));
    return groups;
  }

  /**
   * Delegated methods
   */
  @Override public R getRepository() {
    return baseService.getRepository();
  }

  @Override public String getEntityTypeName() {
    return baseService.getEntityTypeName();
  }

  @Override public T getById(ID id) {
    return baseService.getById(id);
  }

  @Override public boolean isExist(ID id) {
    return baseService.isExist(id);
  }

  @Override public void delete(ID id) {
    baseService.delete(id);
  }

}
