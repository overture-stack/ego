package bio.overture.ego.repository;

import bio.overture.ego.model.entity.Policy;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;
import java.util.UUID;

import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.FETCH;

public interface PolicyRepository extends NamedRepository<Policy, UUID> {

  @EntityGraph(value = "policy-entity-with-relationships", type = FETCH)
  Optional<Policy> getPolicyByNameIgnoreCase(String name);

  boolean existsByNameIgnoreCase(String name);

  @Override
  default Optional<Policy> findByName(String name) {
    return getPolicyByNameIgnoreCase(name);
  }
}
