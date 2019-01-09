package bio.overture.ego.utils;

import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.Token;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.model.enums.EntityStatus;
import bio.overture.ego.model.params.ScopeName;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.PolicyService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.service.TokenStoreService;
import bio.overture.ego.service.UserService;
import com.google.common.collect.ImmutableSet;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static bio.overture.ego.service.UserService.associateUserWithPermissions;
import static bio.overture.ego.utils.CollectionUtils.listOf;
import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static java.util.stream.Collectors.toList;

@Component
/**
 * * For this class, we follow the following naming conventions: createEntity: returns a new object
 * of type Entity. setupEntity: Create an policy, saves it using Hibernate, & returns it.
 * setupEntities: Sets up multiple entities at once setupTestEntities: Sets up specific entities
 * used in our unit tests
 */
public class EntityGenerator {

  @Autowired private TokenService tokenService;

  @Autowired private ApplicationService applicationService;

  @Autowired private UserService userService;

  @Autowired private GroupService groupService;

  @Autowired private PolicyService policyService;

  @Autowired private TokenStoreService tokenStoreService;

  private Application createApplication(String clientId) {
    return new Application(appName(clientId), clientId, clientSecret(clientId));
  }

  private String appName(String clientId) {
    return String.format("Application %s", clientId);
  }

  private String clientSecret(String clientId) {
    return new StringBuilder(clientId).reverse().toString();
  }

  public Application setupApplication(String clientId) {
    return applicationService
        .findApplicationByClientId(clientId)
        .orElseGet( () -> {
          val application = createApplication(clientId);
          return applicationService.create(application);
        });
  }

  public List<Application> setupApplications(String... clientIds) {
    return mapToList(listOf(clientIds), this::setupApplication);
  }

  public void setupTestApplications() {
    setupApplications("111111", "222222", "333333", "444444", "555555");
  }

  public Application setupApplication(String clientId, String clientSecret) {
    return applicationService
        .findApplicationByClientId(clientId)
        .orElseGet(() -> {
          val app = new Application();
          app.setClientId(clientId);
          app.setClientSecret(clientSecret);
          app.setName(clientId);
          app.setStatus("Approved");
          return applicationService.create(app);
        });
  }

  private User createUser(String firstName, String lastName) {
    return User.builder()
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

  private User createUser(String name) {
    val names = name.split(" ", 2);
    return createUser(names[0], names[1]);
  }

  public User setupUser(String name) {
    val names = name.split(" ", 2);
    val userName = String.format("%s%s@domain.com", names[0], names[1]);
    return userService
        .findByName(userName)
        .orElseGet(
            () -> {
              val user = createUser(name);
              return userService.create(user);
            });
  }

  public List<User> setupUsers(String... users) {
    return mapToList(listOf(users), this::setupUser);
  }

  public void setupTestUsers() {
    setupUsers("First User", "Second User", "Third User");
  }

  private Group createGroup(String name) {
    return Group.builder()
        .name(name)
        .status(EntityStatus.PENDING.toString())
        .description("")
        .build();
  }

  public Group setupGroup(String name) {
    return groupService
        .findByName(name)
        .orElseGet(() -> {
          val group = createGroup(name);
          return groupService.create(group);
        });
  }

  public List<Group> setupGroups(String... groupNames) {
    return mapToList(listOf(groupNames), this::setupGroup);
  }

  public void setupTestGroups() {
    setupGroups("Group One", "Group Two", "Group Three");
  }

  private Policy createPolicy(String name, UUID policyId) {
    return Policy.builder().name(name).owner(policyId).build();
  }

  private Policy createPolicy(String name) {
    val args = name.split(",");
    return createPolicy(args[0], args[1]);
  }

  private Policy createPolicy(String name, String groupName) {
    val group = groupService
        .findByName(groupName)
        .orElse(setupGroup(groupName));
    return createPolicy(name, group.getId());
  }

  public Policy setupPolicy(String name, String groupName) {
    return policyService
        .findByName(name)
        .orElseGet(() -> {
          val policy = createPolicy(name, groupName);
          return policyService.create(policy);
        });
  }

  public Policy setupPolicy(String name) {
    return policyService
        .findByName(name)
        .orElseGet( () -> {
          val policy = createPolicy(name);
          return policyService.create(policy);
        });
  }

  public List<Policy> setupPolicies(String... names) {
    return mapToList(listOf(names), this::setupPolicy);
  }

  public void setupTestPolicies() {
    setupPolicies("Study001,Group One", "Study002,Group Two", "Study003,Group Three");
  }

  public Token setupToken(
      User user, String token, long duration, Set<Scope> scopes, Set<Application> applications) {
    val tokenObject =
        Token.builder()
            .token(token)
            .owner(user)
            .applications(applications == null ? new HashSet<>() : applications)
            .expires(Date.from(Instant.now().plusSeconds(duration)))
            .build();

    tokenObject.setScopes(scopes);

    tokenStoreService.create(tokenObject);
    return tokenObject;
  }

  public void addPermissions(User user, Set<Scope> scopes) {
    val userPermissions = scopes.stream()
        .map(s ->
            UserPermission.builder()
                .policy(s.getPolicy())
                .accessLevel(s.getAccessLevel())
                .owner(user)
                .build() )
        .collect(toList());
    associateUserWithPermissions(user, userPermissions);
    userService.getRepository().save(user);
  }

  public static List<ScopeName> scopeNames(String... strings) {
    return mapToList(listOf(strings), ScopeName::new);
  }

  public Set<Scope> getScopes(String... scope) {
    return tokenService.getScopes(ImmutableSet.copyOf(scopeNames(scope)));
  }
}
