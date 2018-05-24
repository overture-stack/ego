package org.overture.ego.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.overture.ego.token.IDToken;
import org.overture.ego.utils.EntityGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.util.Pair;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class UserServiceTest {
  @Autowired
  private ApplicationService applicationService;

  @Autowired
  private UserService userService;

  @Autowired
  private GroupService groupService;

  @Autowired
  private EntityGenerator entityGenerator;

  // Create
  @Test
  public void testCreate() {
    val user = userService.create(entityGenerator.createOneUser(Pair.of("Demo", "User")));
    // UserName == UserEmail
    assertThat(user.getName()).isEqualTo("DemoUser@domain.com");
  }

  @Test
  public void testCreateUniqueNameAndEmail() {
    userService.create(entityGenerator.createOneUser(Pair.of("User", "One")));
    userService.create(entityGenerator.createOneUser(Pair.of("User", "Two")));
    assertThatExceptionOfType(DataIntegrityViolationException.class)
        .isThrownBy(() -> userService.create(entityGenerator.createOneUser(Pair.of("User", "Two"))));
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
    assertThat(idTokenUser.getStatus()).isEqualTo("Pending");
    assertThat(idTokenUser.getRole()).isEqualTo("USER");
  }

  @Test
  public void testCreateFromIDTokenUniqueNameAndEmail() {
    userService.create(entityGenerator.createOneUser(Pair.of("User", "One")));
    val idToken = IDToken.builder()
        .email("UserOne@domain.com")
        .given_name("User")
        .family_name("User")
        .build();

    assertThatExceptionOfType(DataIntegrityViolationException.class)
        .isThrownBy(() -> userService.createFromIDToken(idToken));
  }

  // Get
  @Test
  public void testGet() {

  }

  @Test
  public void testGetEntityNotFoundException() {

  }

  @Test
  public void testGetByName() {

  }

  @Test
  public void testGetByNameAllCaps() {

  }

  @Test
  public void testGetByNameNotFound() {

  }

  @Test
  public void testGetOrCreateDemoUser() {

  }

  // List Users
  @Test
  public void testListUsersNoFilters() {

  }

  @Test
  public void testListUsersNoFiltersEmptyResult() {

  }

  @Test
  public void testListUsersFiltered() {

  }

  @Test
  public void testListUsersFilteredEmptyResult() {

  }

  // Find Users
  @Test
  public void testFindUsersNoFilters() {

  }

  @Test
  public void testFindUsersFiltered() {

  }

  // Find Group Users
  @Test
  public void testFindGroupUsersNoQueryNoFiltersNoUser() {

  }

  @Test
  public void testFindGroupUsersNoQueryNoFiltersEmptyUserString() {

  }

  @Test
  public void testFindGroupUsersNoQueryFilters() {

  }

  @Test
  public void testFindGroupUsersQueryAndFilters() {

  }

  @Test
  public void testFindGroupUsersQueryNoFilters() {

  }

  // Find App Users
  @Test
  public void testFindAppUsersNoQueryNoFiltersNoUser() {

  }

  @Test
  public void testFindAppUsersNoQueryNoFiltersEmptyUserString() {

  }

  @Test
  public void testFindAppUsersNoQueryFilters() {

  }

  @Test
  public void testFindAppUsersQueryAndFilters() {

  }

  @Test
  public void testFindAppUsersQueryNoFilters() {

  }

  // Update
  @Test
  public void testUpdate() {

  }

  @Test
  public void testUpdateNonexistentEntity() {

  }

  @Test
  public void testUpdateIdNotAllowed() {

  }

  @Test
  public void testUpdateNameNotAllowed() {

  }

  @Test
  public void testUpdateEmailNotAllowed() {

  }

  @Test
  public void testUpdateStatusNotInAllowedEnum() {

  }

  @Test
  public void testUpdateLanguageNotInAllowedEnum() {

  }

  // Add User to Groups
  @Test
  public void addUserToGroups() {

  }

  @Test
  public void addUserToGroupsNoGroup() {

  }

  @Test
  public void addUserToGroupsEmptyGroupsList() {

  }

  @Test
  public void addUserToGroupsNoUser() {

  }

  @Test
  public void addUserToGroupsEmptyUserString() {

  }

  // Add User to Apps
  @Test
  public void addUserToApps() {

  }

  @Test
  public void addUserToAppsNoApp() {

  }

  @Test
  public void addUserToAppsEmptyAppString() {

  }

  @Test
  public void addUserToAppsNoUser() {

  }

  @Test
  public void addUserToAppsEmptyUserString() {

  }

  // Delete
  @Test
  public void testDelete() {

  }

  @Test
  public void testDeleteNonExisting() {

  }

  @Test
  public void testDeleteEmptyIdString() {

  }

  // Delete User from Group
  @Test
  public void testDeleteUserFromGroup() {

  }

  @Test
  public void testDeleteUserFromGroupNoGroup() {

  }

  @Test
  public void testDeleteUserFromGroupEmptyGroupString() {

  }

  @Test
  public void testDeleteUserFromGroupNoUser() {

  }

  @Test
  public void testDeleteUserFromGroupEmptyUserString() {

  }

  // Delete User from App
  @Test
  public void testDeleteUserFromApp() {

  }

  @Test
  public void testDeleteUserFromAppNoApp() {

  }

  @Test
  public void testDeleteUserFromAppEmptyAppString() {

  }

  @Test
  public void testDeleteUserFromAppNoUser() {

  }

  @Test
  public void testDeleteUserFromAppEmptyUserString() {

  }
}
