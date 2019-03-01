package bio.overture.ego.repository;

import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.FETCH;

import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.entity.UserPermission;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;

public interface UserPermissionRepository extends PermissionRepository<User, UserPermission> {

  @EntityGraph(value = "user-permission-entity-with-relationships", type = FETCH)
  Set<UserPermission> findAllByOwner_Id(UUID id);
}
