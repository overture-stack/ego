package bio.overture.ego.repository;

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.ApplicationPermission;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Set;
import java.util.UUID;

import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.FETCH;

public interface ApplicationPermissionRepository extends PermissionRepository<Application, ApplicationPermission> {

  @EntityGraph(value = "application-permission-entity-with-relationships", type = FETCH)
  Set<ApplicationPermission> findAllByOwner_Id(UUID id);
}
