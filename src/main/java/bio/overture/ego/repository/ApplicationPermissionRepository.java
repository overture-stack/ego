package bio.overture.ego.repository;

import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.FETCH;

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.ApplicationPermission;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;

public interface ApplicationPermissionRepository
    extends PermissionRepository<Application, ApplicationPermission> {

  @EntityGraph(value = "application-permission-entity-with-relationships", type = FETCH)
  Set<ApplicationPermission> findAllByOwner_Id(UUID id);
}
