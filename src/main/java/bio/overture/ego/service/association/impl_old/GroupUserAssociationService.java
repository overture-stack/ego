package bio.overture.ego.service.association.impl_old;

import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.repository.queryspecification.GroupSpecification;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.service.association.AbstractManyToManyAssociationService;
import bio.overture.ego.service.association.FindRequest;
import lombok.NonNull;
import lombok.val;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.UUID;

import static org.springframework.data.jpa.domain.Specification.where;

public class GroupUserAssociationService extends AbstractManyToManyAssociationService<Group, User> {

  private final GroupService groupService;

  public GroupUserAssociationService(
      @NonNull GroupService groupService,
      @NonNull UserService userService) {
    super(Group.class, User.class, groupService.getRepository(), userService);
    this.groupService = groupService;
  }

  @Override
  public Page<Group> findParentsForChild(FindRequest findRequest) {
    val baseSpec = where(GroupSpecification.containsUser(findRequest.getId()))
        .and(GroupSpecification.filterBy(findRequest.getFilters()));
    val spec = findRequest.getQuery()
        .map(q -> baseSpec.and(GroupSpecification.containsText(q)))
        .orElse(baseSpec);
    return (Page<Group>)groupService.getRepository().findAll(spec, findRequest.getPageable());
  }

  @Override
  public void associate(@NonNull Group group, @NonNull Collection<User> users) {
    group.getUsers().addAll(users);
    users.forEach(x -> x.getGroups().add(group));
  }

  @Override
  public void disassociate(@NonNull Group group, @NonNull Collection<UUID> userIdsToDisassociate) {
    group.getUsers().forEach(u -> u.getGroups().remove(group));
    group.getUsers().removeIf(u -> userIdsToDisassociate.contains(u.getId()));
  }

  @Override
  protected Collection<User> extractChildrenFromParent(Group parent) {
    return parent.getUsers();
  }

  @Override
  protected Group getParentWithChildren(@NonNull UUID id) {
    return groupService.getGroupWithRelationships(id);
  }

}
