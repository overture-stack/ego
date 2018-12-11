package bio.overture.ego.controller;

import static bio.overture.ego.utils.CollectionUtils.listOf;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_ARRAY_ITEMS;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import java.util.UUID;
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
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

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

    Group group = new Group("Wizards");

    HttpEntity<Group> entity = new HttpEntity<Group>(group, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(createURLWithPort("/groups"), HttpMethod.POST, entity, String.class);

    HttpStatus responseStatus = response.getStatusCode();
    assertEquals(HttpStatus.OK, responseStatus);
  }

  @Test
  public void AddUniqueGroup() {

    entityGenerator.setupGroup("SameSame");
    Group group = new Group("SameSame");

    HttpEntity<Group> entity = new HttpEntity<Group>(group, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(createURLWithPort("/groups"), HttpMethod.POST, entity, String.class);

    HttpStatus responseStatus = response.getStatusCode();
    assertEquals(HttpStatus.CONFLICT, responseStatus); // TODO
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
            "{\"id\":\"%s\",\"name\":\"Group One\",\"description\":null,\"status\":null}", groupId);

    assertEquals(HttpStatus.OK, responseStatus);
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

    assertEquals(HttpStatus.NOT_FOUND, responseStatus); // TODO
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
            "[{\"id\":\"%s\",\"name\":\"Group One\",\"description\":null,\"status\":null}, {\"id\":\"%s\",\"name\":\"Group Two\",\"description\":null,\"status\":null}, {\"id\":\"%s\",\"name\":\"Group Three\",\"description\":null,\"status\":null}]",
            groupService.getByName("Group One").getId(),
            groupService.getByName("Group Two").getId(),
            groupService.getByName("Group Three").getId());

    assertEquals(HttpStatus.OK, responseStatus);
    assertThatJson(responseBody)
        .when(IGNORING_EXTRA_ARRAY_ITEMS, IGNORING_ARRAY_ORDER)
        .node("resultSet")
        .isEqualTo(expected);
  }

  // TODO - ADD List/Filter tests

  @Test
  public void UpdateGroup() {

    // Groups created in setup
    val groupId = entityGenerator.setupGroup("Complete").getId();

    Group update = new Group("Updated Complete");
    update.setId(groupId);

    HttpEntity<Group> entity = new HttpEntity<Group>(update, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(
            createURLWithPort(String.format("/groups/%s", groupId)),
            HttpMethod.PUT,
            entity,
            String.class);

    String responseBody = response.getBody();

    HttpStatus responseStatus = response.getStatusCode();
    assertEquals(HttpStatus.OK, responseStatus);
    assertThatJson(responseBody).node("id").isEqualTo(groupId);
    assertThatJson(responseBody).node("name").isEqualTo("Updated Complete");
  }

  // TODO - ADD Update non-existent entity

  @Test
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
            String.class); // TODO - No Patch Method

    String responseBody = response.getBody();

    HttpStatus responseStatus = response.getStatusCode();
    assertEquals(HttpStatus.OK, responseStatus);
    assertThatJson(responseBody).node("id").isEqualTo(groupId);
    assertThatJson(responseBody).node("name").isEqualTo("Updated Partial");
  }

  @Test
  public void DeleteOne() throws JSONException {

    // Groups created in setup
    val groupId = entityGenerator.setupGroup("Temporary").getId();

    // Add a user to this group
    userService.addUserToGroups(
        userService.getByName("FirstUser@domain.com").getId().toString(),
        listOf(groupId.toString()));

    // TODO - ADD application groups relationship

    HttpEntity<String> entity = new HttpEntity<String>(null, headers);

    ResponseEntity<String> response =
        restTemplate.exchange(
            createURLWithPort(String.format("/groups/%s", groupId)),
            HttpMethod.DELETE,
            entity,
            String.class);

    HttpStatus responseStatus = response.getStatusCode();

    // Check http response
    assertEquals(HttpStatus.OK, responseStatus);

    // Check user-group relationship is also deleted
    assertNotEquals(null, userService.getByName("FirstUser@domain.com"));

    // Check group is deleted
    assertEquals(null, groupService.getByName("Temporary"));
  }

  // TODO - ADD tests for adding user/apps to groups

  private String createURLWithPort(String uri) {
    return "http://localhost:" + port + uri;
  }
}
