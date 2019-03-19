package bio.overture.ego.config;

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.association.AssociationService;
import bio.overture.ego.service.association.ManyToManyAssociationService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class AssociatorConfig {

  @Autowired private ApplicationService applicationService;
  @Autowired private GroupService groupService;
  @Autowired private UserService userService;

  /*
  @Bean
  public FunctionalAssociationService<Group, Application> groupApplicationAssociatorService(){
    return FunctionalAssociationService.<Group, Application>builder()
        .parentType(Group.class)
        .childType(Application.class)
        .getChildrenCallback(Group::getApplications)
        .childService(applicationService)
        .getParentWithRelationshipsCallback(groupService::getGroupWithRelationships)
        .associationCallback(GroupService::associateApplications)
        .disassociationCallback(GroupService::disassociateApplications)
        .parentRepository(groupService.getRepository())
        .build();
  }

  @Bean
  public FunctionalAssociationService<Group, User> groupUserAssociatorService(){
    return FunctionalAssociationService.<Group, User>builder()
        .parentType(Group.class)
        .childType(User.class)
        .getChildrenCallback(Group::getUsers)
        .childService(userService)
        .getParentWithRelationshipsCallback(groupService::getGroupWithRelationships)
        .associationCallback(GroupService::associateUsers)
        .disassociationCallback(GroupService::disassociateUsers)
        .parentRepository(groupService.getRepository())
        .build();
  }

  @Bean
  public FunctionalAssociationService<User, Group> userGroupAssociatorService(){
    return FunctionalAssociationService.<User, Group>builder()
        .parentType(User.class)
        .childType(Group.class)
        .getChildrenCallback(User::getGroups)
        .childService(groupService)
        .getParentWithRelationshipsCallback(userService::getUserWithRelationships)
        .associationCallback(UserService::associateUserWithGroups)
        .disassociationCallback(UserService::disassociateUserFromGroups)
        .parentRepository(userService.getRepository())
        .build();
  }
  */

  @Bean
  public AssociationService<Group, Application, UUID> groupApplicationManyToManyAssociationService(){
    return ManyToManyAssociationService.<Group,Application>builder()
        .parentType(Group.class)
        .childType(Application.class)
        .parentRepository(groupService.getRepository())
        .parentService(groupService)
        .childService(applicationService)
        .getChildrenFromParentFunction(Group::getApplications)
        .getParentsFromChildFunction(Application::getGroups)
        .parentFindRequestSpecificationCallback(GroupService::buildFindGroupsByApplicationSpecification)
        .build();
  }

  @Bean
  public AssociationService<Application, Group, UUID> applicationGroupManyToManyAssociationService(){
    return ManyToManyAssociationService.<Application,Group>builder()
        .parentType(Application.class)
        .childType(Group.class)
        .parentRepository(applicationService.getRepository())
        .parentService(applicationService)
        .childService(groupService)
        .getChildrenFromParentFunction(Application::getGroups)
        .getParentsFromChildFunction(Group::getApplications)
        .parentFindRequestSpecificationCallback(ApplicationService::buildFindApplicationByGroupSpecification)
        .build();
  }

  @Bean
  public AssociationService<User, Group, UUID> userGroupManyToManyAssociationService(){
    return ManyToManyAssociationService.<User,Group>builder()
        .parentType(User.class)
        .childType(Group.class)
        .parentRepository(userService.getRepository())
        .parentService(userService)
        .childService(groupService)
        .getChildrenFromParentFunction(User::getGroups)
        .getParentsFromChildFunction(Group::getUsers)
        .parentFindRequestSpecificationCallback(UserService::buildFindUserByGroupSpecification)
        .build();
  }

  @Bean
  public AssociationService<Group, User, UUID> groupUserManyToManyAssociationService(){
    return ManyToManyAssociationService.<Group,User>builder()
        .parentType(Group.class)
        .childType(User.class)
        .parentRepository(groupService.getRepository())
        .parentService(groupService)
        .childService(userService)
        .getChildrenFromParentFunction(Group::getUsers)
        .getParentsFromChildFunction(User::getGroups)
        .parentFindRequestSpecificationCallback(GroupService::buildFindGroupsByUserSpecification)
        .build();
  }


}
