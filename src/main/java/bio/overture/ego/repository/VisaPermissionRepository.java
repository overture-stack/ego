package bio.overture.ego.repository;

import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.FETCH;

import bio.overture.ego.model.entity.VisaPermission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;

public interface VisaPermissionRepository extends NamedRepository<VisaPermission, UUID> {
  @Override
  @Deprecated
  default Optional<VisaPermission> findByName(String name) {
    return null;
  }

  List<VisaPermission> findAll();

  @EntityGraph(value = "visa-permission-entity-with-relationships", type = FETCH)
  List<VisaPermission> findByVisa_Id(UUID visa_id);

  @EntityGraph(value = "visa-permission-entity-with-relationships", type = FETCH)
  List<VisaPermission> findByPolicy_Id(UUID policy_id);

  @EntityGraph(value = "visa-permission-entity-with-relationships", type = FETCH)
  List<VisaPermission> findByPolicyIdAndVisaId(UUID policy_id, UUID visa_id);
}
