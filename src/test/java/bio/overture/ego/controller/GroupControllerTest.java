package bio.overture.ego.controller;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.enums.EntityStatus;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.UUID;

import static bio.overture.ego.utils.EntityTools.extractAppIds;
import static bio.overture.ego.utils.EntityTools.extractGroupIds;
import static bio.overture.ego.utils.EntityTools.extractIDs;
import static java.util.Arrays.asList;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_ARRAY_ITEMS;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GroupControllerTest {

  @LocalServerPort private int port;
  private TestRestTemplate restTemplate = new TestRestTemplate();
  private HttpHeaders headers = new HttpHeaders();

  private static boolean hasRunEntitySetup = false;

  @Autowired private EntityGenerator entityGenerator;
  @Autowired private GroupService groupService;
  @Autowired private UserService userService;
  @Autowired private ApplicationService applicationService;

  @Before
  public void Setup() {

    // Initial setup of entities (run once
    if (!hasRunEntitySetup) {
      entityGenerator.setupTestUsers();
      entityGenerator.setupTestApplications();
      entityGenerator.setupTestGroups();
      hasRunEntitySetup = true;
    }

    headers.add("Authorization", "Bearer TestToken");
    headers.setContentType(MediaType.APPLICATION_JSON);
  }

  @Test
  public void AddGroup() {

    Group group =
        Group.builder()
            .name("Wizards")
            .status(EntityStatus.PENDING.toString())
            .description("")
            .build();

    HttpEntity<Group> entity = new HttpEntity<Group>(group, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(createURLWithPort("/groups"), HttpMethod.POST, entity, String.class);

    HttpStatus responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void AddUniqueGroup() {

    Group group = entityGenerator.setupGroup("SameSame");

    HttpEntity<Group> entity = new HttpEntity<Group>(group, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(createURLWithPort("/groups"), HttpMethod.POST, entity, String.class);

    HttpStatus responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  public void GetGroup() throws JSONException {

    // Groups created in setup
    val groupId = groupService.getByName("Group One").getId();

    HttpEntity<String> entity = new HttpEntity<String>(null, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(
            createURLWithPort(String.format("/groups/%s", groupId)),
            HttpMethod.GET,
            entity,
            String.class);

    HttpStatus responseStatus = response.getStatusCode();
    String responseBody = response.getBody();

    String expected =
        String.format(
            "{\"id\":\"%s\",\"name\":\"Group One\",\"description\":\"\",\"status\":\"Pending\"}",
            groupId);

    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    assertThatJson(responseBody).isEqualTo(expected);
  }

  @Test
  public void GetGroupNotFound() throws JSONException {
    HttpEntity<String> entity = new HttpEntity<String>(null, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(
            createURLWithPort(String.format("/groups/%s", UUID.randomUUID())),
            HttpMethod.GET,
            entity,
            String.class);

    HttpStatus responseStatus = response.getStatusCode();

    assertThat(responseStatus).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  public void ListGroups() throws JSONException {
    HttpEntity<String> entity = new HttpEntity<String>(null, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(createURLWithPort("/groups"), HttpMethod.GET, entity, String.class);

    HttpStatus responseStatus = response.getStatusCode();
    String responseBody = response.getBody();

    String expected =
        String.format(
            "[{\"id\":\"%s\",\"name\":\"Group One\",\"description\":\"\",\"status\":\"Pending\"}, {\"id\":\"%s\",\"name\":\"Group Two\",\"description\":\"\",\"status\":\"Pending\"}, {\"id\":\"%s\",\"name\":\"Group Three\",\"description\":\"\",\"status\":\"Pending\"}]",
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
  public void UpdateGroup() {

    // Groups created in setup
    val group = entityGenerator.setupGroup("Complete");

    Group update =
        Group.builder()
            .id(group.getId())
            .name("Updated Complete")
            .status(group.getStatus())
            .description(group.getDescription())
            .build();

    HttpEntity<Group> entity = new HttpEntity<Group>(update, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(
            createURLWithPort(String.format("/groups/%s", group.getId())),
            HttpMethod.PUT,
            entity,
            String.class);

    String responseBody = response.getBody();

    HttpStatus responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    assertThatJson(responseBody).node("id").isEqualTo(group.getId());
    assertThatJson(responseBody).node("name").isEqualTo("Updated Complete");
  }

  // TODO - ADD Update non-existent entity

  @Test
  @Ignore
  // TODO - Implement Patch method
  public void PartialUpdateGroup() throws JSONException {

    // Groups created in setup
    val groupId = entityGenerator.setupGroup("Partial").getId();

    val update = "{\"name\":\"Updated Partial\"}";
    HttpEntity<String> entity = new HttpEntity<String>(update, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(
            createURLWithPort(String.format("/groups/%s", groupId)),
            HttpMethod.PATCH,
            entity,
            String.class);

    String responseBody = response.getBody();

    HttpStatus responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    assertThatJson(responseBody).node("id").isEqualTo(groupId);
    assertThatJson(responseBody).node("name").isEqualTo("Updated Partial");
  }

  @Test
  public void DeleteOne() throws JSONException {

    val group = entityGenerator.setupGroup("DeleteOne");
    val groupId = group.getId();

    // Users for test
    val userOne = entityGenerator.setupUser("TempGroup User");

    // Application for test
    val appOne = entityGenerator.setupApplication("TempGroupApp");

    // REST to get users/app in group
    val usersBody = asList(userOne.getId().toString());
    val appsBody = asList(appOne.getId().toString());

    HttpEntity<List> saveGroupUsers = new HttpEntity<>(usersBody, headers);
    HttpEntity<List> saveGroupApps = new HttpEntity<>(appsBody, headers);

    ResponseEntity<String> saveGroupUsersRes =
        restTemplate.exchange(
            createURLWithPort(String.format("/groups/%s/users", group.getId())),
            HttpMethod.POST,
            saveGroupUsers,
            String.class);

    ResponseEntity<String> saveGroupAppsRes =
        restTemplate.exchange(
            createURLWithPort(String.format("/groups/%s/applications", group.getId())),
            HttpMethod.POST,
            saveGroupApps,
            String.class);

    // Check user-group relationship is there
    val userWithGroup = userService.getByName("TempGroupUser@domain.com");
    assertThat(extractGroupIds(userWithGroup.getGroups())).contains(groupId);

    // Check app-group relationship is there
    val applicationWithGroup = applicationService.getByClientId("TempGroupApp");
    assertThat(extractGroupIds(applicationWithGroup.getGroups())).contains(groupId);

    HttpEntity<String> entity = new HttpEntity<String>(null, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(
            createURLWithPort(String.format("/groups/%s", groupId)),
            HttpMethod.DELETE,
            entity,
            String.class);

    HttpStatus responseStatus = response.getStatusCode();

    // Check http response
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);

    // Check user-group relationship is also deleted
    val userWithoutGroup = userService.getByName("TempGroupUser@domain.com");
    assertThat(userWithoutGroup).isNotNull();
    assertThat(extractGroupIds(userWithoutGroup.getGroups())).doesNotContain(groupId);

    // Check app-group relationship is also deleted
    val applicationWithoutGroup = applicationService.getByClientId("TempGroupApp");
    assertThat(applicationWithoutGroup).isNotNull();
    assertThat(extractGroupIds(applicationWithoutGroup.getGroups())).doesNotContain(groupId);

    // Check group is deleted
    assertThat(groupService.getByName("DeleteOne")).isNull();
  }

  //TODO: [rtisma] will eventually be fixed when properly using query by Specification, which will allow for runtime base queries. This will allow us to define fetch strategy at run time
  @Test
  public void AddUsersToGroup() {

    val group = entityGenerator.setupGroup("GroupWithUsers");

    val userOne = userService.getByName("FirstUser@domain.com");
    val userTwo = userService.getByName("SecondUser@domain.com");

    val body = asList(userOne.getId().toString(), userTwo.getId().toString());

    HttpEntity<List> entity = new HttpEntity<>(body, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(
            createURLWithPort(String.format("/groups/%s/users", group.getId())),
            HttpMethod.POST,
            entity,
            String.class);

    HttpStatus responseStatus = response.getStatusCode();
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
  public void AddAppsToGroup() {

    val group = entityGenerator.setupGroup("GroupWithApps");

    val appOne = applicationService.getByClientId("111111");
    val appTwo = applicationService.getByClientId("222222");

    val body = asList(appOne.getId().toString(), appTwo.getId().toString());

    HttpEntity<List> entity = new HttpEntity<>(body, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(
            createURLWithPort(String.format("/groups/%s/applications", group.getId())),
            HttpMethod.POST,
            entity,
            String.class);

    HttpStatus responseStatus = response.getStatusCode();
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

  private String createURLWithPort(String uri) {
    return "http://localhost:" + port + uri;
  }
}
