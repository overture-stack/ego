package bio.overture.ego.service;

import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.repository.CommonRepository;
import lombok.NonNull;
import lombok.val;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static bio.overture.ego.model.exceptions.NotFoundException.checkExists;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Joiners.COMMA;

public interface CommonService<T extends Identifiable<ID>, ID, R extends CommonRepository<T, ID>> {

  String getTypeName();

  R getRepository();

  default Optional<T> findByName(@NonNull String name){
    return  getRepository().findByName(name);
  }

  default T getByName(@NonNull String name){
    val result = findByName(name);
    checkExists(result.isPresent(), "The '%s' entity with name '%s' was not found",
        getClass().getSimpleName(), name);
    return result.get();
  }

  default Set<T> getMany(@NonNull List<ID> ids) {
    val groups = getRepository().findAllByIdIn(ids);
    val nonExistingApps = groups
            .stream()
            .map(Identifiable::getId)
            .filter(x -> !getRepository().existsById(x))
            .collect(toImmutableSet());
    checkExists(
        nonExistingApps.isEmpty(),
        "Entities of type '%s' were not found for the following ids: %s",
        getTypeName(),
        COMMA.join(nonExistingApps));
    return groups;
  }

}
