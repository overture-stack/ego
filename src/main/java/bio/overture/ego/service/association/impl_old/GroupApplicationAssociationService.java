package bio.overture.ego.service.association.impl_old;

import static org.springframework.data.jpa.domain.Specification.where;

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.repository.queryspecification.GroupSpecification;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.association.FindRequest;
import java.util.Collection;
import java.util.UUID;
import lombok.NonNull;
import lombok.val;
import org.springframework.data.domain.Page;

public class GroupApplicationAssociationService
    extends AbstractManyToManyAssociationService<Group, Application> {

  private final GroupService groupService;

  public GroupApplicationAssociationService(
      @NonNull ApplicationService applicationService, @NonNull GroupService groupService) {
    super(Group.class, Application.class, groupService.getRepository(), applicationService);
    this.groupService = groupService;
  }

  @Override
  public Page<Group> findParentsForChild(FindRequest findRequest) {
    val baseSpec =
        where(GroupSpecification.containsApplication(findRequest.getId()))
            .and(GroupSpecification.filterBy(findRequest.getFilters()));
    val spec =
        findRequest
            .getQuery()
            .map(q -> baseSpec.and(GroupSpecification.containsText(q)))
            .orElse(baseSpec);
    return (Page<Group>) groupService.getRepository().findAll(spec, findRequest.getPageable());
  }

  @Override
  public void associate(@NonNull Group group, @NonNull Collection<Application> applications) {
    group.getApplications().addAll(applications);
    applications.forEach(x -> x.getGroups().add(group));
  }

  @Override
  public void disassociate(
      @NonNull Group group, @NonNull Collection<UUID> applicationIdsToDisassociate) {
    group.getApplications().forEach(x -> x.getGroups().remove(group));
    group.getApplications().removeIf(x -> applicationIdsToDisassociate.contains(x.getId()));
  }

  @Override
  protected Collection<Application> extractChildrenFromParent(Group parent) {
    return parent.getApplications();
  }

  @Override
  protected Group getParentWithChildren(@NonNull UUID id) {
    return groupService.getWithRelationships(id);
  }
}
