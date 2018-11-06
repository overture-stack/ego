package org.overture.ego.utils;

import lombok.val;
import org.overture.ego.model.dto.Scope;
import org.overture.ego.model.entity.*;
import org.overture.ego.model.params.ScopeName;
import org.overture.ego.service.*;
import org.overture.ego.token.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

import static org.overture.ego.utils.CollectionUtils.listOf;
import static org.overture.ego.utils.CollectionUtils.mapToList;

@Component
/***
 * For this class, we follow the following naming conventions:
 * createEntity: returns a new object of type Entity.
 * setupEntity: Create an policy, saves it using Hibernate, & returns it.
 * setupEntities: Sets up multiple entities at once
 * setupTestEntities: Sets up specific entities used in our unit tests
 */
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
  private TokenService tokenService;

  @Autowired
  private TokenStoreService tokenStoreService;

  public Application createApplication(String clientId) {
    return new Application(appName(clientId), clientId, clientSecret(clientId));
  }

  private String appName(String clientId) {
    return String.format("Application %s", clientId);
  }

  private String clientSecret(String clientId) {
    return new StringBuilder(clientId).reverse().toString();
  }

  public Application setupApplication(String clientId) {
    val application = createApplication(clientId);
    return applicationService.create(application);
  }

  public List<Application> setupApplications(String... clientIds) {
    return mapToList(listOf(clientIds), this::setupApplication);
  }

  public void setupTestApplications() {
    setupApplications("111111", "222222", "333333", "444444", "555555");
  }

  public Application setupApplication(String clientId, String clientSecret) {
    val app = new Application();
    app.setClientId(clientId);
    app.setClientSecret(clientSecret);
    app.setName(clientId);
    app.setStatus("Approved");
    return applicationService.create(app);
  }

  public User createUser(String firstName, String lastName) {
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

  public User createUser(String name) {
    val names= name.split(" ",2);
    return createUser(names[0], names[1]);
  }
  public User setupUser(String name) {
    val user = createUser(name);
    return userService.create(user);
  }

  public List<User> setupUsers(String... users) {
    return mapToList(listOf(users), this::setupUser);
  }

  public void setupTestUsers() {
    setupUsers("First User", "Second User", "Third User");
  }

  public Group createGroup(String name) {
    return new Group(name);
  }

  public Group setupGroup(String name) {
    val group = createGroup(name);
    return groupService.create(group);
  }

  public List<Group> setupGroups(String... groupNames) {
    return mapToList(listOf(groupNames), this::setupGroup);
  }

  public void setupTestGroups() {
    setupGroups("Group One", "Group Two", "Group Three");
  }

  public Policy createPolicy(String name, UUID policyId) {
    return Policy.builder()
        .name(name)
        .owner(policyId)
        .build();
  }

  public Policy createPolicy(String name) {
    val args = name.split(",");
    return createPolicy(args[0], args[1]);
  }

  public Policy createPolicy(String name, String groupName) {
    Group owner = groupService.getByName(groupName);
    if (owner == null) {
      owner = setupGroup(groupName);
    }
    return createPolicy(name, owner.getId());
  }

  public Policy setupPolicy(String name, String groupName) {
    val policy=createPolicy(name, groupName);
    return policyService.create(policy);
  }

  public Policy setupPolicy(String name) {
    val policy=createPolicy(name);
    return policyService.create(policy);
  }

  public List<Policy> setupPolicies(String... names) {
    return mapToList(listOf(names), this::setupPolicy);
  }

  public void setupTestPolicies() {
    setupPolicies("Study001,Group One", "Study002,Group Two", "Study003,Group Three");
  }

  public ScopedAccessToken setupToken(User user, String token, long duration, Set<Scope> scopes,
    Set<Application> applications) {
    val tokenObject = ScopedAccessToken.builder().
      token(token).owner(user).
      applications(applications == null ? new HashSet<>():applications).
      expires(Date.from(Instant.now().plusSeconds(duration))).
      build();

    tokenObject.setScopes(scopes);

    tokenStoreService.create(tokenObject);
    return tokenObject;
  }

  public void addPermission(User user, Scope scope) {
    user.addNewPermission(scope.getPolicy(), scope.getPolicyMask());
  }

  public void addPermissions(User user, Set<Scope> scopes) {
    for (val s: scopes) {
      addPermission(user, s);
    }
    userService.update(user);
  }

  public static List<ScopeName> scopeNames(String ... strings) {
    return mapToList(listOf(strings), ScopeName::new);
  }

  public Set<Scope> getScopes(String... scope) {
    return tokenService.getScopes(new HashSet<>(scopeNames(scope)));
  }
}

