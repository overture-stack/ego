package bio.overture.ego.service.association.impl_old;

import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.association.AbstractManyToManyAssociationService;
import bio.overture.ego.service.association.FindRequest;
import lombok.NonNull;
import lombok.val;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.UUID;

import static bio.overture.ego.service.ApplicationService.buildFindApplicationByGroupSpecification;

public class ApplicationGroupAssociationService extends AbstractManyToManyAssociationService<Application, Group> {

  private final ApplicationService applicationService;

  public ApplicationGroupAssociationService(
      @NonNull GroupService groupService,
      @NonNull ApplicationService applicationService) {
    super(Application.class, Group.class, applicationService.getRepository(), groupService);
    this.applicationService = applicationService;
  }

  @Override
  public Page<Application> findParentsForChild(FindRequest findRequest) {
    val spec =  buildFindApplicationByGroupSpecification(findRequest);
    return (Page<Application>)applicationService.getRepository().findAll(spec, findRequest.getPageable());
  }

  @Override
  public void associate(@NonNull Application application, @NonNull Collection<Group> groups) {
    application.getGroups().addAll(groups);
    groups.forEach(x -> x.getApplications().add(application));
  }

  @Override
  public void disassociate(@NonNull Application application, @NonNull Collection<UUID> groupIdsToDisassociate) {
    application.getGroups().forEach(x -> x.getApplications().remove(application));
    application.getGroups().removeIf(x -> groupIdsToDisassociate.contains(x.getId()));
  }

  @Override
  protected Collection<Group> extractChildrenFromParent(Application parent) {
    return parent.getGroups();
  }

  @Override
  protected Application getParentWithChildren(@NonNull UUID id) {
//    return applicationService.getApplicationWithRelationships(id);
    throw new IllegalStateException("not implemented");
  }

}
