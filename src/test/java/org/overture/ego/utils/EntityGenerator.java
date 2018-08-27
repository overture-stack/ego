package org.overture.ego.utils;

import lombok.val;
import org.overture.ego.model.entity.AclEntity;
import org.overture.ego.model.entity.Application;
import org.overture.ego.model.entity.Group;
import org.overture.ego.model.entity.User;
import org.overture.ego.service.AclEntityService;
import org.overture.ego.service.ApplicationService;
import org.overture.ego.service.GroupService;
import org.overture.ego.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class EntityGenerator {

  @Autowired
  private ApplicationService applicationService;

  @Autowired
  private UserService userService;

  @Autowired
  private GroupService groupService;

  @Autowired
  private AclEntityService aclEntityService;

  public Application createOneApplication(String clientId) {
    return new Application(String.format("Application %s", clientId), clientId, new StringBuilder(clientId).reverse().toString());
  }

  public List<Application> createApplicationsFromList(List<String> clientIds) {
    return clientIds.stream().map(this::createOneApplication).collect(Collectors.toList());
  }

  public void setupSimpleApplications() {
    for (Application application : createApplicationsFromList(Arrays.asList("111111", "222222", "333333", "444444", "555555"))) {
      applicationService.create(application);
    }
  }

  public User createOneUser(Pair<String, String> user) {
    val firstName = user.getFirst();
    val lastName = user.getSecond();

    return User
        .builder()
        .email(String.format("%s%s@domain.com", firstName, lastName))
        .name(String.format("%s%s", firstName, lastName))
        .firstName(firstName)
        .lastName(lastName)
        .status("Approved")
        .preferredLanguage("English")
        .lastLogin(null)
        .role("ADMIN")
        .build();
  }

  public List<User> createUsersFromList(List<Pair<String, String>> users) {
    return users.stream().map(this::createOneUser).collect(Collectors.toList());
  }

  public void setupSimpleUsers() {
    for (User user : createUsersFromList(Arrays.asList(Pair.of("First", "User"), Pair.of("Second", "User"), Pair.of("Third", "User")))) {
      userService.create(user);
    }
  }

  public Group createOneGroup(String name) {
    return new Group(name);
  }

  public List<Group> createGroupsfromList(List<String> groups) {
    return groups.stream().map(this::createOneGroup).collect(Collectors.toList());
  }

  public void setupSimpleGroups() {
    for (Group group : createGroupsfromList(Arrays.asList("Group One", "Group Two", "Group Three"))) {
      groupService.create(group);
    }
  }

  public AclEntity createOneAclEntity(Pair<String, UUID> aclEntity) {
    return AclEntity.builder()
        .name(aclEntity.getFirst())
        .owner(aclEntity.getSecond())
        .build();
  }

  public List<AclEntity> createAclEntitiesFromList(List<Pair<String, UUID>> aclEntities) {
    return aclEntities.stream().map(this::createOneAclEntity).collect(Collectors.toList());
  }

  public void setupSimpleAclEntities(List<Group> threeGroups) {

    for (AclEntity aclEntity : createAclEntitiesFromList(
        Arrays.asList(
            Pair.of("Study001", threeGroups.get(0).getId()),
            Pair.of("Study002", threeGroups.get(1).getId()),
            Pair.of("Study003", threeGroups.get(2).getId())
        ))) {
      aclEntityService.create(aclEntity);
    }
  }
}
