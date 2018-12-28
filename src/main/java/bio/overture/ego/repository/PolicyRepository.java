package bio.overture.ego.repository;

import bio.overture.ego.model.entity.Policy;
import java.util.Optional;
import java.util.UUID;

public interface PolicyRepository extends NamedRepository<Policy, UUID> {

  Optional<Policy> getPolicyByNameIgnoreCase(String name);

  @Override
  default Optional<Policy> findByName(String name) {
    return getPolicyByNameIgnoreCase(name);
  }
}
