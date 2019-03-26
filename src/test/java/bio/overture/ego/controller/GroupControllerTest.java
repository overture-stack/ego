package bio.overture.ego.controller;

import static bio.overture.ego.model.enums.StatusType.PENDING;
import static bio.overture.ego.utils.EntityTools.extractAppIds;
import static bio.overture.ego.utils.EntityTools.extractIDs;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_ARRAY_ITEMS;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

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
    val group = Group.builder().name("Wizards").status(PENDING).description("").build();

    val response = initStringRequest().endpoint("/groups").body(group).post();

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void addUniqueGroup() {
    val group = entityGenerator.setupGroup("SameSame");

    val response = initStringRequest().endpoint("/groups").body(group).post();

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.CONFLICT);
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

    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
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

    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
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
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
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
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    assertThatJson(responseBody).node("id").isEqualTo(groupId);
    assertThatJson(responseBody).node("name").isEqualTo("Updated Partial");
  }

  @Test
  public void deleteOne() {
    val group = entityGenerator.setupGroup("DeleteOne");
    val groupId = group.getId();

    // Users for test
    val userOne = entityGenerator.setupUser("TempGroup User");
    val userId = userOne.getId();

    // Application for test
    val appOne = entityGenerator.setupApplication("TempGroupApp");
    val appId = appOne.getId();

    // REST to get users/app in group
    val usersBody = singletonList(userOne.getId().toString());
    val appsBody = singletonList(appOne.getId().toString());

    initStringRequest().endpoint("/groups/%s/users", group.getId()).body(usersBody).post();
    initStringRequest().endpoint("/groups/%s/applications", group.getId()).body(appsBody).post();

    val response = initStringRequest().endpoint("/groups/%s", groupId).delete();

    val responseStatus = response.getStatusCode();

    // Check http response
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);

    // Check user-group relationship is also deleted
    val userWithoutGroup = initStringRequest().endpoint("/users/%s/groups", userId).get();
    assertThat(userWithoutGroup.getBody()).doesNotContain(groupId.toString());

    // Check user-group relationship is also deleted
    val applicationWithoutGroup =
        initStringRequest().endpoint("/applications/%s/groups", appId).get();
    assertThat(applicationWithoutGroup.getBody()).doesNotContain(groupId.toString());

    // Check group is deleted
    val groupResponse = initStringRequest().endpoint("/groups/%s", groupId).get();
    log.info(groupResponse.getBody());
    assertThat(groupResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);

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
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);

    val getResponse = initStringRequest().endpoint("/groups/%s/users", groupId).get();
    val getResponseStatus = getResponse.getStatusCode();
    assertThat(getResponseStatus).isEqualTo(HttpStatus.OK);
    val getResponseJson = MAPPER.readTree(getResponse.getBody());
    assertThat(getResponseJson.get("count").asInt()).isEqualTo(2);

    val deleteResponse =
        initStringRequest().endpoint("/groups/%s/users/%s", groupId, deleteUser).delete();

    val deleteResponseStatus = deleteResponse.getStatusCode();
    assertThat(deleteResponseStatus).isEqualTo(HttpStatus.OK);

    val secondGetResponse = initStringRequest().endpoint("/groups/%s/users", groupId).get();

    val secondGetResponseStatus = deleteResponse.getStatusCode();
    assertThat(secondGetResponseStatus).isEqualTo(HttpStatus.OK);
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
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);

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
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);

    val getResponse = initStringRequest().endpoint("/groups/%s/applications", groupId).get();
    val getResponseStatus = getResponse.getStatusCode();
    assertThat(getResponseStatus).isEqualTo(HttpStatus.OK);
    val getResponseJson = MAPPER.readTree(getResponse.getBody());
    assertThat(getResponseJson.get("count").asInt()).isEqualTo(2);

    val deleteResponse =
        initStringRequest().endpoint("/groups/%s/applications/%s", groupId, deleteApp).delete();

    val deleteResponseStatus = deleteResponse.getStatusCode();
    assertThat(deleteResponseStatus).isEqualTo(HttpStatus.OK);

    val secondGetResponse = initStringRequest().endpoint("/groups/%s/applications", groupId).get();

    val secondGetResponseStatus = deleteResponse.getStatusCode();
    assertThat(secondGetResponseStatus).isEqualTo(HttpStatus.OK);
    val secondGetResponseJson = MAPPER.readTree(secondGetResponse.getBody());
    assertThat(secondGetResponseJson.get("count").asInt()).isEqualTo(1);
    assertThat(secondGetResponseJson.get("resultSet").elements().next().get("id").asText())
        .isEqualTo(remainApp);
  }
}
