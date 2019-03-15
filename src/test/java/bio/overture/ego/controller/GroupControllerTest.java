package bio.overture.ego.controller;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.GroupRequest;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.repository.GroupPermissionRepository;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.CollectionUtils;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.Joiners;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.NotImplementedException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static bio.overture.ego.model.enums.AccessLevel.DENY;
import static bio.overture.ego.model.enums.AccessLevel.READ;
import static bio.overture.ego.model.enums.AccessLevel.WRITE;
import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static bio.overture.ego.model.enums.StatusType.DISABLED;
import static bio.overture.ego.model.enums.StatusType.PENDING;
import static bio.overture.ego.model.enums.StatusType.REJECTED;
import static bio.overture.ego.utils.CollectionUtils.concatToSet;
import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static bio.overture.ego.utils.CollectionUtils.repeatedCallsOf;
import static bio.overture.ego.utils.Collectors.toImmutableList;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Converters.convertToIds;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentId;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentName;
import static bio.overture.ego.utils.EntityTools.extractAppIds;
import static bio.overture.ego.utils.EntityTools.extractGroupIds;
import static bio.overture.ego.utils.EntityTools.extractIDs;
import static bio.overture.ego.utils.Joiners.COMMA;
import static bio.overture.ego.utils.Streams.stream;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_ARRAY_ITEMS;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GroupControllerTest extends AbstractControllerTest {

  private boolean hasRunEntitySetup = false;

  /** Dependencies */
  @Autowired private EntityGenerator entityGenerator;

  @Autowired private GroupService groupService;
  @Autowired private UserService userService;
  @Autowired private ApplicationService applicationService;
  @Autowired private GroupPermissionRepository groupPermissionRepository;


  @Override
  protected void beforeTest() {
    // Initial setup of entities (run once
    if (!hasRunEntitySetup) {
      entityGenerator.setupTestUsers();
      entityGenerator.setupTestApplications();
      entityGenerator.setupTestGroups();
      hasRunEntitySetup = true;
    }
  }

  @Test
  public void addGroup() {
    val group =
        Group.builder()
            .name("Wizards")
            .status(PENDING)
            .description("")
            .build();

    val response = initStringRequest().endpoint("/groups").body(group).post();

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(OK);
  }

  @Test
  public void addUniqueGroup() {
    val group = entityGenerator.setupGroup("SameSame");

    val response = initStringRequest().endpoint("/groups").body(group).post();

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(CONFLICT);
  }

  @Test
  public void getGroup() {
    // Groups created in setup
    val groupId = groupService.getByName("Group One").getId();
    val response = initStringRequest().endpoint("/groups/%s", groupId).get();

    val responseStatus = response.getStatusCode();
    val responseBody = response.getBody();
    val expected =
        format(
            "{\"id\":\"%s\",\"name\":\"Group One\",\"description\":\"\",\"status\":\"PENDING\"}",
            groupId);

    assertThat(responseStatus).isEqualTo(OK);
    assertThatJson(responseBody).isEqualTo(expected);
  }

  @Test
  public void getGroupNotFound() {
    val response = initStringRequest().endpoint("/groups/%s", UUID.randomUUID()).get();

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  public void listGroups() {

    val totalGroups = groupService.getRepository().count();

    // Get all groups
    val response = initStringRequest().endpoint("/groups?offset=0&limit=%s", totalGroups).get();

    val responseStatus = response.getStatusCode();
    val responseBody = response.getBody();

    val expected =
        format(
            "[{\"id\":\"%s\",\"name\":\"Group One\",\"description\":\"\",\"status\":\"PENDING\"}, {\"id\":\"%s\",\"name\":\"Group Two\",\"description\":\"\",\"status\":\"PENDING\"}, {\"id\":\"%s\",\"name\":\"Group Three\",\"description\":\"\",\"status\":\"PENDING\"}]",
            groupService.getByName("Group One").getId(),
            groupService.getByName("Group Two").getId(),
            groupService.getByName("Group Three").getId());

    assertThat(responseStatus).isEqualTo(OK);
    assertThatJson(responseBody)
        .when(IGNORING_EXTRA_ARRAY_ITEMS, IGNORING_ARRAY_ORDER)
        .node("resultSet")
        .isEqualTo(expected);
  }

  // TODO - ADD List/Filter tests

  @Test
  public void updateGroup() {
    // Groups created in setup
    val group = entityGenerator.setupGroup("Complete");
    val update =
        Group.builder()
            .id(group.getId())
            .name("Updated Complete")
            .status(group.getStatus())
            .description(group.getDescription())
            .build();

    val response = initStringRequest().endpoint("/groups/%s", group.getId()).body(update).put();

    val responseBody = response.getBody();
    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(OK);
    assertThatJson(responseBody).node("id").isEqualTo(group.getId());
    assertThatJson(responseBody).node("name").isEqualTo("Updated Complete");
  }

  // TODO - ADD Update non-existent entity

  @Test
  @Ignore
  // TODO - Implement Patch method
  public void partialUpdateGroup() {
    // Groups created in setup
    val groupId = entityGenerator.setupGroup("Partial").getId();
    val update = "{\"name\":\"Updated Partial\"}";
    val response =
        initStringRequest()
            .endpoint("/groups/%s", groupId)
            .body(update)
            .post(); // TODO this should be a PATCH

    val responseBody = response.getBody();
    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(OK);
    assertThatJson(responseBody).node("id").isEqualTo(groupId);
    assertThatJson(responseBody).node("name").isEqualTo("Updated Partial");
  }

  @Test
  public void deleteOne() {
    val group = entityGenerator.setupGroup("DeleteOne");
    val groupId = group.getId();

    // Users for test
    val userOne = entityGenerator.setupUser("TempGroup User");

    // Application for test
    val appOne = entityGenerator.setupApplication("TempGroupApp");

    // REST to get users/app in group
    val usersBody = singletonList(userOne.getId().toString());
    val appsBody = singletonList(appOne.getId().toString());

    initStringRequest().endpoint("/groups/%s/users", group.getId()).body(usersBody).post();
    initStringRequest().endpoint("/groups/%s/applications", group.getId()).body(appsBody).post();

    // Check user-group relationship is there
    val userWithGroup = userService.getByName("TempGroupUser@domain.com");
    assertThat(extractGroupIds(userWithGroup.getGroups())).contains(groupId);

    // Check app-group relationship is there
    val applicationWithGroup = applicationService.getByClientId("TempGroupApp");
    assertThat(extractGroupIds(applicationWithGroup.getGroups())).contains(groupId);

    val response = initStringRequest().endpoint("/groups/%s", groupId).delete();

    val responseStatus = response.getStatusCode();

    // Check http response
    assertThat(responseStatus).isEqualTo(OK);

    // Check user-group relationship is also deleted
    val userWithoutGroup = userService.getByName("TempGroupUser@domain.com");
    assertThat(userWithoutGroup).isNotNull();
    assertThat(extractGroupIds(userWithoutGroup.getGroups())).doesNotContain(groupId);

    // Check app-group relationship is also deleted
    val applicationWithoutGroup = applicationService.getByClientId("TempGroupApp");
    assertThat(applicationWithoutGroup).isNotNull();
    assertThat(extractGroupIds(applicationWithoutGroup.getGroups())).doesNotContain(groupId);

    // Check group is deleted
    assertThat(groupService.findByName("DeleteOne")).isEmpty();
  }

  // TODO: [rtisma] will eventually be fixed when properly using query by Specification, which will
  // allow for runtime base queries. This will allow us to define fetch strategy at run time
  @Test
  public void addUsersToGroup() {

    val group = entityGenerator.setupGroup("GroupWithUsers");

    val userOne = userService.getByName("FirstUser@domain.com");
    val userTwo = userService.getByName("SecondUser@domain.com");

    val body = asList(userOne.getId().toString(), userTwo.getId().toString());
    val response =
        initStringRequest().endpoint("/groups/%s/users", group.getId()).body(body).post();

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(OK);

    // Check that Group is associated with Users
    val groupWithUsers = groupService.getByName("GroupWithUsers");
    assertThat(extractIDs(groupWithUsers.getUsers())).contains(userOne.getId(), userTwo.getId());

    // Check that each user is associated with the group
    val userOneWithGroups = userService.getByName("FirstUser@domain.com");
    val userTwoWithGroups = userService.getByName("SecondUser@domain.com");

    assertThat(userOneWithGroups.getGroups()).contains(group);
    assertThat(userTwoWithGroups.getGroups()).contains(group);
  }

  @Test
  @SneakyThrows
  public void deleteUserFromGroup() {
    val groupId = entityGenerator.setupGroup("RemoveGroupUsers").getId().toString();
    val deleteUser = entityGenerator.setupUser("Delete This").getId().toString();
    val remainUser = entityGenerator.setupUser("Keep This").getId().toString();

    val body = asList(deleteUser, remainUser);
    val response = initStringRequest().endpoint("/groups/%s/users", groupId).body(body).post();
    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(OK);

    val getResponse = initStringRequest().endpoint("/groups/%s/users", groupId).get();
    val getResponseStatus = getResponse.getStatusCode();
    assertThat(getResponseStatus).isEqualTo(OK);
    val getResponseJson = MAPPER.readTree(getResponse.getBody());
    assertThat(getResponseJson.get("count").asInt()).isEqualTo(2);

    val deleteResponse =
        initStringRequest().endpoint("/groups/%s/users/%s", groupId, deleteUser).delete();

    val deleteResponseStatus = deleteResponse.getStatusCode();
    assertThat(deleteResponseStatus).isEqualTo(OK);

    val secondGetResponse = initStringRequest().endpoint("/groups/%s/users", groupId).get();

    val secondGetResponseStatus = deleteResponse.getStatusCode();
    assertThat(secondGetResponseStatus).isEqualTo(OK);
    val secondGetResponseJson = MAPPER.readTree(secondGetResponse.getBody());
    assertThat(secondGetResponseJson.get("count").asInt()).isEqualTo(1);
    assertThat(secondGetResponseJson.get("resultSet").elements().next().get("id").asText())
        .isEqualTo(remainUser);
  }

  @Test
  public void addAppsToGroup() {

    val group = entityGenerator.setupGroup("GroupWithApps");

    val appOne = applicationService.getByClientId("111111");
    val appTwo = applicationService.getByClientId("222222");

    val body = asList(appOne.getId().toString(), appTwo.getId().toString());
    val response =
        initStringRequest().endpoint("/groups/%s/applications", group.getId()).body(body).post();

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(OK);

    // Check that Group is associated with Users
    val groupWithApps = groupService.getByName("GroupWithApps");
    assertThat(extractAppIds(groupWithApps.getApplications()))
        .contains(appOne.getId(), appTwo.getId());

    // Check that each user is associated with the group
    val appOneWithGroups = applicationService.getByClientId("111111");
    val appTwoWithGroups = applicationService.getByClientId("222222");

    assertThat(appOneWithGroups.getGroups()).contains(group);
    assertThat(appTwoWithGroups.getGroups()).contains(group);
  }

  @Test
  @SneakyThrows
  public void deleteAppFromGroup() {
    val groupId = entityGenerator.setupGroup("RemoveGroupApps").getId().toString();
    val deleteApp = entityGenerator.setupApplication("DeleteThis").getId().toString();
    val remainApp = entityGenerator.setupApplication("KeepThis").getId().toString();

    val body = asList(deleteApp, remainApp);
    val response =
        initStringRequest().endpoint("/groups/%s/applications", groupId).body(body).post();
    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(OK);

    val getResponse = initStringRequest().endpoint("/groups/%s/applications", groupId).get();
    val getResponseStatus = getResponse.getStatusCode();
    assertThat(getResponseStatus).isEqualTo(OK);
    val getResponseJson = MAPPER.readTree(getResponse.getBody());
    assertThat(getResponseJson.get("count").asInt()).isEqualTo(2);

    val deleteResponse =
        initStringRequest().endpoint("/groups/%s/applications/%s", groupId, deleteApp).delete();

    val deleteResponseStatus = deleteResponse.getStatusCode();
    assertThat(deleteResponseStatus).isEqualTo(OK);

    val secondGetResponse = initStringRequest().endpoint("/groups/%s/applications", groupId).get();

    val secondGetResponseStatus = deleteResponse.getStatusCode();
    assertThat(secondGetResponseStatus).isEqualTo(OK);
    val secondGetResponseJson = MAPPER.readTree(secondGetResponse.getBody());
    assertThat(secondGetResponseJson.get("count").asInt()).isEqualTo(1);
    assertThat(secondGetResponseJson.get("resultSet").elements().next().get("id").asText())
        .isEqualTo(remainApp);
  }

	@Test
	public void createGroup_NonExisting_Success(){
    val r = GroupRequest.builder()
        .name(generateNonExistentName(groupService))
        .status(APPROVED)
        .build();
    val r1 = initRequest(Group.class)
        .endpoint("groups")
        .body(r)
        .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);
    assertThat(r1.getBody()).isNotNull();

    val r2 = initRequest(Group.class)
        .endpoint("groups/%s", r1.getBody().getId())
        .get();
    assertThat(r2.getStatusCode()).isEqualTo(OK);
    assertThat(r2.getBody()).isNotNull();
    assertThat(r).isEqualToComparingFieldByField(r1.getBody());
	}

	@Test
	public void createGroup_NameAlreadyExists_Conflict(){
    val existingGroup = entityGenerator.generateRandomGroup();
    val createRequest = GroupRequest.builder()
        .name(existingGroup.getName())
        .status(APPROVED)
        .build();
    val r1 = initStringRequest()
        .endpoint("groups")
        .body(createRequest)
        .logging()
        .post();
    assertThat(r1.getStatusCode()).isEqualTo(CONFLICT);
	}

	@Test
	public void deleteGroup_NonExisting_Conflict(){
    val nonExistingId = generateNonExistentId(groupService);
    val r1 = initStringRequest()
        .endpoint("groups/%s", nonExistingId)
        .delete();
    assertThat(r1.getStatusCode()).isEqualTo(NOT_FOUND);
	}



	@Test
	public void deleteGroupAndRelationshipsOnly_AlreadyExisting_Success(){
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);

    // Add Applications to Group0
    val r1 = addGroupApplicationsPostRequest(group0, data.getApplications());
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Assert the applications were add to Group0
    val  r2 = getGroupApplicationsGetRequest(group0);
    assertThat(r2.getStatusCode()).isEqualTo(OK);
    val actualApplications = extractPageResultSetFromResponse(r2, Application.class);
    assertThat(actualApplications).isNotNull();
    assertThat(actualApplications).hasSize(data.getApplications().size());

    // Add Users to Group0
    val r3 = addGroupUserPostRequest(group0, data.getUsers());
    assertThat(r3.getStatusCode()).isEqualTo(OK);

    // Assert the users were added to Group0
    val  r4 = getGroupUsersGetRequest(group0);
    assertThat(r4.getStatusCode()).isEqualTo(OK);
    val actualUsers = extractPageResultSetFromResponse(r4, User.class);
    assertThat(actualUsers).isNotNull();
    assertThat(actualUsers).hasSize(data.getUsers().size());

    // Add Permissions to Group0
    val r5 = addGroupPermissionPostRequest(group0, data.getPolicies().get(0), DENY);
    assertThat(r5.getStatusCode()).isEqualTo(OK);
    val r6 = addGroupPermissionPostRequest(group0, data.getPolicies().get(1), WRITE);
    assertThat(r6.getStatusCode()).isEqualTo(OK);

    // Assert the permissions were added to Group0
    val r7 = getGroupPermissionsGetRequest(group0);
    assertThat(r7.getStatusCode()).isEqualTo(OK);
    val actualGroupPermissions = extractPageResultSetFromResponse(r7, GroupPermission.class);
    assertThat(actualGroupPermissions).hasSize(2);

    // Delete the group
    val r8 = deleteGroupDeleteRequest(group0);
    assertThat(r8.getStatusCode()).isEqualTo(OK);

    // Assert the group was deleted
    val r9 = getGroupEntityGetRequest(group0);
    assertThat(r9.getStatusCode()).isEqualTo(NOT_FOUND);

    // Assert no group permissions for the group
    val results = groupPermissionRepository.findAllByOwner_Id(group0.getId());
    assertThat(results).hasSize(0);

    // Assert getGroupUsers returns no results
    val r11 = getGroupUsersGetRequest(group0);
    assertThat(r11.getStatusCode()).isEqualTo(OK);
    val actualGroupUsers = extractPageResultSetFromResponse(r11, User.class);
    assertThat(actualGroupUsers).hasSize(0);

    // Assert getGroupApplications returns no results
    val r12 = getGroupApplicationsGetRequest(group0);
    assertThat(r12.getStatusCode()).isEqualTo(OK);
    val actualGroupApps = extractPageResultSetFromResponse(r12, Application.class);
    assertThat(actualGroupApps).hasSize(0);

    // Assert all users still exist
    data.getUsers().forEach(u -> {
      val r13 = getUserGetRequest(u);
      assertThat(r13.getStatusCode()).isEqualTo(OK);
    });

    // Assert all applications still exist
    data.getApplications().forEach(a -> {
      val r13 = getApplicationGetRequest(a);
      assertThat(r13.getStatusCode()).isEqualTo(OK);
    });

    // Assert all policies still exist
    data.getPolicies().forEach(p -> {
      val r13 = getPolicyGetRequest(p);
      assertThat(r13.getStatusCode()).isEqualTo(OK);
    });
  }

	@Test
	public void getGroups_FindAllQuery_Success(){
    val expectedGroups = repeatedCallsOf(() -> entityGenerator.generateRandomGroup(), 4);
    val actualGroups = initRequest(Group.class)
        .endpoint("/groups")
        .get();
    assertThat(actualGroups).isEqualToComparingFieldByField(expectedGroups);
	}

	@Test
	public void getGroups_FindSomeQuery_Success(){
    val g1 = extractOneEntityFromResponse(
        createGroupPostRequest(
            GroupRequest.builder()
                .name("abc11")
                .status(APPROVED)
                .description("blueberry banana")
                .build()),
         Group.class);

    val g2 = extractOneEntityFromResponse(
        createGroupPostRequest(
            GroupRequest.builder()
                .name("abc21")
                .status(APPROVED)
                .description("blueberry orange")
                .build()),
        Group.class);

    val g3 = extractOneEntityFromResponse(
        createGroupPostRequest(
            GroupRequest.builder()
                .name("abc22")
                .status(REJECTED)
                .description("orange banana")
                .build()),
        Group.class);

    val r1 = extractPageResultSetFromResponse(
        initStringRequest()
            .endpoint("/groups")
            .queryParam("query", "abc")
            .logging()
            .get(), Group.class);
    assertThat(r1).containsExactlyInAnyOrder(g1,g2, g3);

    val r2 = extractPageResultSetFromResponse(
        initStringRequest()
            .endpoint("/groups")
            .queryParam("query", "abc2")
            .get(), Group.class);
    assertThat(r2).containsExactlyInAnyOrder(g3,g2);

    val r3 = extractPageResultSetFromResponse(
        initStringRequest()
            .endpoint("/groups")
            .queryParam("query", "abc")
            .queryParam("status", REJECTED)
            .get(), Group.class);
    assertThat(r3).containsExactlyInAnyOrder(g3);

    val r4 = extractPageResultSetFromResponse(
        initStringRequest()
            .endpoint("/groups")
            .queryParam("query", "blueberry")
            .get(), Group.class);
    assertThat(r4).containsExactlyInAnyOrder(g1, g2);

    val r5 = extractPageResultSetFromResponse(
        initStringRequest()
            .endpoint("/groups")
            .queryParam("query", "orange")
            .get(), Group.class);
    assertThat(r5).containsExactlyInAnyOrder(g3, g2);

    val r6 = extractPageResultSetFromResponse(
        initStringRequest()
            .endpoint("/groups")
            .queryParam("query", "orange")
            .queryParam("status", REJECTED)
            .get(), Group.class);
    assertThat(r6).containsExactlyInAnyOrder(g3);

    val r7 = extractPageResultSetFromResponse(
        initStringRequest()
            .endpoint("/groups")
            .queryParam("query", g1.getId())
            .get(), Group.class);
    assertThat(r7).containsExactlyInAnyOrder(g1);
	}

	@Test
	public void addUsersToGroup_NonExistentGroup_NotFound(){
    val data = generateUniqueTestGroupData();
    val nonExistentId = generateNonExistentId(groupService);
    val nonExistentGroup = Group.builder().id(nonExistentId).build();
    val r1 = addGroupUserPostRequest(nonExistentGroup, data.getUsers());
    assertThat(r1.getStatusCode()).isEqualTo(NOT_FOUND);
	}

	@Test
	public void addUsersToGroup_AllExistingUnassociatedUsers_Success(){
    // Generate test data
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);

    // Assert there are no users for the group
    val r0 = getGroupUsersGetRequest(group0);
    assertThat(r0.getStatusCode()).isEqualTo(OK);
    val actualUsersBefore = extractPageResultSetFromResponse(r0, User.class);
    assertThat(actualUsersBefore).isEmpty();

    // Add the users to the group
    val r1 = addGroupUserPostRequest(group0, data.getUsers());
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Assert the users were added
    val r2 = getGroupUsersGetRequest(group0);
    assertThat(r2.getStatusCode()).isEqualTo(OK);
    val actualUsersAfter = extractPageResultSetFromResponse(r2, User.class);
    assertThat(actualUsersAfter).containsExactlyInAnyOrderElementsOf(data.getUsers());
	}

	@Test
	public void addUsersToGroup_SomeExistingUsersButAllUnassociated_NotFound(){
    // Setup data
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);
    val existingUserIds = convertToIds(data.getUsers());
    val someNonExistingUserIds = repeatedCallsOf(() -> generateNonExistentId(userService),3);
    val mergedUserIds = concatToSet(existingUserIds, someNonExistingUserIds);

    // Attempt to add nonexistent users to the group
    val r1 = initStringRequest()
        .endpoint("/groups/%s/users", group0.getId())
        .body(mergedUserIds)
        .post();
    assertThat(r1.getStatusCode()).isEqualTo(NOT_FOUND);
	}

	@Test
	public void addUsersToGroup_AllExsitingUsersButSomeAlreadyAssociated_Conflict(){
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);

    // Assert there are no users for the group
    val r0 = getGroupUsersGetRequest(group0);
    assertThat(r0.getStatusCode()).isEqualTo(OK);
    val actualUsersBefore = extractPageResultSetFromResponse(r0, User.class);
    assertThat(actualUsersBefore).isEmpty();

    //Add some new unassociated users
    val someUsers = newArrayList(data.getUsers().get(0));
    val r1 = addGroupUserPostRequest(group0, someUsers );
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Assert that adding already associated users returns a conflict
    val r2 = addGroupUserPostRequest(group0, data.getUsers() );
    assertThat(r2.getStatusCode()).isEqualTo(CONFLICT);
	}

	@Test
	public void removeUsersFromGroup_AllExistingAssociatedUsers_Success(){
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);

    // Assert there are no users for the group
    val r0 = getGroupUsersGetRequest(group0);
    assertThat(r0.getStatusCode()).isEqualTo(OK);
    val actualUsersBefore = extractPageResultSetFromResponse(r0, User.class);
    assertThat(actualUsersBefore).isEmpty();

    // Add users to group
    val r1 = addGroupUserPostRequest(group0, data.getUsers());
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Delete all users
    val r2 = deleteUsersFromGroupDeleteRequest(group0, data.getUsers());
    assertThat(r2.getStatusCode()).isEqualTo(OK);

    // Assert there are no users for the group
    val r3 = getGroupUsersGetRequest(group0);
    assertThat(r3.getStatusCode()).isEqualTo(OK);
    val actualUsersAfter = extractPageResultSetFromResponse(r3, User.class);
    assertThat(actualUsersAfter).isEmpty();
	}

	@Test
	public void removeUsersFromGroup_AllExistingUsersButSomeNotAssociated_Conflict(){
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);

    // Assert there are no users for the group
    val r0 = getGroupUsersGetRequest(group0);
    assertThat(r0.getStatusCode()).isEqualTo(OK);
    val actualUsersBefore = extractPageResultSetFromResponse(r0, User.class);
    assertThat(actualUsersBefore).isEmpty();

    // Add some users to group
    val r1 = addGroupUserPostRequest(group0, newArrayList(data.getUsers().get(0)));
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Delete all users
    val r2 = deleteUsersFromGroupDeleteRequest(group0, data.getUsers());
    assertThat(r2.getStatusCode()).isEqualTo(CONFLICT);
	}

	@Test
	public void removeUsersFromGroup_SomeNonExistingUsersButAllAssociated_NotFound(){
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);

    // Assert there are no users for the group
    val r0 = getGroupUsersGetRequest(group0);
    assertThat(r0.getStatusCode()).isEqualTo(OK);
    val actualUsersBefore = extractPageResultSetFromResponse(r0, User.class);
    assertThat(actualUsersBefore).isEmpty();

    // Add all users to group
    val r1 = addGroupUserPostRequest(group0, data.getUsers());
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Create list of userIds to delete, including one non existent id
    val userIdsToDelete = data.getUsers().stream().map(Identifiable::getId).collect(toList());
    userIdsToDelete.add(generateNonExistentId(userService));

    // Delete existing associated users and non-existing users, and assert a not found error
    val r2 = initStringRequest()
        .endpoint("groups/%s/users/%s", group0.getId(), COMMA.join(userIdsToDelete))
        .delete();
    assertThat(r2.getStatusCode()).isEqualTo(NOT_FOUND);
	}

	@Test
	public void removeUsersFromGroup_NonExistentGroup_NotFound(){
    val data = generateUniqueTestGroupData();
    val existingUserIds = convertToIds(data.getUsers());
    val nonExistentId = generateNonExistentId(groupService);

    val r1 = initStringRequest()
        .endpoint("groups/%s/users/%s", nonExistentId, COMMA.join(existingUserIds))
        .delete();
    assertThat(r1.getStatusCode()).isEqualTo(NOT_FOUND);
	}

	@Test
	public void getUsersFromGroup_FindAllQuery_Success(){
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);

    // Assert without using a controller, there are no users for the group
    val beforeGroup = groupService.getGroupWithRelationships(group0.getId());
    assertThat(beforeGroup.getUsers()).isEmpty();

    // Add users to group
    val r1 = addGroupUserPostRequest(group0, data.getUsers());
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Assert without using a controller, there are users for the group
    val afterGroup = groupService.getGroupWithRelationships(group0.getId());
    assertThat(afterGroup.getUsers()).containsExactlyInAnyOrderElementsOf(data.getUsers());

    // Get user for a group using a controller
    val r2 = initStringRequest()
        .endpoint("groups/%s/users", group0.getId())
        .get();
    assertThat(r2.getStatusCode()).isEqualTo(OK);
    val actualUsers = extractPageResultSetFromResponse(r2, User.class);
    assertThat(actualUsers).containsExactlyInAnyOrderElementsOf(data.getUsers());
	}

	@Test
	public void getUsersFromGroup_NonExistentGroup_NotFound(){
    val nonExistentId = generateNonExistentId(groupService);
    val r1 = initStringRequest()
        .endpoint("groups/%s/users", nonExistentId)
        .get();
    assertThat(r1.getStatusCode()).isEqualTo(NOT_FOUND);
	}

	@Test
	public void getUsersFromGroup_FindSomeQuery_Success(){

    // Create users and groups
    val g1 = entityGenerator.generateRandomGroup();
    val u1 = entityGenerator.setupUser("blueberry banana");
    val u2 = entityGenerator.setupUser("blueberry orange");
    val u3 = entityGenerator.setupUser("banana orange");

    // Update their status
    u1.setStatus(APPROVED);
    u2.setStatus(APPROVED);
    u3.setStatus(DISABLED);

    // Add users to group
    val r1 = addGroupUserPostRequest(g1, newArrayList(u1,u2,u3));
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Search users
    val r2 = initStringRequest()
        .endpoint("groups/%s/user", g1.getId())
        .logging(true)
        .queryParam("query", "orange")
        .queryParam("status", DISABLED)
        .get();
    assertThat(r2.getStatusCode()).isEqualTo(OK);
    val actualUsers2 = extractPageResultSetFromResponse(r2, User.class);
    assertThat(actualUsers2).containsExactlyInAnyOrder(u3);

    val r3 = initStringRequest()
        .endpoint("groups/%s/user", g1.getId())
        .queryParam("query", "orange")
        .queryParam("status", APPROVED)
        .get();
    assertThat(r3.getStatusCode()).isEqualTo(OK);
    val actualUsers3 = extractPageResultSetFromResponse(r3, User.class);
    assertThat(actualUsers3).containsExactlyInAnyOrder(u2);

    val r4 = initStringRequest()
        .endpoint("groups/%s/user", g1.getId())
        .queryParam("status", APPROVED)
        .get();
    assertThat(r4.getStatusCode()).isEqualTo(OK);
    val actualUsers4 = extractPageResultSetFromResponse(r4, User.class);
    assertThat(actualUsers4).containsExactlyInAnyOrder(u1, u2);

    val r5 = initStringRequest()
        .endpoint("groups/%s/user", g1.getId())
        .queryParam("query", "blueberry")
        .get();
    assertThat(r5.getStatusCode()).isEqualTo(OK);
    val actualUsers5 = extractPageResultSetFromResponse(r5, User.class);
    assertThat(actualUsers5).containsExactlyInAnyOrder(u1, u2);

    val r6 = initStringRequest()
        .endpoint("groups/%s/user", g1.getId())
        .queryParam("query", "banana")
        .get();
    assertThat(r6.getStatusCode()).isEqualTo(OK);
    val actualUsers6 = extractPageResultSetFromResponse(r6, User.class);
    assertThat(actualUsers6).containsExactlyInAnyOrder(u1, u3);
	}

	@Test
	public void getGroup_ExistingGroup_Success(){
    val group = entityGenerator.generateRandomGroup();
    assertThat(groupService.isExist(group.getId())).isTrue();
    val r1 = getGroupEntityGetRequest(group);
    assertThat(r1.getStatusCode()).isEqualTo(OK);
	}

	@Test
	public void getGroup_NonExistentGroup_Success(){
    val nonExistentId = generateNonExistentId(groupService);
    val r1 = initStringRequest()
        .endpoint("groups/%s", nonExistentId)
        .get();
    assertThat(r1.getStatusCode()).isEqualTo(NOT_FOUND);
	}

	@Test
	public void UUIDValidation_MalformedUUID_Conflict(){
    val data = generateUniqueTestGroupData();
    val group0 = data.getGroups().get(0);
    val badUUID = "123sksk";

    initStringRequest()
        .endpoint("/groups/%s", badUUID)
        .deleteAnd()
        .assertBadRequest();

    initStringRequest()
        .endpoint("/groups/%s", badUUID)
        .getAnd()
        .assertBadRequest();

    initStringRequest()
        .endpoint("/groups/%s/applications", badUUID)
        .getAnd()
        .assertBadRequest();

    initStringRequest()
        .endpoint("/groups/%s/applications", badUUID)
        .postAnd()
        .assertBadRequest();

    val appIds = mapToList(data.getApplications(), x -> x.getId().toString());
    appIds.add(badUUID);

    // Test when an id in the payload is not a uuid
    initStringRequest()
        .endpoint("/groups/%s/applications", group0.getId())
        .body(appIds)
        .postAnd()
        .assertBadRequest();

    initStringRequest()
        .endpoint("/groups/%s/applications/%s", badUUID, data.getApplications().get(0).getId())
        .deleteAnd()
        .assertBadRequest();

    initStringRequest()
        .endpoint("/groups/%s/applications/%s", group0.getId(), COMMA.join(appIds))
        .deleteAnd()
        .assertBadRequest();

    initStringRequest()
        .endpoint("groups/%s/permissions", badUUID)
        .getAnd()
        .assertBadRequest();

    initStringRequest()
        .endpoint("groups/%s/permissions", badUUID)
        .postAnd()
        .assertBadRequest();

    // Test when an id in the payload is not a uuid
    val body = MAPPER.createArrayNode()
        .add(
            MAPPER.createObjectNode()
            .put("mask", READ.toString())
            .put("policyId", data.getPolicies().get(0).getId().toString()))
        .add(
            MAPPER.createObjectNode()
                .put("mask", READ.toString())
                .put("policyId", badUUID));
    initStringRequest()
        .endpoint("groups/%s/permissions", group0.getId())
        .body(body)
        .postAnd()
        .assertBadRequest();


    val r2 = addGroupPermissionPostRequest(group0, data.getPolicies().get(0), READ);
    assertThat(r2.getStatusCode()).isEqualTo(OK);

    val r3 = getGroupPermissionsGetRequest(group0);
    val actualPermissions = extractPageResultSetFromResponse(r3, GroupPermission.class);
    assertThat(actualPermissions).hasSize(1);
    val existingPermissionId = actualPermissions.get(0).getId();

    initStringRequest()
        .endpoint("groups/%s/permissions/%s", badUUID, existingPermissionId)
        .deleteAnd()
        .assertBadRequest();

    initStringRequest()
        .endpoint("groups/%s/permissions/%s", group0.getId(), badUUID+","+existingPermissionId)
        .deleteAnd()
        .assertBadRequest();

    initStringRequest()
        .endpoint("/groups/%s/users", badUUID)
        .getAnd()
        .assertBadRequest();

    initStringRequest()
        .endpoint("/groups/%s/users", badUUID)
        .postAnd()
        .assertBadRequest();

    val userIds = mapToList(data.getUsers(), x -> x.getId().toString());
    userIds.add(badUUID);

    // Test when an id in the payload is not a uuid
    initStringRequest()
        .endpoint("/groups/%s/users", group0.getId())
        .body(userIds)
        .postAnd()
        .assertBadRequest();

    initStringRequest()
        .endpoint("/groups/%s/users/%s", badUUID, data.getUsers().get(0).getId())
        .deleteAnd()
        .assertBadRequest();

    initStringRequest()
        .endpoint("/groups/%s/users/%s", group0.getId(), COMMA.join(userIds))
        .deleteAnd()
        .assertBadRequest();
	}


	@Test
	public void updateGroup_ExistingGroup_Success(){
		throw new NotImplementedException("need to implement the test 'updateGroup_ExistingGroup_Success'");
	}

	@Test
	public void updateGroup_NonExistentGroup_NotFound(){
		throw new NotImplementedException("need to implement the test 'updateGroup_NonExistentGroup_NotFound'");
	}

	@Test
	public void updateGroup_NameAlreadyExists_Conflict(){
		throw new NotImplementedException("need to implement the test 'updateGroup_NameAlreadyExists_Conflict'");
	}

	@Test
	public void updateGroup_SomeNullFields_Success(){
		throw new NotImplementedException("need to implement the test 'updateGroup_SomeNullFields_Success'");
	}

	@Test
	public void statusValidation_MalformedStatus_Conflict(){
		throw new NotImplementedException("need to implement the test 'statusValidation_MalformedStatus_Conflict'");
	}

	@Test
	public void getScopes_FindAllQuery_Success(){
		throw new NotImplementedException("need to implement the test 'getScopes_FindAllQuery_Success'");
	}

	@Test
	public void getScopes_FindSomeQuery_Success(){
		throw new NotImplementedException("need to implement the test 'getScopes_FindSomeQuery_Success'");
	}

	@Test
	public void addAppsToGroup_NonExistentGroup_NotFound(){
		throw new NotImplementedException("need to implement the test 'addAppsToGroup_NonExistentGroup_NotFound'");
	}

	@Test
	public void addAppsToGroup_AllExistingUnassociatedApps_Success(){
		throw new NotImplementedException("need to implement the test 'addAppsToGroup_AllExistingUnassociatedApps_Success'");
	}

	@Test
	public void addAppsToGroup_SomeExistingAppsButAllUnassociated_NotFound(){
		throw new NotImplementedException("need to implement the test 'addAppsToGroup_SomeExistingAppsButAllUnassociated_NotFound'");
	}

	@Test
	public void addAppsToGroup_AllExsitingAppsButSomeAlreadyAssociated_Conflict(){
		throw new NotImplementedException("need to implement the test 'addAppsToGroup_AllExsitingAppsButSomeAlreadyAssociated_Conflict'");
	}

	@Test
	public void removeAppsFromGroup_AllExistingAssociatedApps_Success(){
		throw new NotImplementedException("need to implement the test 'removeAppsFromGroup_AllExistingAssociatedApps_Success'");
	}

	@Test
	public void removeAppsFromGroup_AllExistingAppsButSomeAlreadyAssociated_Conflict(){
		throw new NotImplementedException("need to implement the test 'removeAppsFromGroup_AllExistingAppsButSomeAlreadyAssociated_Conflict'");
	}

	@Test
	public void removeAppsFromGroup_SomeNonExistingAppsButAllAssociated_NotFound(){
		throw new NotImplementedException("need to implement the test 'removeAppsFromGroup_SomeNonExistingAppsButAllAssociated_NotFound'");
	}

	@Test
	public void removeAppsFromGroup_NonExistentGroup_NotFound(){
		throw new NotImplementedException("need to implement the test 'removeAppsFromGroup_NonExistentGroup_NotFound'");
	}

	@Test
	public void getAppsFromGroup_FindAllQuery_Success(){
		throw new NotImplementedException("need to implement the test 'getAppsFromGroup_FindAllQuery_Success'");
	}

	@Test
	public void getAppsFromGroup_NonExistentGroup_NotFound(){
		throw new NotImplementedException("need to implement the test 'getAppsFromGroup_NonExistentGroup_NotFound'");
	}

	@Test
	public void getAppsFromGroup_FindSomeQuery_Success(){
		throw new NotImplementedException("need to implement the test 'getAppsFromGroup_FindSomeQuery_Success'");
	}

  @SneakyThrows
  private <T> List<T> extractPageResultSetFromResponse(ResponseEntity<String> r, Class<T> tClass){
    assertThat(r.getStatusCode()).isEqualTo(OK);
    assertThat(r.getBody()).isNotNull();
    val page = MAPPER.readTree(r.getBody());
    assertThat(page).isNotNull();
    return stream(page.path("resultSet").iterator())
        .map(x -> MAPPER.convertValue(x, tClass))
        .collect(toImmutableList());
  }

  @SneakyThrows
  private <T> T extractOneEntityFromResponse(ResponseEntity<String> r, Class<T> tClass){
    assertThat(r.getStatusCode()).isEqualTo(OK);
    assertThat(r.getBody()).isNotNull();
    return MAPPER.readValue(r.getBody(), tClass);
  }

  @SneakyThrows
  private <T> Set<T> extractManyEntitiesFromResponse(ResponseEntity<String> r, Class<T> tClass){
    assertThat(r.getStatusCode()).isEqualTo(OK);
    assertThat(r.getBody()).isNotNull();
    return stream(MAPPER.readTree(r.getBody()).iterator())
        .map(x -> MAPPER.convertValue(x, tClass))
        .collect(toImmutableSet());
  }

  @SneakyThrows
  private TestGroupData generateUniqueTestGroupData(){
    val groups = repeatedCallsOf(() -> entityGenerator.generateRandomGroup(), 2);
    val applications = repeatedCallsOf(() -> entityGenerator.generateRandomApplication(), 2);
    val users = repeatedCallsOf(() -> entityGenerator.generateRandomUser(), 2);
    val policies = repeatedCallsOf(() -> entityGenerator.generateRandomPolicy(), 2);

    return TestGroupData.builder()
        .groups(groups)
        .applications(applications)
        .users(users)
        .policies(policies)
        .build();
  }

  @SneakyThrows
  private ResponseEntity<String> addGroupPermissionPostRequest(Group g, Policy p, AccessLevel mask){
    val body = PermissionRequest.builder()
        .mask(mask)
        .policyId(p.getId())
        .build();
    return initStringRequest()
        .endpoint("/policies/%s/permission/group/%s",p.getId(), g.getId())
        .body(body)
        .post();
  }

  private ResponseEntity<String> addGroupApplicationsPostRequest(Group g, Collection<Application> applications){
    val appIds = convertToIds(applications);
    return initStringRequest()
        .endpoint("/groups/%s/applications", g.getId())
        .body(appIds)
        .post();
  }

  private ResponseEntity<String> addGroupUserPostRequest(Group g, Collection<User> users){
    val userIds = convertToIds(users);
    return initStringRequest()
        .endpoint("/groups/%s/users", g.getId())
        .body(userIds)
        .post();
  }

  private ResponseEntity<String> getGroupUsersGetRequest(Group g){
    return initStringRequest()
        .endpoint("/groups/%s/users", g.getId())
        .get();
  }

  private ResponseEntity<String> getGroupApplicationsGetRequest(Group g){
    return initStringRequest()
        .endpoint("/groups/%s/applications", g.getId())
        .get();
  }

  private ResponseEntity<String> getGroupPermissionsGetRequest(Group g){
    return initStringRequest()
        .endpoint("/groups/%s/permissions", g.getId())
        .get();
  }

  private ResponseEntity<String> deleteUsersFromGroupDeleteRequest(Group g, Collection<User> users){
    val userIds = convertToIds(users);
    return initStringRequest()
        .endpoint("/groups/%s/users/%s", g.getId(), COMMA.join(userIds))
        .delete();
  }

  private ResponseEntity<String> deleteGroupDeleteRequest(Group g){
    return initStringRequest()
        .endpoint("/groups/%s", g.getId())
        .delete();
  }

  private ResponseEntity<String> getGroupEntityGetRequest(Group g){
    return initStringRequest()
        .endpoint("/groups/%s", g.getId())
        .get();
  }

  private ResponseEntity<String> createGroupPostRequest(GroupRequest g){
    return initStringRequest()
        .endpoint("/groups")
        .body(g)
        .post();
  }

  private ResponseEntity<String> getUserGetRequest(User u){
    return initStringRequest()
        .endpoint("/users/%s", u.getId())
        .get();
  }

  private ResponseEntity<String> getApplicationGetRequest(Application a){
    return initStringRequest()
        .endpoint("/applications/%s", a.getId())
        .get();
  }

  private ResponseEntity<String> getPolicyGetRequest(Policy p){
    return initStringRequest()
        .endpoint("/policies/%s", p.getId())
        .get();
  }

  @Value
  @Builder
  public static class TestGroupData{
    @NonNull private final List<Group> groups;
    @NonNull private final List<Application> applications;
    @NonNull private final List<User> users;
    @NonNull private final List<Policy> policies;
  }


}
