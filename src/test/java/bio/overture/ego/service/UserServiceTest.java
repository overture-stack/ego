package bio.overture.ego.service;

import static bio.overture.ego.model.enums.AccessLevel.DENY;
import static bio.overture.ego.model.enums.AccessLevel.READ;
import static bio.overture.ego.model.enums.AccessLevel.WRITE;
import static bio.overture.ego.model.enums.LanguageType.ENGLISH;
import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static bio.overture.ego.model.enums.StatusType.DISABLED;
import static bio.overture.ego.model.enums.StatusType.PENDING;
import static bio.overture.ego.model.enums.UserType.ADMIN;
import static bio.overture.ego.model.enums.UserType.USER;
import static bio.overture.ego.service.UserService.USER_CONVERTER;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentId;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import bio.overture.ego.controller.resolver.PageableResolver;
import bio.overture.ego.model.dto.CreateUserRequest;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.dto.UpdateUserRequest;
import bio.overture.ego.model.entity.AbstractPermission;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.model.exceptions.UniqueViolationException;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.service.join.UserGroupJoinService;
import bio.overture.ego.token.IDToken;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.PolicyPermissionUtils;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
@Ignore("replace with controller tests.")
public class UserServiceTest {

  private static final UUID NON_EXISTENT_USER =
      UUID.fromString("827fae28-7fb8-11e8-adc0-fa7ae01bbebc");

  @Autowired private ApplicationService applicationService;
  @Autowired private UserService userService;
  @Autowired private GroupService groupService;
  @Autowired private PolicyService policyService;
  @Autowired private EntityGenerator entityGenerator;
  @Autowired private UserPermissionService userPermissionService;
  @Autowired private UserGroupJoinService userGroupJoinService;

  @Test
  public void userConverter_UpdateUserRequest_User() {
    val email = System.currentTimeMillis() + "@gmail.com";
    val firstName = "John";
    val lastName = "Doe";
    val userType = ADMIN;
    val status = APPROVED;
    val preferredLanguage = ENGLISH;
    val id = randomUUID();
    val createdAt = new Date();

    val applications =
        IntStream.range(0, 3)
            .boxed()
            .map(x -> Application.builder().id(randomUUID()).build())
            .collect(toImmutableSet());

    val user =
        User.builder()
            .email(email)
            .firstName(firstName)
            .lastName(lastName)
            .type(userType)
            .status(status)
            .preferredLanguage(preferredLanguage)
            .id(id)
            .createdAt(createdAt)
            .applications(applications)
            .userPermissions(null)
            .build();

    val partialUserUpdateRequest =
        UpdateUserRequest.builder().firstName("Rob").status(DISABLED).build();
    USER_CONVERTER.updateUser(partialUserUpdateRequest, user);

    assertThat(user.getPreferredLanguage()).isEqualTo(preferredLanguage);
    assertThat(user.getCreatedAt()).isEqualTo(createdAt);
    assertThat(user.getStatus()).isEqualTo(DISABLED);
    assertThat(user.getLastName()).isEqualTo(lastName);
    assertThat(user.getName()).isEqualTo(email);
    assertThat(user.getEmail()).isEqualTo(email);
    assertThat(user.getFirstName()).isEqualTo("Rob");
    assertThat(user.getType()).isEqualTo(userType);
    assertThat(user.getId()).isEqualTo(id);
    assertThat(user.getApplications()).containsExactlyInAnyOrderElementsOf(applications);
    assertThat(user.getUserPermissions()).isNull();
    assertThat(user.getUserGroups()).isEmpty();
  }

  @Test
  public void userConversion_CreateUserRequest_User() {
    val t = System.currentTimeMillis();
    val request =
        CreateUserRequest.builder()
            .email(t + "@gmail.com")
            .firstName("John")
            .type(ADMIN)
            .status(APPROVED)
            .preferredLanguage(ENGLISH)
            .build();
    val user = USER_CONVERTER.convertToUser(request);
    assertThat(user.getEmail()).isEqualTo(request.getEmail());
    assertThat(user.getName()).isEqualTo(user.getEmail());
    assertThat(user.getCreatedAt()).isNotNull();
    assertThat(user.getId()).isNull();
    assertThat(user.getLastName()).isNull();
    assertThat(user.getFirstName()).isEqualTo(request.getFirstName());
    assertThat(user.getType()).isEqualTo(request.getType());
    assertThat(user.getStatus()).isEqualTo(request.getStatus());
    assertThat(user.getPreferredLanguage()).isEqualTo(request.getPreferredLanguage());
    assertThat(user.getUserGroups()).isEmpty();
    assertThat(user.getUserPermissions()).isEmpty();
    assertThat(user.getApplications()).isEmpty();
  }

  // Create
  @Test
  public void testCreate() {
    val user = entityGenerator.setupUser("Demo User");
    // UserName == UserEmail
    assertThat(user.getName()).isEqualTo("DemoUser@domain.com");
  }

  @Test
  public void testCreateFromIDToken() {
    val idToken =
        IDToken.builder()
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
    assertThat(idTokenUser.getType()).isEqualTo("USER");
  }

  @Test
  public void testCreateFromIDTokenUniqueNameAndEmail() {
    // Note: This test has one strike due to Hibernate Cache.
    entityGenerator.setupUser("User One");
    val idToken =
        IDToken.builder().email("UserOne@domain.com").given_name("User").family_name("One").build();
    assertThatExceptionOfType(UniqueViolationException.class)
        .isThrownBy(() -> userService.createFromIDToken(idToken));
  }

  // Get
  @Test
  public void testGet() {
    val user = entityGenerator.setupUser("User One");
    val savedUser = userService.getById(user.getId());
    assertThat(savedUser.getName()).isEqualTo("UserOne@domain.com");
  }

  @Test
  public void testGetNotFoundException() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> userService.getById(NON_EXISTENT_USER));
  }

  @Test
  public void testGetByName() {
    entityGenerator.setupUser("User One");
    val savedUser = userService.getByName("UserOne@domain.com");
    assertThat(savedUser.getName()).isEqualTo("UserOne@domain.com");
  }

  @Test
  public void testGetByNameAllCaps() {
    entityGenerator.setupUser("User One");
    val savedUser = userService.getByName("USERONE@DOMAIN.COM");
    assertThat(savedUser.getName()).isEqualTo("UserOne@domain.com");
  }

  @Test
  @Ignore
  public void testGetByNameNotFound() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> userService.getByName("UserOne@domain.com"));
  }

  // List Users
  @Test
  public void testListUsersNoFilters() {
    entityGenerator.setupTestUsers();
    val users =
        userService.listUsers(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(users.getTotalElements()).isEqualTo(3L);
  }

  @Test
  public void testListUsersNoFiltersEmptyResult() {
    val users =
        userService.listUsers(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(users.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testListUsersFiltered() {
    entityGenerator.setupTestUsers();
    val userFilter = new SearchFilter("email", "FirstUser@domain.com");
    val users =
        userService.listUsers(singletonList(userFilter), new PageableResolver().getPageable());
    assertThat(users.getTotalElements()).isEqualTo(1L);
  }

  @Test
  public void testListUsersFilteredEmptyResult() {
    entityGenerator.setupTestUsers();
    val userFilter = new SearchFilter("email", "FourthUser@domain.com");
    val users =
        userService.listUsers(singletonList(userFilter), new PageableResolver().getPageable());
    assertThat(users.getTotalElements()).isEqualTo(0L);
  }

  // Find Users
  @Test
  public void testFindUsersNoFilters() {
    entityGenerator.setupTestUsers();
    val users =
        userService.findUsers(
            "First", Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(users.getTotalElements()).isEqualTo(1L);
    assertThat(users.getContent().get(0).getName()).isEqualTo("FirstUser@domain.com");
  }

  @Test
  public void testFindUsersFiltered() {
    entityGenerator.setupTestUsers();
    val userFilter = new SearchFilter("email", "FirstUser@domain.com");
    val users =
        userService.findUsers(
            "Second", singletonList(userFilter), new PageableResolver().getPageable());
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
    val groupId = groupService.getByName("Group One").getId();

    userGroupJoinService.associate(user.getId(), singletonList(groupId));
    userGroupJoinService.associate(userTwo.getId(), singletonList(groupId));

    val users =
        userGroupJoinService.listUsersForGroup(groupId, new PageableResolver().getPageable());

    assertThat(users.getTotalElements()).isEqualTo(2L);
    assertThat(users.getContent()).contains(user, userTwo);
  }

  @Test
  public void testFindGroupUsersNoQueryNoFiltersNoUsersFound() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val groupId = groupService.getByName("Group One").getId();

    val users =
        userGroupJoinService.listUsersForGroup(groupId, new PageableResolver().getPageable());

    assertThat(users.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindGroupUsersNoQueryFilters() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val user = userService.getByName("FirstUser@domain.com");
    val userTwo = userService.getByName("SecondUser@domain.com");
    val groupId = groupService.getByName("Group One").getId();

    userGroupJoinService.associate(user.getId(), newArrayList(groupId));
    userGroupJoinService.associate(userTwo.getId(), newArrayList(groupId));

    val userFilters = new SearchFilter("name", "First");

    val users =
        userGroupJoinService.findUsersForGroup(
            groupId, newArrayList(userFilters), new PageableResolver().getPageable());

    assertThat(users.getTotalElements()).isEqualTo(1L);
    assertThat(users.getContent()).contains(user);
  }

  @Test
  public void testFindGroupUsersQueryAndFilters() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val user = userService.getByName("FirstUser@domain.com");
    val userTwo = (userService.getByName("SecondUser@domain.com"));
    val groupId = groupService.getByName("Group One").getId();

    userGroupJoinService.associate(user.getId(), singletonList(groupId));
    userGroupJoinService.associate(userTwo.getId(), singletonList(groupId));

    val userFilters = new SearchFilter("name", "First");

    val users =
        userGroupJoinService.findUsersForGroup(
            groupId, "Second", singletonList(userFilters), new PageableResolver().getPageable());

    assertThat(users.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindGroupUsersQueryNoFilters() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val user = userService.getByName("FirstUser@domain.com");
    val userTwo = (userService.getByName("SecondUser@domain.com"));
    val groupId = groupService.getByName("Group One").getId();

    userGroupJoinService.associate(user.getId(), singletonList(groupId));
    userGroupJoinService.associate(userTwo.getId(), singletonList(groupId));
    val users =
        userGroupJoinService.findUsersForGroup(
            groupId, "Second", Collections.emptyList(), new PageableResolver().getPageable());

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
    val appId = applicationService.getByClientId("111111").getId();

    userService.addUserToApps(user.getId(), singletonList(appId));
    userService.addUserToApps(userTwo.getId(), singletonList(appId));

    val users =
        userService.findAppUsers(
            appId, Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(users.getTotalElements()).isEqualTo(2L);
    assertThat(users.getContent()).contains(user, userTwo);
  }

  @Test
  public void testFindAppUsersNoQueryNoFiltersNoUser() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val appId = applicationService.getByClientId("111111").getId();

    val users =
        userService.findAppUsers(
            appId, Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(users.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindAppUsersNoQueryFilters() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val user = userService.getByName("FirstUser@domain.com");
    val userTwo = (userService.getByName("SecondUser@domain.com"));
    val appId = applicationService.getByClientId("111111").getId();

    userService.addUserToApps(user.getId(), singletonList(appId));
    userService.addUserToApps(userTwo.getId(), singletonList(appId));

    val userFilters = new SearchFilter("name", "First");

    val users =
        userService.findAppUsers(
            appId, singletonList(userFilters), new PageableResolver().getPageable());

    assertThat(users.getTotalElements()).isEqualTo(1L);
    assertThat(users.getContent()).contains(user);
  }

  @Test
  public void testFindAppUsersQueryAndFilters() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val user = userService.getByName("FirstUser@domain.com");
    val userTwo = (userService.getByName("SecondUser@domain.com"));
    val appId = applicationService.getByClientId("111111").getId();

    userService.addUserToApps(user.getId(), singletonList(appId));
    userService.addUserToApps(userTwo.getId(), singletonList(appId));

    val userFilters = new SearchFilter("name", "First");

    val users =
        userService.findAppUsers(
            appId, "Second", singletonList(userFilters), new PageableResolver().getPageable());

    assertThat(users.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindAppUsersQueryNoFilters() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val user = userService.getByName("FirstUser@domain.com");
    val userTwo = (userService.getByName("SecondUser@domain.com"));
    val appId = applicationService.getByClientId("111111").getId();

    userService.addUserToApps(user.getId(), singletonList(appId));
    userService.addUserToApps(userTwo.getId(), singletonList(appId));

    val users =
        userService.findAppUsers(
            appId, "First", Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(users.getTotalElements()).isEqualTo(1L);
    assertThat(users.getContent()).contains(user);
  }

  // Update
  @Test
  public void testUpdate() {
    val user = entityGenerator.setupUser("First User");
    val updated =
        userService.partialUpdate(
            user.getId(), UpdateUserRequest.builder().firstName("NotFirst").build());
    assertThat(updated.getFirstName()).isEqualTo("NotFirst");
  }

  @Test
  public void testUpdateTypeUser() {
    val user = entityGenerator.setupUser("First User");
    val updated =
        userService.partialUpdate(user.getId(), UpdateUserRequest.builder().type(USER).build());
    assertThat(updated.getType()).isEqualTo("USER");
  }

  @Test
  public void testUpdateUserTypeAdmin() {
    val user = entityGenerator.setupUser("First User");
    val updated =
        userService.partialUpdate(user.getId(), UpdateUserRequest.builder().type(ADMIN).build());
    assertThat(updated.getType()).isEqualTo("ADMIN");
  }

  @Test
  public void uniqueEmailCheck_CreateUser_ThrowsUniqueConstraintException() {
    val r1 =
        CreateUserRequest.builder()
            .preferredLanguage(ENGLISH)
            .type(ADMIN)
            .status(APPROVED)
            .email(UUID.randomUUID() + "@gmail.com")
            .build();

    val u1 = userService.create(r1);
    assertThat(userService.isExist(u1.getId())).isTrue();
    r1.setType(USER);
    r1.setStatus(PENDING);

    assertThat(u1.getEmail()).isEqualTo(r1.getEmail());
    assertThatExceptionOfType(UniqueViolationException.class)
        .isThrownBy(() -> userService.create(r1));
  }

  @Test
  public void uniqueEmailCheck_UpdateUser_ThrowsUniqueConstraintException() {
    val e1 = UUID.randomUUID().toString() + "@something.com";
    val e2 = UUID.randomUUID().toString() + "@something.com";
    val cr1 =
        CreateUserRequest.builder()
            .preferredLanguage(ENGLISH)
            .type(ADMIN)
            .status(APPROVED)
            .email(e1)
            .build();

    val cr2 =
        CreateUserRequest.builder()
            .preferredLanguage(ENGLISH)
            .type(USER)
            .status(PENDING)
            .email(e2)
            .build();

    val u1 = userService.create(cr1);
    assertThat(userService.isExist(u1.getId())).isTrue();
    val u2 = userService.create(cr2);
    assertThat(userService.isExist(u2.getId())).isTrue();

    val ur3 = UpdateUserRequest.builder().email(e1).build();

    assertThat(u1.getEmail()).isEqualTo(ur3.getEmail());
    assertThat(u2.getEmail()).isNotEqualTo(ur3.getEmail());
    assertThatExceptionOfType(UniqueViolationException.class)
        .isThrownBy(() -> userService.partialUpdate(u2.getId(), ur3));
  }

  @Test
  public void testUpdateNonexistentEntity() {
    val nonExistentId = generateNonExistentId(userService);
    val updateRequest =
        UpdateUserRequest.builder()
            .firstName("Doesnot")
            .lastName("Exist")
            .status(APPROVED)
            .preferredLanguage(ENGLISH)
            .lastLogin(null)
            .type(ADMIN)
            .build();
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> userService.partialUpdate(nonExistentId, updateRequest));
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
    val groupId = group.getId();
    val groupTwo = groupService.getByName("Group Two");
    val groupTwoId = groupTwo.getId();
    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId();

    userGroupJoinService.associate(userId, asList(groupId, groupTwoId));

    val groups =
        groupService.findUserGroups(
            userId, Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(groups.getContent()).contains(group, groupTwo);
  }

  @Test
  public void addUserToGroupsNoUser() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group One");
    val groupId = group.getId();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> userGroupJoinService.associate(NON_EXISTENT_USER, singletonList(groupId)));
  }

  @Test
  public void addUserToGroupsWithGroupsListOneEmptyString() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId();

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> userGroupJoinService.associate(userId, ImmutableList.of()));
  }

  @Test
  public void addUserToGroupsEmptyGroupsList() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId();

    userGroupJoinService.associate(userId, Collections.emptyList());

    val nonUpdated = userService.getByName("FirstUser@domain.com");
    assertThat(nonUpdated).isEqualTo(user);
  }

  // Add User to Apps
  @Test
  public void addUserToApps() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val app = applicationService.getByClientId("111111");
    val appId = app.getId();
    val appTwo = applicationService.getByClientId("222222");
    val appTwoId = appTwo.getId();
    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId();

    userService.addUserToApps(userId, asList(appId, appTwoId));

    val apps =
        applicationService.findUserApps(
            userId, Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(apps.getContent()).contains(app, appTwo);
  }

  @Test
  public void addUserToAppsNoUser() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val app = applicationService.getByClientId("111111");
    val appId = app.getId();

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> userService.addUserToApps(NON_EXISTENT_USER, singletonList(appId)));
  }

  @Test
  public void addUserToAppsWithAppsListOneEmptyString() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId();

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> userService.addUserToApps(userId, ImmutableList.of()));
  }

  @Test
  public void addUserToAppsEmptyAppsList() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId();

    userService.addUserToApps(userId, Collections.emptyList());

    val nonUpdated = userService.getByName("FirstUser@domain.com");
    assertThat(nonUpdated).isEqualTo(user);
  }

  // Delete
  @Test
  public void testDelete() {
    entityGenerator.setupTestUsers();

    val usersBefore =
        userService.listUsers(Collections.emptyList(), new PageableResolver().getPageable());

    val user = userService.getByName("FirstUser@domain.com");

    userService.delete(user.getId());

    val usersAfter =
        userService.listUsers(Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(usersBefore.getTotalElements() - usersAfter.getTotalElements()).isEqualTo(1L);
    assertThat(usersAfter.getContent()).doesNotContain(user);
  }

  @Test
  public void testDeleteNonExisting() {
    entityGenerator.setupTestUsers();
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> userService.delete(NON_EXISTENT_USER));
  }

  // Delete User from Group
  @Test
  public void testDeleteUserFromGroup() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group One");
    val groupId = group.getId();
    val groupTwo = groupService.getByName("Group Two");
    val groupTwoId = groupTwo.getId();
    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId();

    userGroupJoinService.associate(userId, asList(groupId, groupTwoId));

    userGroupJoinService.disassociate(userId, singletonList(groupId));

    val groupWithoutUser =
        groupService.findUserGroups(
            userId, Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(groupWithoutUser.getContent()).containsOnly(groupTwo);
  }

  @Test
  public void testDeleteUserFromGroupNoUser() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group One");
    val groupId = group.getId();
    val groupTwo = groupService.getByName("Group Two");
    val groupTwoId = groupTwo.getId();
    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId();

    userGroupJoinService.associate(userId, asList(groupId, groupTwoId));

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> userGroupJoinService.disassociate(NON_EXISTENT_USER, singletonList(groupId)));
  }

  @Test
  public void testDeleteUserFromGroupEmptyGroupsList() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();

    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId();
    val group = groupService.getByName("Group One");
    val groupId = group.getId();

    userGroupJoinService.associate(userId, singletonList(groupId));
    assertThat(user.getUserGroups().size()).isEqualTo(1);

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> userGroupJoinService.disassociate(userId, ImmutableList.of()));
  }

  // Delete User from App
  @Test
  public void testDeleteUserFromApp() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val app = applicationService.getByClientId("111111");
    val appId = app.getId();
    val appTwo = applicationService.getByClientId("222222");
    val appTwoId = appTwo.getId();
    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId();

    userService.addUserToApps(userId, asList(appId, appTwoId));

    userService.deleteUserFromApps(userId, singletonList(appId));

    val groupWithoutUser =
        applicationService.findUserApps(
            userId, Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(groupWithoutUser.getContent()).containsOnly(appTwo);
  }

  @Test
  public void testDeleteUserFromAppNoUser() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val app = applicationService.getByClientId("111111");
    val appId = app.getId();
    val appTwo = applicationService.getByClientId("222222");
    val appTwoId = appTwo.getId();
    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId();

    userService.addUserToApps(userId, asList(appId, appTwoId));

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> userService.deleteUserFromApps(NON_EXISTENT_USER, singletonList(appId)));
  }

  @Test
  public void testDeleteUserFromAppEmptyAppsList() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestApplications();

    val app = applicationService.getByClientId("111111");
    val appId = app.getId();
    val appTwo = applicationService.getByClientId("222222");
    val appTwoId = appTwo.getId();
    val user = userService.getByName("FirstUser@domain.com");
    val userId = user.getId();

    userService.addUserToApps(userId, asList(appId, appTwoId));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> userService.deleteUserFromApps(userId, ImmutableList.of()));
  }

  @Test
  public void testAddUserPermissions() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestPolicies();

    val user = userService.getByName("FirstUser@domain.com");

    val study001 = policyService.getByName("Study001");
    val study001id = study001.getId();

    val study002 = policyService.getByName("Study002");
    val study002id = study002.getId();

    val study003 = policyService.getByName("Study003");
    val study003id = study003.getId();

    val permissions =
        asList(
            new PermissionRequest(study001id, READ),
            new PermissionRequest(study002id, WRITE),
            new PermissionRequest(study003id, DENY));

    userPermissionService.addPermissions(user.getId(), permissions);

    assertThat(PolicyPermissionUtils.extractPermissionStrings(user.getUserPermissions()))
        .containsExactlyInAnyOrder("Study001.READ", "Study002.WRITE", "Study003.DENY");
  }

  @Test
  public void testRemoveUserPermissions() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestPolicies();

    val user = userService.getByName("FirstUser@domain.com");

    val study001 = policyService.getByName("Study001");
    val study001id = study001.getId();

    val study002 = policyService.getByName("Study002");
    val study002id = study002.getId();

    val study003 = policyService.getByName("Study003");
    val study003id = study003.getId();

    val permissions =
        asList(
            new PermissionRequest(study001id, READ),
            new PermissionRequest(study002id, WRITE),
            new PermissionRequest(study003id, DENY));

    userPermissionService.addPermissions(user.getId(), permissions);

    val userPermissionsToRemove =
        user.getUserPermissions()
            .stream()
            .filter(p -> !p.getPolicy().getName().equals("Study001"))
            .map(AbstractPermission::getId)
            .collect(Collectors.toList());

    userPermissionService.deletePermissions(user.getId(), userPermissionsToRemove);

    assertThat(PolicyPermissionUtils.extractPermissionStrings(user.getUserPermissions()))
        .containsExactlyInAnyOrder("Study001.READ");
  }

  @Test
  public void testGetUserPermissions() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestPolicies();

    val user = userService.getByName("FirstUser@domain.com");

    val study001 = policyService.getByName("Study001");
    val study001id = study001.getId();

    val study002 = policyService.getByName("Study002");
    val study002id = study002.getId();

    val study003 = policyService.getByName("Study003");
    val study003id = study003.getId();

    val permissions =
        asList(
            new PermissionRequest(study001id, READ),
            new PermissionRequest(study002id, WRITE),
            new PermissionRequest(study003id, DENY));

    userPermissionService.addPermissions(user.getId(), permissions);

    val pagedUserPermissions =
        userPermissionService.getPermissions(user.getId(), new PageableResolver().getPageable());

    assertThat(pagedUserPermissions.getTotalElements()).isEqualTo(3L);
  }
}
