package bio.overture.ego.repository;

import static org.springframework.data.jpa.repository.EntityGraph.EntityGraphType.FETCH;

import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.GroupPermission;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;

public interface GroupPermissionRepository
    extends NameablePermissionRepository<Group, GroupPermission> {

  @EntityGraph(value = "group-permission-entity-with-relationships", type = FETCH)
  Set<GroupPermission> findAllByOwner_Id(UUID id);
}
