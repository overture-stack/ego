package org.overture.ego.service;

import org.junit.Test;
import org.overture.ego.utils.EntityGenerator;
import org.springframework.beans.factory.annotation.Autowired;

public class GroupsServiceTest {
  @Autowired
  private ApplicationService applicationService;

  @Autowired
  private GroupService userService;

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

  // List Groups
  @Test
  public void testListGroupsNoFilters() {

  }

  @Test
  public void testListGroupsNoFiltersEmptyResult() {

  }

  @Test
  public void testListGroupsFiltered() {

  }

  @Test
  public void testListGroupsFilteredEmptyResult() {

  }

  // Find Groups
  @Test
  public void testFindGroupsNoFilters() {

  }

  @Test
  public void testFindGroupsFiltered() {

  }

  // Find User's Groups
  @Test
  public void testFindUsersGroupsNoQueryNoFiltersNoGroup() {

  }

  @Test
  public void testFindUsersGroupsNoQueryNoFiltersEmptyGroupString() {

  }

  @Test
  public void testFindUsersGroupsNoQueryFilters() {

  }

  @Test
  public void testFindUsersGroupsQueryAndFilters() {

  }

  @Test
  public void testFindUsersGroupsQueryNoFilters() {

  }

  // Find Application's Groups
  @Test
  public void testFindApplicationsGroupsNoQueryNoFiltersNoGroup() {

  }

  @Test
  public void testFindApplicationsGroupsNoQueryNoFiltersEmptyGroupString() {

  }

  @Test
  public void testFindApplicationsGroupsNoQueryFilters() {

  }

  @Test
  public void testFindApplicationsGroupsQueryAndFilters() {

  }

  @Test
  public void testFindApplicationsGroupsQueryNoFilters() {

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

  // Add Apps to Group
  @Test
  public void addAppsToGroup() {

  }

  @Test
  public void addAppsToGroupNoGroup() {

  }

  @Test
  public void addAppsToGroupEmptyGroupString() {

  }

  @Test
  public void addAppsToGroupNoApp() {

  }

  @Test
  public void addAppsToGroupEmptyAppList() {

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

  // Delete Apps from Group
  @Test
  public void testDeleteAppFromGroup() {

  }

  @Test
  public void testDeleteAppsFromGroupNoGroup() {

  }

  @Test
  public void testDeleteAppsFromGroupEmptyGroupString() {

  }

  @Test
  public void testDeleteAppsFromGroupNoApps() {

  }

  @Test
  public void testDeleteAppsFromGroupEmptyAppsList() {

  }
}
