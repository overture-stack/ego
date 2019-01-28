package bio.overture.ego.repository;

import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.FETCH;

import bio.overture.ego.model.entity.Policy;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;

public interface PolicyRepository extends NamedRepository<Policy, UUID> {

  @EntityGraph(value = "policy-entity-with-relationships", type = FETCH)
  Optional<Policy> getPolicyByNameIgnoreCase(String name);

  boolean existsByNameIgnoreCase(String name);

  @Override
  default Optional<Policy> findByName(String name) {
    return getPolicyByNameIgnoreCase(name);
  }
}
