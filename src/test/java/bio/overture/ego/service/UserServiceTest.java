package bio.overture.ego.service;

import bio.overture.ego.controller.resolver.PageableResolver;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.params.PolicyIdStringWithAccessLevel;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.token.IDToken;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.PolicyPermissionUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class UserServiceTest {

  private static final String NON_EXISTENT_USER = "827fae28-7fb8-11e8-adc0-fa7ae01bbebc";

  @Autowired
  private ApplicationService applicationService;

  @Autowired
  private UserService userService;

  @Autowired
  private GroupService groupService;

  @Autowired
  private PolicyService policyService;

  @Autowired
  private EntityGenerator entityGenerator;

  // Create
  @Test
  public void testCreate() {
    val user = userService.create(entityGenerator.createUser("Demo", "User"));
    // UserName == UserEmail
    assertThat(user.getName()).isEqualTo("DemoUser@domain.com");
  }

  @Test
  public void testCreateUniqueNameAndEmail() {
    userService.create(entityGenerator.createUser("User", "One"));
    userService.create(entityGenerator.createUser("User", "One"));
    assertThatExceptionOfType(DataIntegrityViolationException.class)
      .isThrownBy(() -> userService.getByName("UserOne@domain.com"));
  }

  @Test
  public void testCreateFromIDToken() {
    val idToken = IDToken.builder()
      .email("UserOne@domain.com")
      .given_name("User")
      .family_name("User")
      .build();

    val idTokenUser = userService.createFromIDToken(idToken);

    assertThat(idTokenUser.getName()).isEqualTo("UserOne@domain.com");
    assertThat(idTokenUser.getEmail()).isEqualTo("UserOne@domain.com");
    assertThat(idTokenUser.getFirstName()).isEqualTo("User");
    assertThat(idTokenUser.getLastName()).isEqualTo("User");
    assertThat(idTokenUser.getStatus()).isEqualTo("Approved");
    assertThat(idTokenUser.getRole()).isEqualTo("USER");
  }

  @Test
  public void testCreateFromIDTokenUniqueNameAndEmail() {
    // Note: This test has one strike due to Hibernate Cache.
    userService.create(entityGenerator.createUser("User", "One"));
    val idToken = IDToken.builder()
      .email("UserOne@domain.com")
      .given_name("User")
      .family_name("One")
      .build();
    userService.createFromIDToken(idToken);

    assertThatExceptionOfType(DataIntegrityViolationException.class)
      .isThrownBy(() -> userService.getByName("UserOne@domain.com"));
  }

  // Get
  @Test
  public void testGet() {
    val user = userService.create(entityGenerator.createUser("User", "One"));
    val savedUser = userService.get(user.getId().toString());
    assertThat(savedUser.getName()).isEqualTo("UserOne@domain.com");
  }

  @Test
  public void testGetEntityNotFoundException() {
    assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> userService.get(NON_EXISTENT_USER));
  }

  @Test
  public void testGetByName() {
    userService.create(entityGenerator.createUser("User", "One"));
    val savedUser = userService.getByName("UserOne@domain.com");
    assertThat(savedUser.getName()).isEqualTo("UserOne@domain.com");
  }

  @Test
  public void testGetByNameAllCaps() {
    userService.create(entityGenerator.createUser("User", "One"));
    val savedUser = userService.getByName("USERONE@DOMAIN.COM");
    assertThat(savedUser.getName()).isEqualTo("UserOne@domain.com");
  }

  @Test
  @Ignore
  public void testGetByNameNotFound() {
    // TODO Currently returning null, should throw exception (EntityNotFoundException?)
    assertThatExceptionOfType(EntityNotFoundException.class)
        .isThrownBy(() -> userService.getByName("UserOne@domain.com"));
  }

  @Test
  public void testGetOrCreateDemoUser() {
    val demoUser = userService.getOrCreateDemoUser();
    assertThat(demoUser.getName()).isEqualTo("Demo.User@example.com");
    assertThat(demoUser.getEmail()).isEqualTo("Demo.User@example.com");
    assertThat(demoUser.getFirstName()).isEqualTo("Demo");
    assertThat(demoUser.getLastName()).isEqualTo("User");
    assertThat(demoUser.getStatus()).isEqualTo("Approved");
    assertThat(demoUser.getRole()).isEqualTo("ADMIN");
  }

  @Test
  public void testGetOrCreateDemoUserAlREADyExisting() {
    // This should force the demo user to have admin and approved status's
    val demoUserObj = User.builder()
      .name("Demo.User@example.com")
      .email("Demo.User@example.com")
      .firstName("Demo")
      .lastName("User")
      .status("Pending")
      .role("USER")
      .build();

    val user = userService.create(demoUserObj);

    assertThat(user.getStatus()).isEqualTo("Pending");
    assertThat(user.getRole()).isEqualTo("USER");

    val demoUser = userService.getOrCreateDemoUser();
    assertThat(demoUser.getStatus()).isEqualTo("Approved");
    assertThat(demoUser.getRole()).isEqualTo("ADMIN");
  }

  // List Users
  @Test
  public void testListUsersNoFilters() {
    entityGenerator.setupTestUsers();
    val users = userService
      .listUsers(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(users.getTotalElements()).isEqualTo(3L);
  }

  @Test
  public void testListUsersNoFiltersEmptyResult() {
    val users = userService
      .listUsers(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(users.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testListUsersFiltered() {
    entityGenerator.setupTestUsers();
    val userFilter = new SearchFilter("email", "FirstUser@domain.com");
    val users = userService
      .listUsers(singletonList(userFilter), new PageableResolver().getPageable());
    assertThat(users.getTotalElements()).isEqualTo(1L);
  }

  @Test
  public void testListUsersFilteredEmptyResult() {
    entityGenerator.setupTestUsers();
    val userFilter = new SearchFilter("email", "FourthUser@domain.com");
    val users = userService
      .listUsers(singletonList(userFilter), new PageableResolver().getPageable());
    assertThat(users.getTotalElements()).isEqualTo(0L);
  }

  // Find Users
  @Test
  public void testFindUsersNoFilters() {
    entityGenerator.setupTestUsers();
    val users = userService
      .findUsers("First", Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(users.getTotalElements()).isEqualTo(1L);
    assertThat(users.getContent().get(0).getName()).isEqualTo("FirstUser@domain.com");
  }

  @Test
  public void testFindUsersFiltered() {
    entityGenerator.setupTestUsers();
    val userFilter = new SearchFilter("email", "FirstUser@domain.com");
    val users = userService
      .findUsers("Second", singletonList(userFilter), new PageableResolver().getPageable());
    // Expect empty list
    assertThat(users.getTotalElements()).isEqualTo(0L);
  }

  // Find Group Users
  @Test
  public void testFindGroupUsersNoQueryNoFilters() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val user = userService.getByName("FirstUser@domain.com");
    val userTwo = (userService.getByName("SecondUser@domain.com"));
    val groupId = groupService.getByName("Group One").getId().toString();

    userService.addUserToGroups(user.getId().toString(), singletonList(groupId));
    userService.addUserToGroups(userTwo.getId().toString(), singletonList(groupId));

    val users = userService.findGroupUsers(
      groupId,
      Collections.emptyList(),
      new PageableResolver().getPageable()
    );

    assertThat(users.getTotalElements()).isEqualTo(2L);
    assertThat(users.getContent()).contains(user, userTwo);
  }

  @Test
  public void testFindGroupUsersNoQueryNoFiltersNoUsersFound() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val groupId = groupService.getByName("Group One").getId().toString();

    val users = userService.findGroupUsers(
      groupId,
      Collections.emptyList(),
      new PageableResolver().getPageable()
    );

    assertThat(users.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindGroupUsersNoQueryFiltersEmptyGroupString() {
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestUsers();
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> userService.findGroupUsers("",
        Collections.emptyList(),
        new PageableResolver().getPageable())
      );
  }

  @Test
  public void testFindGroupUsersNoQueryFilters() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val user = userService.getByName("FirstUser@domain.com");
    val userTwo = (userService.getByName("SecondUser@domain.com"));
    val groupId = groupService.getByName("Group One").getId().toString();

    userService.addUserToGroups(user.getId().toString(), singletonList(groupId));
    userService.addUserToGroups(userTwo.getId().toString(), singletonList(groupId));

    val userFilters = new SearchFilter("name", "First");

    val users = userService.findGroupUsers(
      groupId,
      singletonList(userFilters),
      new PageableResolver().getPageable()
    );

    assertThat(users.getTotalElements()).isEqualTo(1L);
    assertThat(users.getContent()).contains(user);
  }

  @Test
  public void testFindGroupUsersQueryAndFilters() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val user = userService.getByName("FirstUser@domain.com");
    val userTwo = (userService.getByName("SecondUser@domain.com"));
    val groupId = groupService.getByName("Group One").getId().toString();

    userService.addUserToGroups(user.getId().toString(), singletonList(groupId));
    userService.addUserToGroups(userTwo.getId().toString(), singletonList(groupId));

    val userFilters = new SearchFilter("name", "First");

    val users = userService.findGroupUsers(
      groupId,
      "Second",
      singletonList(userFilters),
      new PageableResolver().getPageable()
    );

    assertThat(users.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindGroupUsersQueryNoFilters() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val user = userService.getByName("FirstUser@domain.com");
    val userTwo = (userService.getByName("SecondUser@domain.com"));
    val groupId = groupService.getByName("Group One").getId().toString();

    userService.addUserToGroups(user.getId().toString(), singletonList(groupId));
    userService.addUserToGroups(userTwo.getId().toString(), singletonList(groupId));


    val users = userService.findGroupUsers(
      groupId,
      "Second",
      Collections.emptyList(),
      new PageableResolver().getPageable()
    );

    assertThat(users.getTotalElements()).isEqualTo(1L);
    assertThat(users.getContent()).contains(userTwo);
  }

  // Find App Users

  @Test
  public void testFindAppUsersNoQueryNoFilters() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val user = userService.getByName("FirstUser@domain.com");
    val userTwo = (userService.getByName("SecondUser@domain.com"));
    val appId = applicationService.getByClientId("111111").getId().toString();

    userService.addUserToApps(user.getId().toString(), singletonList(appId));
    userService.addUserToApps(userTwo.getId().toString(), singletonList(appId));

    val users = userService.findAppUsers(
      appId,
      Collections.emptyList(),
      new PageableResolver().getPageable()
    );

    assertThat(users.getTotalElements()).isEqualTo(2L);
    assertThat(users.getContent()).contains(user, userTwo);
  }

  @Test
  public void testFindAppUsersNoQueryNoFiltersNoUser() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val appId = applicationService.getByClientId("111111").getId().toString();

    val users = userService.findAppUsers(
      appId,
      Collections.emptyList(),
      new PageableResolver().getPageable()
    );

    assertThat(users.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindAppUsersNoQueryNoFiltersEmptyUserString() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> userService
        .findAppUsers(
          "",
          Collections.emptyList(),
          new PageableResolver().getPageable()
        )
      );
  }

  @Test
  public void testFindAppUsersNoQueryFilters() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val user = userService.getByName("FirstUser@domain.com");
    val userTwo = (userService.getByName("SecondUser@domain.com"));
    val appId = applicationService.getByClientId("111111").getId().toString();

    userService.addUserToApps(user.getId().toString(), singletonList(appId));
    userService.addUserToApps(userTwo.getId().toString(), singletonList(appId));

    val userFilters = new SearchFilter("name", "First");

    val users = userService.findAppUsers(
      appId,
      singletonList(userFilters),
      new PageableResolver().getPageable()
    );

    assertThat(users.getTotalElements()).isEqualTo(1L);
    assertThat(users.getContent()).contains(user);
  }

  @Test
  public void testFindAppUsersQueryAndFilters() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val user = userService.getByName("FirstUser@domain.com");
    val userTwo = (userService.getByName("SecondUser@domain.com"));
    val appId = applicationService.getByClientId("111111").getId().toString();

    userService.addUserToApps(user.getId().toString(), singletonList(appId));
    userService.addUserToApps(userTwo.getId().toString(), singletonList(appId));

    val userFilters = new SearchFilter("name", "First");

    val users = userService.findAppUsers(
      appId,
      "Second",
      singletonList(userFilters),
      new PageableResolver().getPageable()
    );

    assertThat(users.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindAppUsersQueryNoFilters() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val user = userService.getByName("FirstUser@domain.com");
    val userTwo = (userService.getByName("SecondUser@domain.com"));
    val appId = applicationService.getByClientId("111111").getId().toString();

    userService.addUserToApps(user.getId().toString(), singletonList(appId));
    userService.addUserToApps(userTwo.getId().toString(), singletonList(appId));

    val users = userService.findAppUsers(
      appId,
      "First",
      Collections.emptyList(),
      new PageableResolver().getPageable()
    );

    assertThat(users.getTotalElements()).isEqualTo(1L);
    assertThat(users.getContent()).contains(user);
  }

  // Update
  @Test
  public void testUpdate() {
    val user = entityGenerator.setupUser("First User");
    user.setFirstName("NotFirst");
    val updated = userService.update(user);
    assertThat(updated.getFirstName()).isEqualTo("NotFirst");
  }

  @Test
  public void testUpdateRoleUser() {
    val user = entityGenerator.setupUser("First User");
    user.setRole("user");
    val updated = userService.update(user);
    assertThat(updated.getRole()).isEqualTo("USER");
  }

  @Test
  public void testUpdateRoleAdmin() {
    val user = entityGenerator.setupUser("First User");
    user.setRole("admin");
    val updated = userService.update(user);
    assertThat(updated.getRole()).isEqualTo("ADMIN");
  }

  @Test
  public void testUpdateNonexistentEntity() {
    userService.create(entityGenerator.createUser("First", "User"));
    val nonExistentEntity = entityGenerator.createUser("First", "User");
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
      .isThrownBy(() -> userService.update(nonExistentEntity));
  }


  @Test
  public void testUpdateIdNotAllowed() {
    val user = userService.create(entityGenerator.createUser("First", "User"));
    user.setId(UUID.fromString("0c1dc4b8-7fb8-11e8-adc0-fa7ae01bbebc"));
    // New id means new non-existent policy or one that exists and is being overwritten
    assertThatExceptionOfType(EntityNotFoundException.class)
      .isThrownBy(() -> userService.update(user));
  }

  @Test
  @Ignore
  public void testUpdateNameNotAllowed() {
//    val user = userService.create(entityGenerator.createUser(Pair.of("First", "User")));
//    user.setName("NewName");
//    val updated = userService.update(user);
    assertThat(1).isEqualTo(2);
    // TODO Check for uniqueness in application, currently only SQL
  }

  @Test
  @Ignore
  public void testUpdateEmailNotAllowed() {
//    val user = userService.create(entityGenerator.createUser(Pair.of("First", "User")));
//    user.setEmail("NewName@domain.com");
//    val updated = userService.update(user);
    assertThat(1).isEqualTo(2);
    // TODO Check for uniqueness in application, currently only SQL
  }

  @Test
  @Ignore
  public void testUpdateStatusNotInAllowedEnum() {
//    entityGenerator.setupTestUsers();
//    val user = userService.getByName("FirstUser@domain.com");
//    user.setStatus("Junk");
//    val updated = userService.update(user);
    assertThat(1).isEqualTo(2);
    // TODO Check for uniqueness in application, currently only SQL
  }

  @Test
  @Ignore
  public void testUpdateLanguageNotInAllowedEnum() {
//    entityGenerator.setupTestUsers();
//    val user = userService.getByName("FirstUser@domain.com");
//    user.setPreferredLanguage("Klingon");
//    val updated = userService.update(user);
    assertThat(1).isEqualTo(2);
    // TODO Check for uniqueness in application, currently only SQL
  }

  // Add User to Groups
  @Test
  public void addUserToGroups() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group One");
    val groupId = group.getId().toString();
    val groupTwo = groupService.getByName("Group Two");
    val groupTwoId = groupTwo.getId().toString();
    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId().toString();

    userService.addUserToGroups(userId, asList(groupId, groupTwoId));

    val groups = groupService.findUserGroups(
      userId,
      Collections.emptyList(),
      new PageableResolver().getPageable()
    );

    assertThat(groups.getContent()).contains(group, groupTwo);
  }

  @Test
  public void addUserToGroupsNoUser() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group One");
    val groupId = group.getId().toString();

    assertThatExceptionOfType(EntityNotFoundException.class)
      .isThrownBy(() -> userService.addUserToGroups(NON_EXISTENT_USER, singletonList(groupId)));
  }

  @Test
  public void addUserToGroupsEmptyUserString() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group One");
    val groupId = group.getId().toString();

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> userService.addUserToGroups("", singletonList(groupId)));
  }

  @Test
  public void addUserToGroupsWithGroupsListOneEmptyString() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId().toString();

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> userService.addUserToGroups(userId, singletonList("")));
  }

  @Test
  public void addUserToGroupsEmptyGroupsList() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId().toString();

    userService.addUserToGroups(userId, Collections.emptyList());

    val nonUpdated = userService.getByName("FirstUser@domain.com");
    assertThat(nonUpdated).isEqualTo(user);
  }

  // Add User to Apps
  @Test
  public void addUserToApps() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val app = applicationService.getByClientId("111111");
    val appId = app.getId().toString();
    val appTwo = applicationService.getByClientId("222222");
    val appTwoId = appTwo.getId().toString();
    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId().toString();

    userService.addUserToApps(userId, asList(appId, appTwoId));

    val apps = applicationService.findUserApps(
      userId,
      Collections.emptyList(),
      new PageableResolver().getPageable()
    );

    assertThat(apps.getContent()).contains(app, appTwo);
  }

  @Test
  public void addUserToAppsNoUser() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val app = applicationService.getByClientId("111111");
    val appId = app.getId().toString();

    assertThatExceptionOfType(EntityNotFoundException.class)
      .isThrownBy(() -> userService.addUserToApps(NON_EXISTENT_USER, singletonList(appId)));
  }

  @Test
  public void addUserToAppsWithAppsListOneEmptyString() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId().toString();

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> userService.addUserToApps(userId, singletonList("")));
  }

  @Test
  public void addUserToAppsEmptyAppsList() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId().toString();

    userService.addUserToApps(userId, Collections.emptyList());

    val nonUpdated = userService.getByName("FirstUser@domain.com");
    assertThat(nonUpdated).isEqualTo(user);
  }

  // Delete
  @Test
  public void testDelete() {
    entityGenerator.setupTestUsers();

    val user = userService.getByName("FirstUser@domain.com");

    userService.delete(user.getId().toString());

    val users = userService.listUsers(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(users.getTotalElements()).isEqualTo(2L);
    assertThat(users.getContent()).doesNotContain(user);
  }

  @Test
  public void testDeleteNonExisting() {
    entityGenerator.setupTestUsers();
    assertThatExceptionOfType(EmptyResultDataAccessException.class)
      .isThrownBy(() -> userService.delete(NON_EXISTENT_USER));
  }

  @Test
  public void testDeleteEmptyIdString() {
    entityGenerator.setupTestGroups();
    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> userService.delete(""));
  }

  // Delete User from Group
  @Test
  public void testDeleteUserFromGroup() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group One");
    val groupId = group.getId().toString();
    val groupTwo = groupService.getByName("Group Two");
    val groupTwoId = groupTwo.getId().toString();
    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId().toString();

    userService.addUserToGroups(userId, asList(groupId, groupTwoId));

    userService.deleteUserFromGroups(userId, singletonList(groupId));

    val groupWithoutUser = groupService.findUserGroups(
      userId,
      Collections.emptyList(),
      new PageableResolver().getPageable()
    );

    assertThat(groupWithoutUser.getContent()).containsOnly(groupTwo);
  }

  @Test
  public void testDeleteUserFromGroupNoUser() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group One");
    val groupId = group.getId().toString();
    val groupTwo = groupService.getByName("Group Two");
    val groupTwoId = groupTwo.getId().toString();
    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId().toString();

    userService.addUserToGroups(userId, asList(groupId, groupTwoId));

    assertThatExceptionOfType(EntityNotFoundException.class)
      .isThrownBy(() -> userService
        .deleteUserFromGroups(NON_EXISTENT_USER, singletonList(groupId)));
  }

  @Test
  public void testDeleteUserFromGroupEmptyUserString() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group One");
    val groupId = group.getId().toString();
    val groupTwo = groupService.getByName("Group Two");
    val groupTwoId = groupTwo.getId().toString();
    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId().toString();

    userService.addUserToGroups(userId, asList(groupId, groupTwoId));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> userService
        .deleteUserFromGroups("", singletonList(groupId)));
  }

  @Test
  public void testDeleteUserFromGroupEmptyGroupsList() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId().toString();
    val group = groupService.getByName("Group One");
    val groupId = group.getId().toString();

    userService.addUserToGroups(userId, singletonList(groupId));
    assertThat(user.getWholeGroups().size()).isEqualTo(1);

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> userService
        .deleteUserFromGroups(userId, singletonList("")));
  }

  // Delete User from App
  @Test
  public void testDeleteUserFromApp() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val app = applicationService.getByClientId("111111");
    val appId = app.getId().toString();
    val appTwo = applicationService.getByClientId("222222");
    val appTwoId = appTwo.getId().toString();
    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId().toString();

    userService.addUserToApps(userId, asList(appId, appTwoId));

    userService.deleteUserFromApps(userId, singletonList(appId));

    val groupWithoutUser = applicationService.findUserApps(
      userId,
      Collections.emptyList(),
      new PageableResolver().getPageable()
    );

    assertThat(groupWithoutUser.getContent()).containsOnly(appTwo);
  }

  @Test
  public void testDeleteUserFromAppNoUser() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val app = applicationService.getByClientId("111111");
    val appId = app.getId().toString();
    val appTwo = applicationService.getByClientId("222222");
    val appTwoId = appTwo.getId().toString();
    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId().toString();

    userService.addUserToApps(userId, asList(appId, appTwoId));

    assertThatExceptionOfType(EntityNotFoundException.class)
      .isThrownBy(() -> userService
        .deleteUserFromApps(NON_EXISTENT_USER, singletonList(appId)));
  }

  @Test
  public void testDeleteUserFromAppEmptyUserString() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val app = applicationService.getByClientId("111111");
    val appId = app.getId().toString();
    val appTwo = applicationService.getByClientId("222222");
    val appTwoId = appTwo.getId().toString();
    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId().toString();

    userService.addUserToApps(userId, asList(appId, appTwoId));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> userService
        .deleteUserFromApps("", singletonList(appId)));
  }

  @Test
  public void testDeleteUserFromAppEmptyAppsList() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val app = applicationService.getByClientId("111111");
    val appId = app.getId().toString();
    val appTwo = applicationService.getByClientId("222222");
    val appTwoId = appTwo.getId().toString();
    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId().toString();

    userService.addUserToApps(userId, asList(appId, appTwoId));

    assertThatExceptionOfType(IllegalArgumentException.class)
      .isThrownBy(() -> userService
        .deleteUserFromApps(userId, singletonList("")));
  }

  @Test
  public void testAddUserPermissions() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestPolicies();

    val user = userService.getByName("FirstUser@domain.com");

    val study001 = policyService.getByName("Study001");
    val study001id = study001.getId().toString();

    val study002 = policyService.getByName("Study002");
    val study002id = study002.getId().toString();

    val study003 = policyService.getByName("Study003");
    val study003id = study003.getId().toString();

    val permissions = asList(
        new PolicyIdStringWithAccessLevel(study001id, "READ"),
        new PolicyIdStringWithAccessLevel(study002id, "WRITE"),
        new PolicyIdStringWithAccessLevel(study003id, "DENY")
    );

    userService.addUserPermissions(user.getId().toString(), permissions);

    assertThat(PolicyPermissionUtils.extractPermissionStrings(user.getUserPermissions()))
        .containsExactlyInAnyOrder(
            "Study001.READ",
            "Study002.WRITE",
            "Study003.DENY"
        );
  }

  @Test
  public void testRemoveUserPermissions() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestPolicies();

    val user = userService.getByName("FirstUser@domain.com");

    val study001 = policyService.getByName("Study001");
    val study001id = study001.getId().toString();

    val study002 = policyService.getByName("Study002");
    val study002id = study002.getId().toString();

    val study003 = policyService.getByName("Study003");
    val study003id = study003.getId().toString();

    val permissions = asList(
        new PolicyIdStringWithAccessLevel(study001id, "READ"),
        new PolicyIdStringWithAccessLevel(study002id, "WRITE"),
        new PolicyIdStringWithAccessLevel(study003id, "DENY")
    );

    userService.addUserPermissions(user.getId().toString(), permissions);

    val userPermissionsToRemove = user.getUserPermissions()
        .stream()
        .filter(p -> !p.getPolicy().getName().equals("Study001"))
        .map(p -> p.getId().toString())
        .collect(Collectors.toList());

    userService.deleteUserPermissions(user.getId().toString(), userPermissionsToRemove);

    assertThat(PolicyPermissionUtils.extractPermissionStrings(user.getUserPermissions()))
        .containsExactlyInAnyOrder(
            "Study001.READ"
        );
  }

  @Test
  public void testGetUserPermissions() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestPolicies();

    val user = userService.getByName("FirstUser@domain.com");

    val study001 = policyService.getByName("Study001");
    val study001id = study001.getId().toString();

    val study002 = policyService.getByName("Study002");
    val study002id = study002.getId().toString();

    val study003 = policyService.getByName("Study003");
    val study003id = study003.getId().toString();

    val permissions = asList(
        new PolicyIdStringWithAccessLevel(study001id, "READ"),
        new PolicyIdStringWithAccessLevel(study002id, "WRITE"),
        new PolicyIdStringWithAccessLevel(study003id, "DENY")
    );

    userService.addUserPermissions(user.getId().toString(), permissions);

    val pagedUserPermissions = userService.getUserPermissions(user.getId().toString(), new PageableResolver().getPageable());

    assertThat(pagedUserPermissions.getTotalElements()).isEqualTo(3L);
  }
}
