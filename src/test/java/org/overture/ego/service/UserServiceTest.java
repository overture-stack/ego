package org.overture.ego.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.overture.ego.utils.EntityGenerator;
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

  }

  @Test
  public void testCreateUniqueName() {

  }

  @Test
  public void testCreateUniqueEmail() {

  }

  @Test
  public void testCreateFromIDToken() {

  }

  @Test
  public void testCreateFromIDTokenUniqueName() {

  }

  @Test
  public void testCreateFromIDTokenUniqueEmail() {

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
  public void testListAppsNoFiltersEmptyResult() {

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
  public void testUpdateClientIdNotAllowed() {

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
  public void addUserToGroupsEmptyGroupString() {

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
