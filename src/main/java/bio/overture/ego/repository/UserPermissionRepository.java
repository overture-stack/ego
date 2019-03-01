package bio.overture.ego.repository;

import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.entity.UserPermission;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Set;
import java.util.UUID;

import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.FETCH;

public interface UserPermissionRepository extends PermissionRepository<User, UserPermission> {

  @EntityGraph(value = "user-permission-entity-with-relationships", type = FETCH)
  Set<UserPermission> findAllByOwner_Id(UUID id);

}
