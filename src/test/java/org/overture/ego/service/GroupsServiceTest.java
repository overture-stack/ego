package org.overture.ego.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.overture.ego.controller.resolver.PageableResolver;
import org.overture.ego.model.params.Scope;
import org.overture.ego.model.search.SearchFilter;
import org.overture.ego.utils.EntityGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.overture.ego.utils.AclPermissionUtils.extractPermissionStrings;

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
  private PolicyService policyService;

  @Autowired
  private EntityGenerator entityGenerator;

  // Create
  @Test
  public void testCreate() {
    val group = groupService.create(entityGenerator.createOneGroup("Group One"));
    assertThat(group.getName()).isEqualTo("Group One");
  }

  @Test
  @Ignore
  public void testCreateUniqueName() {
//    groupService.create(entityGenerator.createOneGroup("Group One"));
//    groupService.create(entityGenerator.createOneGroup("Group Two"));
//    assertThatExceptionOfType(DataIntegrityViolationException.class)
//        .isThrownBy(() -> groupService.create(entityGenerator.createOneGroup("Group One")));
    assertThat(1).isEqualTo(2);
    // TODO Check for uniqueness in application, currently only SQL
  }

  // Get
  @Test
  public void testGet() {
    val group = groupService.create(entityGenerator.createOneGroup("Group One"));
    val saveGroup = groupService.get(group.getId().toString());
    assertThat(saveGroup.getName()).isEqualTo("Group One");
  }

  @Test
  public void testGetEntityNotFoundException() {
    assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> groupService.get(UUID.randomUUID().toString()));
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
  @Ignore
  public void testGetByNameNotFound() {
    // TODO Currently returning null, should throw exception (EntityNotFoundException?)
    assertThatExceptionOfType(EntityNotFoundException.class)
        .isThrownBy(() -> groupService.getByName("Group One"));
  }

  // List Groups
  @Test
  public void testListGroupsNoFilters() {
    entityGenerator.setupSimpleGroups();
    val groups = groupService
        .listGroups(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(groups.getTotalElements()).isEqualTo(3L);
  }

  @Test
  public void testListGroupsNoFiltersEmptyResult() {
    val groups = groupService
        .listGroups(Collections.emptyList(), new PageableResolver().getPageable());
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
    val groups = groupService
        .findGroups("Two", Arrays.asList(groupNameFilter), new PageableResolver().getPageable());
    // Expect empty list
    assertThat(groups.getTotalElements()).isEqualTo(0L);
  }

  // Find User's Groups
  @Test
  public void testFindUsersGroupsNoQueryNoFilters() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleUsers();

    val userId = userService.getByName("FirstUser@domain.com").getId().toString();
    val userTwoId = userService.getByName("SecondUser@domain.com").getId().toString();
    val groupId = groupService.getByName("Group One").getId().toString();

    userService.addUserToGroups(userId, Arrays.asList(groupId));
    userService.addUserToGroups(userTwoId, Arrays.asList(groupId));

    val groups = groupService.findUserGroups(
        userId,
        Collections.emptyList(),
        new PageableResolver().getPageable()
    );

    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName()).isEqualTo("Group One");
  }

  @Test
  public void testFindUsersGroupsNoQueryNoFiltersNoGroupsFound() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleUsers();

    val userId = userService.getByName("FirstUser@domain.com").getId().toString();

    val groups = groupService.findUserGroups(
        userId,
        Collections.emptyList(),
        new PageableResolver().getPageable()
    );

    assertThat(groups.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindUsersGroupsNoQueryNoFiltersEmptyGroupString() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleUsers();
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> groupService.findUserGroups("", Collections.emptyList(), new PageableResolver().getPageable()));
  }

  @Test
  public void testFindUsersGroupsNoQueryFilters() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleUsers();

    val userId = userService.getByName("FirstUser@domain.com").getId().toString();
    val groupId = groupService.getByName("Group One").getId().toString();
    val groupTwoId = groupService.getByName("Group Two").getId().toString();

    userService.addUserToGroups(userId, Arrays.asList(groupId, groupTwoId));

    val groupsFilters = new SearchFilter("name", "Group One");

    val groups = groupService.findUserGroups(
        userId,
        Arrays.asList(groupsFilters),
        new PageableResolver().getPageable()
    );

    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName()).isEqualTo("Group One");
  }

  @Test
  public void testFindUsersGroupsQueryAndFilters() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleUsers();

    val userId = userService.getByName("FirstUser@domain.com").getId().toString();
    val groupId = groupService.getByName("Group One").getId().toString();
    val groupTwoId = groupService.getByName("Group Two").getId().toString();

    userService.addUserToGroups(userId, Arrays.asList(groupId, groupTwoId));

    val groupsFilters = new SearchFilter("name", "Group One");

    val groups = groupService.findUserGroups(
        userId,
        "Two",
        Arrays.asList(groupsFilters),
        new PageableResolver().getPageable()
    );

    assertThat(groups.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindUsersGroupsQueryNoFilters() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleUsers();

    val userId = userService.getByName("FirstUser@domain.com").getId().toString();
    val groupId = groupService.getByName("Group One").getId().toString();
    val groupTwoId = groupService.getByName("Group Two").getId().toString();

    userService.addUserToGroups(userId, Arrays.asList(groupId, groupTwoId));

    val groups = groupService.findUserGroups(
        userId,
        "Two",
        Collections.emptyList(),
        new PageableResolver().getPageable()
    );

    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName()).isEqualTo("Group Two");
  }

  // Find Application's Groups
  @Test
  public void testFindApplicationsGroupsNoQueryNoFilters() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val groupId = groupService.getByName("Group One").getId().toString();
    val groupTwoId = groupService.getByName("Group Two").getId().toString();
    val applicationId = applicationService.getByClientId("111111").getId().toString();
    val applicationTwoId = applicationService.getByClientId("222222").getId().toString();

    groupService.addAppsToGroup(groupId, Arrays.asList(applicationId));
    groupService.addAppsToGroup(groupTwoId, Arrays.asList(applicationTwoId));

    val groups = groupService.findApplicationGroups(
        applicationId,
        Collections.emptyList(),
        new PageableResolver().getPageable()
    );

    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName()).isEqualTo("Group One");
  }

  @Test
  public void testFindApplicationsGroupsNoQueryNoFiltersNoGroup() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val applicationId = applicationService.getByClientId("111111").getId().toString();

    val groups = groupService.findApplicationGroups(applicationId, Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindApplicationsGroupsNoQueryNoFiltersEmptyGroupString() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> groupService
            .findApplicationGroups(
                "",
                Collections.emptyList(),
                new PageableResolver().getPageable()
            )
        );
  }

  @Test
  public void testFindApplicationsGroupsNoQueryFilters() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val groupId = groupService.getByName("Group One").getId().toString();
    val groupTwoId = groupService.getByName("Group Two").getId().toString();
    val applicationId = applicationService.getByClientId("111111").getId().toString();

    groupService.addAppsToGroup(groupId, Arrays.asList(applicationId));
    groupService.addAppsToGroup(groupTwoId, Arrays.asList(applicationId));

    val groupsFilters = new SearchFilter("name", "Group One");

    val groups = groupService.findApplicationGroups(applicationId, Arrays.asList(groupsFilters), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName()).isEqualTo("Group One");
  }

  @Test
  public void testFindApplicationsGroupsQueryAndFilters() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val groupId = groupService.getByName("Group One").getId().toString();
    val groupTwoId = groupService.getByName("Group Two").getId().toString();
    val applicationId = applicationService.getByClientId("111111").getId().toString();

    groupService.addAppsToGroup(groupId, Arrays.asList(applicationId));
    groupService.addAppsToGroup(groupTwoId, Arrays.asList(applicationId));

    val groupsFilters = new SearchFilter("name", "Group One");

    val groups = groupService.findApplicationGroups(applicationId, "Two", Arrays.asList(groupsFilters), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindApplicationsGroupsQueryNoFilters() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val groupId = groupService.getByName("Group One").getId().toString();
    val groupTwoId = groupService.getByName("Group Two").getId().toString();
    val applicationId = applicationService.getByClientId("111111").getId().toString();

    groupService.addAppsToGroup(groupId, Arrays.asList(applicationId));
    groupService.addAppsToGroup(groupTwoId, Arrays.asList(applicationId));

    val groups = groupService.findApplicationGroups(applicationId, "Group One", Collections.emptyList(), new PageableResolver().getPageable());

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
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
        .isThrownBy(() -> groupService.update(nonExistentEntity));
  }

  @Test
  public void testUpdateIdNotAllowed() {
    val group = groupService.create(entityGenerator.createOneGroup("Group One"));
    group.setId(new UUID(12312912931L,12312912931L));
    // New id means new non-existent entity or one that exists and is being overwritten
    assertThatExceptionOfType(EntityNotFoundException.class)
        .isThrownBy(() -> groupService.update(group));
  }

  @Test
  @Ignore
  public void testUpdateNameNotAllowed() {
//    entityGenerator.setupSimpleGroups();
//    val group = groupService.getByName("Group One");
//    group.setName("New Name");
//    val updated = groupService.update(group);
    assertThat(1).isEqualTo(2);
    // TODO Check for uniqueness in application, currently only SQL
  }

  @Test
  @Ignore
  public void testUpdateStatusNotInAllowedEnum() {
//    entityGenerator.setupSimpleGroups();
//    val group = groupService.getByName("Group One");
//    group.setStatus("Junk");
//    val updated = groupService.update(group);
    assertThat(1).isEqualTo(2);
    // TODO Check for uniqueness in application, currently only SQL
  }

  // Add Apps to Group
  @Test
  public void addAppsToGroup() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val groupId = groupService.getByName("Group One").getId().toString();
    val application = applicationService.getByClientId("111111");
    val applicationId = application.getId().toString();

    groupService.addAppsToGroup(groupId, Arrays.asList(applicationId));

    val group = groupService.get(groupId);

    assertThat(group.getWholeApplications()).contains(applicationService.getByClientId("111111"));
  }

  @Test
  public void addAppsToGroupNoGroup() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();
    val applicationId = applicationService.getByClientId("111111").getId().toString();
    assertThatExceptionOfType(EntityNotFoundException.class)
        .isThrownBy(() -> groupService.addAppsToGroup(UUID.randomUUID().toString(), Arrays.asList(applicationId)));
  }

  @Test
  public void addAppsToGroupEmptyGroupString() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();
    val applicationId = applicationService.getByClientId("111111").getId().toString();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> groupService.addAppsToGroup("", Arrays.asList(applicationId)));
  }

  @Test
  public void addAppsToGroupNoApp() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val groupId = groupService.getByName("Group One").getId().toString();
    assertThatExceptionOfType(EntityNotFoundException.class)
        .isThrownBy(() -> groupService.addAppsToGroup(groupId, Arrays.asList(UUID.randomUUID().toString())));
  }

  @Test
  public void addAppsToGroupWithAppListOneEmptyString() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val groupId = groupService.getByName("Group One").getId().toString();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> groupService.addAppsToGroup(groupId, Arrays.asList("")));
  }

  @Test
  public void addAppsToGroupEmptyAppList() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val group = groupService.getByName("Group One");
    val groupId = group.getId().toString();

    groupService.addAppsToGroup(groupId, Collections.emptyList());

    val nonUpdated = groupService.getByName("Group One");
    assertThat(nonUpdated).isEqualTo(group);
  }

  // Delete
  @Test
  public void testDelete() {
    entityGenerator.setupSimpleGroups();

    val group = groupService.getByName("Group One");

    groupService.delete(group.getId().toString());

    val groups = groupService.listGroups(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(groups.getTotalElements()).isEqualTo(2L);
    assertThat(groups.getContent()).doesNotContain(group);
  }

  @Test
  public void testDeleteNonExisting() {
    entityGenerator.setupSimpleGroups();
    assertThatExceptionOfType(EmptyResultDataAccessException.class)
        .isThrownBy(() -> groupService.delete(UUID.randomUUID().toString()));
  }

  @Test
  public void testDeleteEmptyIdString() {
    entityGenerator.setupSimpleGroups();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> groupService.delete(""));
  }

  // Delete Apps from Group
  @Test
  public void testDeleteAppFromGroup() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val groupId = groupService.getByName("Group One").getId().toString();
    val application = applicationService.getByClientId("111111");
    val applicationId = application.getId().toString();

    groupService.addAppsToGroup(groupId, Arrays.asList(applicationId));

    val group = groupService.get(groupId);
    assertThat(group.getWholeApplications().size()).isEqualTo(1);

    groupService.deleteAppsFromGroup(groupId, Arrays.asList(applicationId));

    val groupWithDeleteApp = groupService.get(groupId);
    assertThat(groupWithDeleteApp.getWholeApplications().size()).isEqualTo(0);
  }

  @Test
  public void testDeleteAppsFromGroupNoGroup() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val groupId = groupService.getByName("Group One").getId().toString();
    val application = applicationService.getByClientId("111111");
    val applicationId = application.getId().toString();

    groupService.addAppsToGroup(groupId, Arrays.asList(applicationId));

    val group = groupService.get(groupId);
    assertThat(group.getWholeApplications().size()).isEqualTo(1);

    assertThatExceptionOfType(EntityNotFoundException.class)
        .isThrownBy(() -> groupService
            .deleteAppsFromGroup(UUID.randomUUID().toString(), Arrays.asList(applicationId)));
  }

  @Test
  public void testDeleteAppsFromGroupEmptyGroupString() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val groupId = groupService.getByName("Group One").getId().toString();
    val application = applicationService.getByClientId("111111");
    val applicationId = application.getId().toString();

    groupService.addAppsToGroup(groupId, Arrays.asList(applicationId));

    val group = groupService.get(groupId);
    assertThat(group.getWholeApplications().size()).isEqualTo(1);

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> groupService.deleteAppsFromGroup("", Arrays.asList(applicationId)));
  }

  @Test
  public void testDeleteAppsFromGroupEmptyAppsList() {
    entityGenerator.setupSimpleGroups();
    entityGenerator.setupSimpleApplications();

    val groupId = groupService.getByName("Group One").getId().toString();
    val application = applicationService.getByClientId("111111");
    val applicationId = application.getId().toString();

    groupService.addAppsToGroup(groupId, Arrays.asList(applicationId));

    val group = groupService.get(groupId);
    assertThat(group.getWholeApplications().size()).isEqualTo(1);

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> groupService.deleteAppsFromGroup(groupId, Arrays.asList("")));
  }

  @Test
  public void testAddGroupPermissions() {
    entityGenerator.setupSimpleGroups();
    val groups = groupService
        .listGroups(Collections.emptyList(), new PageableResolver().getPageable())
        .getContent();
    entityGenerator.setupSimpleAclEntities(groups);

    val study001 = policyService.getByName("Study001");
    val study001id = study001.getId().toString();

    val study002 = policyService.getByName("Study002");
    val study002id = study002.getId().toString();

    val study003 = policyService.getByName("Study003");
    val study003id = study003.getId().toString();

    val permissions = Arrays.asList(
        new Scope(study001id, "READ"),
        new Scope(study002id, "WRITE"),
        new Scope(study003id, "DENY")
    );

    val firstGroup = groups.get(0);

    groupService.addGroupPermissions(firstGroup.getId().toString(), permissions);

    assertThat(extractPermissionStrings(firstGroup.getGroupPermissions()))
        .containsExactlyInAnyOrder(
            "Study001.READ",
            "Study002.WRITE",
            "Study003.DENY"
        );
  }

  @Test
  public void testDeleteGroupPermissions() {
    entityGenerator.setupSimpleGroups();
    val groups = groupService
        .listGroups(Collections.emptyList(), new PageableResolver().getPageable())
        .getContent();
    entityGenerator.setupSimpleAclEntities(groups);

    val firstGroup = groups.get(0);

    val study001 = policyService.getByName("Study001");
    val study001id = study001.getId().toString();

    val study002 = policyService.getByName("Study002");
    val study002id = study002.getId().toString();

    val study003 = policyService.getByName("Study003");
    val study003id = study003.getId().toString();

    val permissions = Arrays.asList(
        new Scope(study001id, "READ"),
        new Scope(study002id, "WRITE"),
        new Scope(study003id, "DENY")
    );

    groupService.addGroupPermissions(firstGroup.getId().toString(), permissions);

    val groupPermissionsToRemove = firstGroup.getGroupPermissions()
        .stream()
        .filter(p -> !p.getEntity().getName().equals("Study001"))
        .map(p -> p.getId().toString())
        .collect(Collectors.toList());

    groupService.deleteGroupPermissions(firstGroup.getId().toString(), groupPermissionsToRemove);

    assertThat(extractPermissionStrings(firstGroup.getGroupPermissions()))
        .containsExactlyInAnyOrder(
            "Study001.READ"
        );
  }

  @Test
  public void testGetGroupPermissions() {
    entityGenerator.setupSimpleGroups();
    val groups = groupService
        .listGroups(Collections.emptyList(), new PageableResolver().getPageable())
        .getContent();
    entityGenerator.setupSimpleAclEntities(groups);

    val firstGroup = groups.get(0);

    val study001 = policyService.getByName("Study001");
    val study001id = study001.getId().toString();

    val study002 = policyService.getByName("Study002");
    val study002id = study002.getId().toString();

    val study003 = policyService.getByName("Study003");
    val study003id = study003.getId().toString();

    val permissions = Arrays.asList(
        new Scope(study001id, "READ"),
        new Scope(study002id, "WRITE"),
        new Scope(study003id, "DENY")
    );

    groupService.addGroupPermissions(firstGroup.getId().toString(), permissions);

    val pagedGroupPermissions = groupService.getGroupPermissions(firstGroup.getId().toString(), new PageableResolver().getPageable());

    assertThat(pagedGroupPermissions.getTotalElements()).isEqualTo(3L);
  }
}
