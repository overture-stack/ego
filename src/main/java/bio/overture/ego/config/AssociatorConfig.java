package bio.overture.ego.config;

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.service.association.AssociationService;
import bio.overture.ego.service.association.ManyToManyAssociationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class AssociatorConfig {

  @Autowired private ApplicationService applicationService;
  @Autowired private GroupService groupService;
  @Autowired private UserService userService;

  @Bean
  public AssociationService<Group, Application, UUID>
      groupApplicationManyToManyAssociationService() {
    return ManyToManyAssociationService.<Group, Application>builder()
        .parentType(Group.class)
        .childType(Application.class)
        .parentRepository(groupService.getRepository())
        .parentService(groupService)
        .childService(applicationService)
        .getChildrenFromParentFunction(Group::getApplications)
        .getParentsFromChildFunction(Application::getGroups)
        .parentFindRequestSpecificationCallback(
            GroupService::buildFindGroupsByApplicationSpecification)
        .build();
  }

  @Bean
  public AssociationService<Application, Group, UUID>
      applicationGroupManyToManyAssociationService() {
    return ManyToManyAssociationService.<Application, Group>builder()
        .parentType(Application.class)
        .childType(Group.class)
        .parentRepository(applicationService.getRepository())
        .parentService(applicationService)
        .childService(groupService)
        .getChildrenFromParentFunction(Application::getGroups)
        .getParentsFromChildFunction(Group::getApplications)
        .parentFindRequestSpecificationCallback(
            ApplicationService::buildFindApplicationByGroupSpecification)
        .build();
  }
}
