package org.overture.ego.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.overture.ego.controller.resolver.PageableResolver;
import org.overture.ego.model.search.SearchFilter;
import org.overture.ego.utils.EntityGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class GroupsServiceTest {
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
    val group = groupService.create(entityGenerator.createOneGroup("Group One"));
    assertThat(group.getName()).isEqualTo("Group One");
  }

  @Test
  public void testCreateUniqueName() {
    groupService.create(entityGenerator.createOneGroup("Group One"));
    groupService.create(entityGenerator.createOneGroup("Group Two"));
    assertThatExceptionOfType(DataIntegrityViolationException.class)
        .isThrownBy(() -> groupService.create(entityGenerator.createOneGroup("Group One")));
  }

  // Get
  @Test
  public void testGet() {
    val group = groupService.create(entityGenerator.createOneGroup("Group One"));
    val saveGroup = groupService.get(Integer.toString(group.getId()));
    assertThat(saveGroup.getName()).isEqualTo("Group One");
  }

  @Test
  public void testGetEntityNotFoundException() {
    assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> groupService.get("1"));
  }

  @Test
  public void testGetByName() {
    groupService.create(entityGenerator.createOneGroup("Group One"));
    val saveGroup = groupService.getByName("Group One");
    assertThat(saveGroup.getName()).isEqualTo("Group One");
  }

  @Test
  public void testGetByNameAllCaps() {
    groupService.create(entityGenerator.createOneGroup("Group One"));
    val saveGroup = groupService.getByName("GROUP ONE");
    assertThat(saveGroup.getName()).isEqualTo("Group One");
  }

  @Test
  public void testGetByNameNotFound() {
    // TODO Currently returning null, should throw exception (EntityNotFoundException?)
    assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> groupService.getByName("Group One"));
  }

  // List Groups
  @Test
  public void testListGroupsNoFilters() {
    entityGenerator.setupSimpleGroups();
    val groups = groupService.listGroups(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(groups.getTotalElements()).isEqualTo(3L);
  }

  @Test
  public void testListGroupsNoFiltersEmptyResult() {
    val groups = groupService.listGroups(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(groups.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testListGroupsFiltered() {
    entityGenerator.setupSimpleGroups();
    val groupNameFilter = new SearchFilter("name", "Group One");
    val groups = groupService.listGroups(Arrays.asList(groupNameFilter), new PageableResolver().getPageable());
    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName()).isEqualTo("Group One");
  }

  @Test
  public void testListGroupsFilteredEmptyResult() {
    entityGenerator.setupSimpleGroups();
    val groupNameFilter = new SearchFilter("name", "Group Four");
    val groups = groupService.listGroups(Arrays.asList(groupNameFilter), new PageableResolver().getPageable());
    assertThat(groups.getTotalElements()).isEqualTo(0L);
  }

  // Find Groups
  @Test
  public void testFindGroupsNoFilters() {
    entityGenerator.setupSimpleGroups();
    val groups = groupService.findGroups("One", Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName()).isEqualTo("Group One");
  }

  @Test
  public void testFindGroupsFiltered() {
    entityGenerator.setupSimpleGroups();
    val groupNameFilter = new SearchFilter("name", "Group One");
    val groups = groupService.findGroups("Two", Arrays.asList(groupNameFilter), new PageableResolver().getPageable());
    // Expect empty list
    assertThat(groups.getTotalElements()).isEqualTo(0L);
  }

  // Find User's Groups
  @Test
  public void testFindUsersGroupsNoQueryNoFilters() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleUsers();

    val userId = Integer.toString(userService.getByName("FirstUser@domain.com").getId());
    val userTwoId = Integer.toString(userService.getByName("SecondUser@domain.com").getId());
    val groupId = Integer.toString(groupService.getByName("Group One").getId());

    userService.addUsersToGroups(userId, Arrays.asList(groupId));
    userService.addUsersToGroups(userTwoId, Arrays.asList(groupId));

    val groups = groupService.findUsersGroup(userId, Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName()).isEqualTo("Group One");
  }

  @Test
  public void testFindUsersGroupsNoQueryNoFiltersNoGroup() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleUsers();

    val userId = Integer.toString(userService.getByName("FirstUser@domain.com").getId());
    val groups = groupService.findUsersGroup(userId, Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindUsersGroupsNoQueryNoFiltersEmptyGroupString() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleUsers();
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> groupService.findUsersGroup("", Collections.emptyList(), new PageableResolver().getPageable()));
  }

  @Test
  public void testFindUsersGroupsNoQueryFilters() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleUsers();

    val userId = Integer.toString(userService.getByName("FirstUser@domain.com").getId());
    val groupId = Integer.toString(groupService.getByName("Group One").getId());
    val groupTwoId = Integer.toString(groupService.getByName("Group Two").getId());

    userService.addUsersToGroups(userId, Arrays.asList(groupId, groupTwoId));

    val groupsFilters = new SearchFilter("name", "Group One");

    val groups = groupService.findUsersGroup(userId, Arrays.asList(groupsFilters), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName()).isEqualTo("Group One");
  }

  @Test
  public void testFindUsersGroupsQueryAndFilters() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleUsers();

    val userId = Integer.toString(userService.getByName("FirstUser@domain.com").getId());
    val groupId = Integer.toString(groupService.getByName("Group One").getId());
    val groupTwoId = Integer.toString(groupService.getByName("Group Two").getId());

    userService.addUsersToGroups(userId, Arrays.asList(groupId, groupTwoId));

    val groupsFilters = new SearchFilter("name", "Group One");

    val groups = groupService.findUsersGroup(userId, "Two", Arrays.asList(groupsFilters), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindUsersGroupsQueryNoFilters() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleUsers();

    val userId = Integer.toString(userService.getByName("FirstUser@domain.com").getId());
    val groupId = Integer.toString(groupService.getByName("Group One").getId());
    val groupTwoId = Integer.toString(groupService.getByName("Group Two").getId());

    userService.addUsersToGroups(userId, Arrays.asList(groupId, groupTwoId));

    val groups = groupService.findUsersGroup(userId, "Two", Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName()).isEqualTo("Group Two");
  }

  // Find Application's Groups
  @Test
  public void testFindApplicationsGroupsNoQueryNoFilters() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val groupId = Integer.toString(groupService.getByName("Group One").getId());
    val groupTwoId = Integer.toString(groupService.getByName("Group Two").getId());
    val applicationId = Integer.toString(applicationService.getByClientId("111111").getId());
    val applicationTwoId = Integer.toString(applicationService.getByClientId("222222").getId());

    groupService.addAppsToGroups(groupId, Arrays.asList(applicationId));
    groupService.addAppsToGroups(groupTwoId, Arrays.asList(applicationTwoId));

    val groups = groupService.findApplicationsGroup(applicationId, Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName()).isEqualTo("Group One");
  }

  @Test
  public void testFindApplicationsGroupsNoQueryNoFiltersNoGroup() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val applicationId = Integer.toString(applicationService.getByClientId("111111").getId());

    val groups = groupService.findApplicationsGroup(applicationId, Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindApplicationsGroupsNoQueryNoFiltersEmptyGroupString() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> groupService.findApplicationsGroup("", Collections.emptyList(), new PageableResolver().getPageable()));
  }

  @Test
  public void testFindApplicationsGroupsNoQueryFilters() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val groupId = Integer.toString(groupService.getByName("Group One").getId());
    val groupTwoId = Integer.toString(groupService.getByName("Group Two").getId());
    val applicationId = Integer.toString(applicationService.getByClientId("111111").getId());

    groupService.addAppsToGroups(groupId, Arrays.asList(applicationId));
    groupService.addAppsToGroups(groupTwoId, Arrays.asList(applicationId));

    val groupsFilters = new SearchFilter("name", "Group One");

    val groups = groupService.findApplicationsGroup(applicationId, Arrays.asList(groupsFilters), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName()).isEqualTo("Group One");
  }

  @Test
  public void testFindApplicationsGroupsQueryAndFilters() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val groupId = Integer.toString(groupService.getByName("Group One").getId());
    val groupTwoId = Integer.toString(groupService.getByName("Group Two").getId());
    val applicationId = Integer.toString(applicationService.getByClientId("111111").getId());

    groupService.addAppsToGroups(groupId, Arrays.asList(applicationId));
    groupService.addAppsToGroups(groupTwoId, Arrays.asList(applicationId));

    val groupsFilters = new SearchFilter("name", "Group One");

    val groups = groupService.findApplicationsGroup(applicationId, "Two", Arrays.asList(groupsFilters), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindApplicationsGroupsQueryNoFilters() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val groupId = Integer.toString(groupService.getByName("Group One").getId());
    val groupTwoId = Integer.toString(groupService.getByName("Group Two").getId());
    val applicationId = Integer.toString(applicationService.getByClientId("111111").getId());

    groupService.addAppsToGroups(groupId, Arrays.asList(applicationId));
    groupService.addAppsToGroups(groupTwoId, Arrays.asList(applicationId));

    val groups = groupService.findApplicationsGroup(applicationId, "Group One", Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName()).isEqualTo("Group One");
  }

  // Update
  @Test
  public void testUpdate() {
    val group = groupService.create(entityGenerator.createOneGroup("Group One"));
    group.setDescription("New Description");
    val updated = groupService.update(group);
    assertThat(updated.getDescription()).isEqualTo("New Description");
  }

  @Test
  public void testUpdateNonexistentEntity() {
    groupService.create(entityGenerator.createOneGroup("Group One"));
    val nonExistentEntity = entityGenerator.createOneGroup("Group Two");
    assertThatExceptionOfType(EntityNotFoundException.class)
        .isThrownBy(() -> groupService.update(nonExistentEntity));
  }

  @Test
  public void testUpdateIdNotAllowed() {
    val group = groupService.create(entityGenerator.createOneGroup("Group One"));
    group.setId(777);
    // New id means new non-existent entity or one that exists and is being overwritten
    assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> groupService.update(group));
  }

  @Test
  public void testUpdateNameNotAllowed() {
    entityGenerator.setupSimpleGroups();
    val group = groupService.getByName("Group One");
    group.setName("New Name");
    val updated = groupService.update(group);
    assertThat(1).isEqualTo(2);
    // TODO Check for uniqueness in application, currently only SQL
  }

  @Test
  public void testUpdateStatusNotInAllowedEnum() {
    entityGenerator.setupSimpleGroups();
    val group = groupService.getByName("Group One");
    group.setStatus("Junk");
    val updated = groupService.update(group);
    assertThat(1).isEqualTo(2);
    // TODO Check for uniqueness in application, currently only SQL
  }

  // Add Apps to Group
  @Test
  public void addAppsToGroup() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val groupId = Integer.toString(groupService.getByName("Group One").getId());
    val application = applicationService.getByClientId("111111");
    val applicationId = Integer.toString(application.getId());

    groupService.addAppsToGroups(groupId, Arrays.asList(applicationId));

    val group = groupService.get(groupId);

    assertThat(group.getWholeApplications()).contains(applicationService.getByClientId("111111"));
  }

  @Test
  public void addAppsToGroupNoGroup() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();
    val applicationId = Integer.toString(applicationService.getByClientId("111111").getId());
    assertThatExceptionOfType(EntityNotFoundException.class)
        .isThrownBy(() -> groupService.addAppsToGroups("777", Arrays.asList(applicationId)));
  }

  @Test
  public void addAppsToGroupEmptyGroupString() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();
    val applicationId = Integer.toString(applicationService.getByClientId("111111").getId());
    assertThatExceptionOfType(NumberFormatException.class)
        .isThrownBy(() -> groupService.addAppsToGroups("", Arrays.asList(applicationId)));
  }

  @Test
  public void addAppsToGroupNoApp() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val groupId = Integer.toString(groupService.getByName("Group One").getId());
    assertThatExceptionOfType(EntityNotFoundException.class)
        .isThrownBy(() -> groupService.addAppsToGroups(groupId, Arrays.asList("777")));
  }

  @Test
  public void addAppsToGroupEmptyAppList() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val groupId = Integer.toString(groupService.getByName("Group One").getId());
    assertThatExceptionOfType(NumberFormatException.class)
        .isThrownBy(() -> groupService.addAppsToGroups(groupId, Arrays.asList("")));
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
