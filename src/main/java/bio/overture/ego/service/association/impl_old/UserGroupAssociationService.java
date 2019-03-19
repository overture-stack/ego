package bio.overture.ego.service.association.impl_old;

import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.repository.queryspecification.UserSpecification;
import bio.overture.ego.service.UserService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.association.FindRequest;
import lombok.NonNull;
import lombok.val;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.UUID;

import static org.springframework.data.jpa.domain.Specification.where;

public class UserGroupAssociationService extends AbstractManyToManyAssociationService<User, Group> {

  private final UserService userService;

  public UserGroupAssociationService(
      @NonNull UserService userService,
      @NonNull GroupService groupService) {
    super(User.class, Group.class, userService.getRepository(), groupService);
    this.userService = userService;
  }

  @Override
  public Page<User> findParentsForChild(FindRequest findRequest) {
    val baseSpec = where(UserSpecification.inGroup(findRequest.getId()))
        .and(UserSpecification.filterBy(findRequest.getFilters()));
    val spec = findRequest.getQuery()
        .map(q -> baseSpec.and(UserSpecification.containsText(q)))
        .orElse(baseSpec);
    return (Page<User>)userService.getRepository().findAll(spec, findRequest.getPageable());
  }


  @Override
  public void associate(@NonNull User user, @NonNull Collection<Group> groups) {
    user.getGroups().addAll(groups);
    groups.forEach(g -> g.getUsers().add(user));
  }

  @Override
  public void disassociate(@NonNull User user, @NonNull Collection<UUID> groupIdsToDisassociate) {
    user.getGroups().forEach(g -> g.getUsers().remove(user));
    user.getGroups().removeIf(g -> groupIdsToDisassociate.contains(g.getId()));
  }

  @Override
  protected Collection<Group> extractChildrenFromParent(User user) {
    return user.getGroups();
  }

  @Override
  protected User getParentWithChildren(@NonNull UUID id) {
    return userService.getUserWithRelationships(id);
  }

}
