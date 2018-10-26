package org.overture.ego.utils;

import lombok.val;
import org.overture.ego.model.entity.Scope;
import org.overture.ego.model.entity.*;
import org.overture.ego.model.enums.PolicyMask;
import org.overture.ego.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
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
  private PolicyService policyService;

  @Autowired
  private TokenStoreService tokenStoreService;

  public static TestData instance = null;

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

  public Application setupApplication(String clientId, String clientSecret) {
    val app = new Application();
    app.setClientId(clientId);
    app.setClientSecret(clientSecret);
    app.setName(clientId);
    app.setStatus("Approved");
    return applicationService.create(app);
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

  public User setupUser(String name) {
    val names= name.split(" ",2);
    val user = createOneUser(Pair.of(names[0], names[1]));
    return userService.create(user);
  }

  public void setupSimpleUsers() {
    for (User user : createUsersFromList(Arrays.asList(Pair.of("First", "User"), Pair.of("Second", "User"), Pair.of("Third", "User")))) {
      userService.create(user);
    }
  }

  public Group createOneGroup(String name) {
    return new Group(name);
  }

  public Group setupGroup(String name) {
    val group = createOneGroup(name);
    return groupService.create(group);
  }

  public List<Group> createGroupsfromList(List<String> groups) {
    return groups.stream().map(this::createOneGroup).collect(Collectors.toList());
  }

  public void setupSimpleGroups() {
    for (Group group : createGroupsfromList(Arrays.asList("Group One", "Group Two", "Group Three"))) {
      groupService.create(group);
    }
  }

  public Policy createOneAclEntity(Pair<String, UUID> aclEntity) {
    return Policy.builder()
        .name(aclEntity.getFirst())
        .owner(aclEntity.getSecond())
        .build();
  }

  public Policy setupPolicy(String name, UUID groupId) {
    val policy = createOneAclEntity(Pair.of(name, groupId));
    return policyService.create(policy);
  }

  public List<Policy> createAclEntitiesFromList(List<Pair<String, UUID>> aclEntities) {
    return aclEntities.stream().map(this::createOneAclEntity).collect(Collectors.toList());
  }

  public void setupSimpleAclEntities(List<Group> threeGroups) {

    for (Policy policy : createAclEntitiesFromList(
        Arrays.asList(
            Pair.of("Study001", threeGroups.get(0).getId()),
            Pair.of("Study002", threeGroups.get(1).getId()),
            Pair.of("Study003", threeGroups.get(2).getId())
        ))) {
      policyService.create(policy);
    }
  }

  private List<String> list(String... s) {
    return Arrays.asList(s);
  }

  public ScopedAccessToken setupToken(User user, String token, long duration, Set<Policy> policies,
    Set<Application> applications) {
    val scopes = policies.stream().map(p -> new Scope(p, PolicyMask.WRITE)).collect(Collectors.toSet());
    val tokenObject = ScopedAccessToken.builder().
      token(token).owner(user).
      applications(applications == null ? new HashSet<>():applications).
      expires(Date.from(Instant.now().plusSeconds(duration))).
      build();

    tokenObject.setScopes(scopes);

    tokenStoreService.create(tokenObject);
    return tokenObject;
  }

  public void addGroups(User user, List<Group> groups) {
    for(val g:groups) {
      if (user.getWholeGroups().contains(g)) {
        // user already has this group
      } else {
        user.addNewGroup(g);
      }
    }
    userService.update(user);
  }

  public void addPermission(User user, PolicyMask mask, Set<Policy> scopes) {
    for(val policy:scopes) {
      user.addNewPermission(policy, mask);
    }
    userService.update(user);
  }

}

