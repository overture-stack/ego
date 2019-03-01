package bio.overture.ego.repository;

import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.GroupPermission;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Set;
import java.util.UUID;

import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.FETCH;

public interface GroupPermissionRepository extends PermissionRepository<Group, GroupPermission> {

  @EntityGraph(value = "group-permission-entity-with-relationships", type = FETCH)
  Set<GroupPermission> findAllByOwner_Id(UUID id);
}
