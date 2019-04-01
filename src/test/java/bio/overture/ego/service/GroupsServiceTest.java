package bio.overture.ego.service;

import static bio.overture.ego.model.enums.AccessLevel.DENY;
import static bio.overture.ego.model.enums.AccessLevel.READ;
import static bio.overture.ego.model.enums.AccessLevel.WRITE;
import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static bio.overture.ego.model.enums.StatusType.PENDING;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentId;
import static bio.overture.ego.utils.EntityTools.extractGroupNames;
import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import bio.overture.ego.controller.resolver.PageableResolver;
import bio.overture.ego.model.dto.GroupRequest;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.entity.AbstractPermission;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.model.exceptions.UniqueViolationException;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.join.UserGroupRepository;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.PolicyPermissionUtils;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;
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
public class GroupsServiceTest {
  @Autowired private ApplicationService applicationService;

  @Autowired private UserService userService;

  @Autowired private GroupService groupService;
  @Autowired private GroupPermissionService groupPermissionService;

  @Autowired private PolicyService policyService;

  @Autowired private EntityGenerator entityGenerator;
  @Autowired private UserGroupRepository userGroupRepository;

  // Create
  @Test
  public void testCreate() {
    val group = entityGenerator.setupGroup("Group One");
    assertThat(group.getName()).isEqualTo("Group One");
  }

  @Test
  public void uniqueNameCheck_CreateGroup_ThrowsUniqueConstraintException() {
    val r1 = GroupRequest.builder().name(UUID.randomUUID().toString()).status(PENDING).build();

    val g1 = groupService.create(r1);
    assertThat(groupService.isExist(g1.getId())).isTrue();

    assertThat(g1.getName()).isEqualTo(r1.getName());
    assertThatExceptionOfType(UniqueViolationException.class)
        .isThrownBy(() -> groupService.create(r1));
  }

  @Test
  public void uniqueClientIdCheck_UpdateGroup_ThrowsUniqueConstraintException() {
    val name1 = UUID.randomUUID().toString();
    val name2 = UUID.randomUUID().toString();
    val cr1 = GroupRequest.builder().name(name1).status(PENDING).build();

    val cr2 = GroupRequest.builder().name(name2).status(APPROVED).build();

    val g1 = groupService.create(cr1);
    assertThat(groupService.isExist(g1.getId())).isTrue();
    val g2 = groupService.create(cr2);
    assertThat(groupService.isExist(g2.getId())).isTrue();

    val ur3 = GroupRequest.builder().name(name1).build();

    assertThat(g1.getName()).isEqualTo(ur3.getName());
    assertThat(g2.getName()).isNotEqualTo(ur3.getName());
    assertThatExceptionOfType(UniqueViolationException.class)
        .isThrownBy(() -> groupService.partialUpdate(g2.getId(), ur3));
  }

  @Test
  @Ignore
  public void testCreateUniqueName() {
    //    groupService.create(entityGenerator.createGroup("Group One"));
    //    groupService.create(entityGenerator.createGroup("Group Two"));
    //    assertThatExceptionOfType(DataIntegrityViolationException.class)
    //        .isThrownBy(() -> groupService.create(entityGenerator.createGroup("Group One")));
    assertThat(1).isEqualTo(2);
    // TODO Check for uniqueness in application, currently only SQL
  }

  // Get
  @Test
  public void testGet() {
    val group = entityGenerator.setupGroup("Group One");
    val saveGroup = groupService.getById(group.getId());
    assertThat(saveGroup.getName()).isEqualTo("Group One");
  }

  @Test
  public void testGetNotFoundException() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> groupService.getById(UUID.randomUUID()));
  }

  @Test
  public void testGetByName() {
    entityGenerator.setupGroup("Group One");
    val saveGroup = groupService.getByName("Group One");
    assertThat(saveGroup.getName()).isEqualTo("Group One");
  }

  @Test
  public void testGetByNameAllCaps() {
    entityGenerator.setupGroup("Group One");
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
    entityGenerator.setupTestGroups();
    val groups =
        groupService.listGroups(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(groups.getTotalElements()).isEqualTo(3L);
  }

  @Test
  public void testListGroupsNoFiltersEmptyResult() {
    val groups =
        groupService.listGroups(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(groups.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testListGroupsFiltered() {
    entityGenerator.setupTestGroups();
    val groupNameFilter = new SearchFilter("name", "Group One");
    val groups =
        groupService.listGroups(
            Arrays.asList(groupNameFilter), new PageableResolver().getPageable());
    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName()).isEqualTo("Group One");
  }

  @Test
  public void testListGroupsFilteredEmptyResult() {
    entityGenerator.setupTestGroups();
    val groupNameFilter = new SearchFilter("name", "Group Four");
    val groups =
        groupService.listGroups(
            Arrays.asList(groupNameFilter), new PageableResolver().getPageable());
    assertThat(groups.getTotalElements()).isEqualTo(0L);
  }

  // Find Groups
  @Test
  public void testFindGroupsNoFilters() {
    entityGenerator.setupTestGroups();
    val groups =
        groupService.findGroups(
            "One", Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName()).isEqualTo("Group One");
  }

  @Test
  public void testFindGroupsFiltered() {
    entityGenerator.setupTestGroups();
    val groupNameFilter = new SearchFilter("name", "Group One");
    val groups =
        groupService.findGroups(
            "Two", Arrays.asList(groupNameFilter), new PageableResolver().getPageable());
    // Expect empty list
    assertThat(groups.getTotalElements()).isEqualTo(0L);
  }

  // Find User's Groups
  @Test
  public void testFindUsersGroupsNoQueryNoFilters() {
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestUsers();

    val userId = userService.getByName("FirstUser@domain.com").getId();
    val userTwoId = userService.getByName("SecondUser@domain.com").getId();
    val groupId = groupService.getByName("Group One").getId();

    userService.associateGroupsWithUser(userId, Arrays.asList(groupId));
    userService.associateGroupsWithUser(userTwoId, Arrays.asList(groupId));

    val groups =
        userService.findGroupsForUser(
            userId, ImmutableList.of(), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName()).isEqualTo("Group One");
  }

  @Test
  public void testFindUsersGroupsNoQueryNoFiltersNoGroupsFound() {
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestUsers();

    val userId = userService.getByName("FirstUser@domain.com").getId();

    val groups =
        userService.findGroupsForUser(
            userId, ImmutableList.of(), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindUsersGroupsNoQueryFilters() {
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestUsers();

    val userId = userService.getByName("FirstUser@domain.com").getId();
    val groupId = groupService.getByName("Group One").getId();
    val groupTwoId = groupService.getByName("Group Two").getId();

    userService.associateGroupsWithUser(userId, Arrays.asList(groupId, groupTwoId));

    val groupsFilters = new SearchFilter("name", "Group One");

    val groups =
        userService.findGroupsForUser(
            userId, ImmutableList.of(), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName()).isEqualTo("Group One");
  }

  @Test
  public void testFindUsersGroupsQueryAndFilters() {
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestUsers();

    val userId = userService.getByName("FirstUser@domain.com").getId();
    val groupId = groupService.getByName("Group One").getId();
    val groupTwoId = groupService.getByName("Group Two").getId();

    userService.associateGroupsWithUser(userId, Arrays.asList(groupId, groupTwoId));

    val groupsFilters = new SearchFilter("name", "Group One");

    val groups =
        userService.findGroupsForUser(
            userId, "Two", ImmutableList.of(groupsFilters), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindUsersGroupsQueryNoFilters() {
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestUsers();

    val userId = userService.getByName("FirstUser@domain.com").getId();
    val groupId = groupService.getByName("Group One").getId();
    val groupTwoId = groupService.getByName("Group Two").getId();

    userService.associateGroupsWithUser(userId, Arrays.asList(groupId, groupTwoId));

    val groups =
        userService.findGroupsForUser(
            userId, "Two", ImmutableList.of(), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName()).isEqualTo("Group Two");
  }

  // Find Application's Groups
  @Test
  public void testFindApplicationsGroupsNoQueryNoFilters() {
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestApplications();

    val groupId = groupService.getByName("Group One").getId();
    val groupTwoId = groupService.getByName("Group Two").getId();
    val applicationId = applicationService.getByClientId("111111").getId();
    val applicationTwoId = applicationService.getByClientId("222222").getId();

    groupService.addAppsToGroup(groupId, Arrays.asList(applicationId));
    groupService.addAppsToGroup(groupTwoId, Arrays.asList(applicationTwoId));

    val groups =
        groupService.findApplicationGroups(
            applicationId, ImmutableList.of(), new PageableResolver().getPageable());

    assertThat(extractGroupNames(groups.getContent())).contains("Group One");
    assertThat(extractGroupNames(groups.getContent())).doesNotContain("Group Two");
  }

  @Test
  public void testFindApplicationsGroupsNoQueryNoFiltersNoGroup() {
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestApplications();

    val applicationId = applicationService.getByClientId("111111").getId();

    val groups =
        groupService.findApplicationGroups(
            applicationId, ImmutableList.of(), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindApplicationsGroupsNoQueryFilters() {
    entityGenerator.setupTestGroups("testFindApplicationsGroupsNoQueryFilters");
    entityGenerator.setupTestApplications("testFindApplicationsGroupsNoQueryFilters");

    val groupId =
        groupService.getByName("Group One_testFindApplicationsGroupsNoQueryFilters").getId();
    val groupTwoId =
        groupService.getByName("Group Two_testFindApplicationsGroupsNoQueryFilters").getId();
    val applicationId =
        applicationService.getByClientId("111111_testFindApplicationsGroupsNoQueryFilters").getId();

    groupService.addAppsToGroup(groupId, Arrays.asList(applicationId));
    groupService.addAppsToGroup(groupTwoId, Arrays.asList(applicationId));

    val groupsFilters =
        new SearchFilter("name", "Group One_testFindApplicationsGroupsNoQueryFilters");

    val groups =
        groupService.findApplicationGroups(
            applicationId, ImmutableList.of(groupsFilters), new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName())
        .isEqualTo("Group One_testFindApplicationsGroupsNoQueryFilters");
  }

  @Test
  public void testFindApplicationsGroupsQueryAndFilters() {
    entityGenerator.setupTestGroups("testFindApplicationsGroupsQueryAndFilters");
    entityGenerator.setupTestApplications("testFindApplicationsGroupsQueryAndFilters");

    val groupId =
        groupService.getByName("Group One_testFindApplicationsGroupsQueryAndFilters").getId();
    val groupTwoId =
        groupService.getByName("Group Two_testFindApplicationsGroupsQueryAndFilters").getId();
    val applicationId =
        applicationService
            .getByClientId("111111_testFindApplicationsGroupsQueryAndFilters")
            .getId();

    groupService.addAppsToGroup(groupId, Arrays.asList(applicationId));
    groupService.addAppsToGroup(groupTwoId, Arrays.asList(applicationId));

    val groupsFilters =
        new SearchFilter("name", "Group One_testFindApplicationsGroupsQueryAndFilters");

    val groups =
        groupService.findApplicationGroups(
            applicationId,
            "Two",
            ImmutableList.of(groupsFilters),
            new PageableResolver().getPageable());

    assertThat(groups.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindApplicationsGroupsQueryNoFilters() {
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestApplications();

    val groupId = groupService.getByName("Group One").getId();
    val groupTwoId = groupService.getByName("Group Two").getId();
    val applicationId = applicationService.getByClientId("111111").getId();

    groupService.addAppsToGroup(groupId, Arrays.asList(applicationId));
    groupService.addAppsToGroup(groupTwoId, Arrays.asList(applicationId));

    val groups =
        groupService.findApplicationGroups(
            applicationId, "Group One", ImmutableList.of(), new PageableResolver().getPageable());
    assertThat(groups.getTotalElements()).isEqualTo(1L);
    assertThat(groups.getContent().get(0).getName()).isEqualTo("Group One");
  }

  // Update
  @Test
  public void testUpdate() {
    val group = entityGenerator.setupGroup("Group One");
    val updateRequest = GroupRequest.builder().description("New Description").build();
    val updated = groupService.partialUpdate(group.getId(), updateRequest);
    assertThat(updated.getDescription()).isEqualTo("New Description");
  }

  @Test
  public void testUpdateNonexistentEntity() {
    val nonExistentId = generateNonExistentId(groupService);
    val nonExistentEntity =
        GroupRequest.builder().name("NonExistent").status(PENDING).description("").build();
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> groupService.partialUpdate(nonExistentId, nonExistentEntity));
  }

  @Test
  @Ignore
  public void testUpdateNameNotAllowed() {
    //    entityGenerator.setupTestGroups();
    //    val group = groupService.getByName("Group One");
    //    group.setName("New Name");
    //    val updated = groupService.update(group);
    assertThat(1).isEqualTo(2);
    // TODO Check for uniqueness in application, currently only SQL
  }

  @Test
  @Ignore
  public void testUpdateStatusNotInAllowedEnum() {
    //    entityGenerator.setupTestGroups();
    //    val group = groupService.getByName("Group One");
    //    group.setStatus("Junk");
    //    val updated = groupService.update(group);
    assertThat(1).isEqualTo(2);
    // TODO Check for uniqueness in application, currently only SQL
  }

  // Add Apps to Group
  @Test
  public void addAppsToGroup() {
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestApplications();

    val groupId = groupService.getByName("Group One").getId();
    val application = applicationService.getByClientId("111111");
    val applicationId = application.getId();

    groupService.addAppsToGroup(groupId, Arrays.asList(applicationId));

    val group = groupService.getById(groupId);

    assertThat(group.getApplications()).contains(applicationService.getByClientId("111111"));
  }

  @Test
  public void addAppsToGroupNoGroup() {
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestApplications();
    val applicationId = applicationService.getByClientId("111111").getId();
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () -> groupService.addAppsToGroup(UUID.randomUUID(), Arrays.asList(applicationId)));
  }

  @Test
  public void addAppsToGroupNoApp() {
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestApplications();

    val groupId = groupService.getByName("Group One").getId();
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> groupService.addAppsToGroup(groupId, Arrays.asList(UUID.randomUUID())));
  }

  @Test
  public void addAppsToGroupEmptyAppList() {
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestApplications();

    val group = groupService.getByName("Group One");
    val groupId = group.getId();

    groupService.addAppsToGroup(groupId, Collections.emptyList());

    val nonUpdated = groupService.getByName("Group One");
    assertThat(nonUpdated).isEqualTo(group);
  }

  // Delete
  @Test
  public void testDelete() {
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group One");

    groupService.delete(group.getId());

    val groups =
        groupService.listGroups(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(groups.getTotalElements()).isEqualTo(2L);
    assertThat(groups.getContent()).doesNotContain(group);
  }

  @Test
  public void testDeleteNonExisting() {
    entityGenerator.setupTestGroups();
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> groupService.delete(UUID.randomUUID()));
  }

  // Delete Apps from Group
  @Test
  public void testDeleteAppFromGroup() {
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestApplications();

    val groupId = groupService.getByName("Group One").getId();
    val application = applicationService.getByClientId("111111");
    val applicationId = application.getId();

    groupService.addAppsToGroup(groupId, Arrays.asList(applicationId));

    val group = groupService.getById(groupId);
    assertThat(group.getApplications().size()).isEqualTo(1);

    groupService.deleteAppsFromGroup(groupId, Arrays.asList(applicationId));

    val groupWithDeleteApp = groupService.getById(groupId);
    assertThat(groupWithDeleteApp.getApplications().size()).isEqualTo(0);
  }

  @Test
  public void testDeleteAppsFromGroupNoGroup() {
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestApplications();

    val groupId = groupService.getByName("Group One").getId();
    val application = applicationService.getByClientId("111111");
    val applicationId = application.getId();

    groupService.addAppsToGroup(groupId, Arrays.asList(applicationId));

    val group = groupService.getById(groupId);
    assertThat(group.getApplications().size()).isEqualTo(1);

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(
            () ->
                groupService.deleteAppsFromGroup(UUID.randomUUID(), Arrays.asList(applicationId)));
  }

  @Test
  public void testDeleteAppsFromGroupEmptyAppsList() {
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestApplications();

    val groupId = groupService.getByName("Group One").getId();
    val application = applicationService.getByClientId("111111");
    val applicationId = application.getId();

    groupService.addAppsToGroup(groupId, Arrays.asList(applicationId));

    val group = groupService.getById(groupId);
    assertThat(group.getApplications().size()).isEqualTo(1);

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> groupService.deleteAppsFromGroup(groupId, Arrays.asList()));
  }

  /** This test guards against bad cascades against users */
  @Test
  public void testDeleteGroupWithUserRelations() {
    val user = entityGenerator.setupUser("foo bar");
    val group = entityGenerator.setupGroup("testGroup");

    val updatedGroup =
        userService.associateGroupsWithUser(group.getId(), newArrayList(user.getId()));

    groupService.delete(updatedGroup.getId());
    assertThat(userService.getById(user.getId())).isNotNull();
  }

  /** This test guards against bad cascades against applications */
  @Test
  public void testDeleteGroupWithApplicationRelations() {
    val app = entityGenerator.setupApplication("foobar");
    val group = entityGenerator.setupGroup("testGroup");

    val updatedGroup = groupService.addAppsToGroup(group.getId(), newArrayList(app.getId()));

    groupService.delete(updatedGroup.getId());
    assertThat(applicationService.getById(app.getId())).isNotNull();
  }

  @Test
  public void testAddGroupPermissions() {
    entityGenerator.setupTestGroups();
    val groups =
        groupService
            .listGroups(Collections.emptyList(), new PageableResolver().getPageable())
            .getContent();
    entityGenerator.setupTestPolicies();

    val study001 = policyService.getByName("Study001");
    val study001id = study001.getId();

    val study002 = policyService.getByName("Study002");
    val study002id = study002.getId();

    val study003 = policyService.getByName("Study003");
    val study003id = study003.getId();

    val permissions =
        Arrays.asList(
            new PermissionRequest(study001id, READ),
            new PermissionRequest(study002id, WRITE),
            new PermissionRequest(study003id, DENY));

    val firstGroup = groups.get(0);

    groupPermissionService.addPermissions(firstGroup.getId(), permissions);

    assertThat(PolicyPermissionUtils.extractPermissionStrings(firstGroup.getPermissions()))
        .containsExactlyInAnyOrder("Study001.READ", "Study002.WRITE", "Study003.DENY");
  }

  @Test
  public void testDeleteGroupPermissions() {
    entityGenerator.setupTestGroups();
    val groups =
        groupService
            .listGroups(Collections.emptyList(), new PageableResolver().getPageable())
            .getContent();
    entityGenerator.setupTestPolicies();

    val firstGroup = groups.get(0);

    val study001 = policyService.getByName("Study001");
    val study001id = study001.getId();

    val study002 = policyService.getByName("Study002");
    val study002id = study002.getId();

    val study003 = policyService.getByName("Study003");
    val study003id = study003.getId();

    val permissions =
        Arrays.asList(
            new PermissionRequest(study001id, READ),
            new PermissionRequest(study002id, WRITE),
            new PermissionRequest(study003id, DENY));

    groupPermissionService.addPermissions(firstGroup.getId(), permissions);

    val groupPermissionsToRemove =
        firstGroup.getPermissions().stream()
            .filter(p -> !p.getPolicy().getName().equals("Study001"))
            .map(AbstractPermission::getId)
            .collect(Collectors.toList());

    groupPermissionService.deletePermissions(firstGroup.getId(), groupPermissionsToRemove);

    assertThat(PolicyPermissionUtils.extractPermissionStrings(firstGroup.getPermissions()))
        .containsExactlyInAnyOrder("Study001.READ");
  }

  @Test
  public void testGetGroupPermissions() {
    entityGenerator.setupPolicies(
        "testGetGroupPermissions_Study001, testGetGroupPermissions_Group",
        "testGetGroupPermissions_Study002, testGetGroupPermissions_Group",
        "testGetGroupPermissions_Study003, testGetGroupPermissions_Group");

    val testGroup = entityGenerator.setupGroup("testGetGroupPermissions_Group");

    val study001 = policyService.getByName("testGetGroupPermissions_Study001");
    val study001id = study001.getId();

    val study002 = policyService.getByName("testGetGroupPermissions_Study002");
    val study002id = study002.getId();

    val study003 = policyService.getByName("testGetGroupPermissions_Study003");
    val study003id = study003.getId();

    val permissions =
        Arrays.asList(
            new PermissionRequest(study001id, READ),
            new PermissionRequest(study002id, WRITE),
            new PermissionRequest(study003id, DENY));

    groupPermissionService.addPermissions(testGroup.getId(), permissions);

    val pagedGroupPermissions =
        groupPermissionService.getPermissions(
            testGroup.getId(), new PageableResolver().getPageable());

    assertThat(pagedGroupPermissions.getTotalElements()).isEqualTo(1L);
    assertThat(pagedGroupPermissions.getContent().get(0).getAccessLevel().toString())
        .isEqualToIgnoringCase("READ");
    assertThat(pagedGroupPermissions.getContent().get(0).getPolicy().getName())
        .isEqualToIgnoringCase("testGetGroupPermissions_Study001");
  }
}
