package bio.overture.ego.controller;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.utils.EntityGenerator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_EXTRA_ARRAY_ITEMS;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.Assert.assertEquals;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = AuthorizationServiceMain.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GroupControllerTest {

  @LocalServerPort private int port;
  private TestRestTemplate restTemplate = new TestRestTemplate();
  private HttpHeaders headers = new HttpHeaders();

  private static boolean hasRunEntitySetup = false;

  @Autowired private EntityGenerator entityGenerator;
  @Autowired private GroupService groupService;

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

    ResponseEntity<String> response = restTemplate.exchange(
        createURLWithPort("/groups"),
        HttpMethod.POST, entity, String.class);

    HttpStatus responseStatus = response.getStatusCode();

    assertEquals(responseStatus, HttpStatus.OK);
  }

  @Test
  public void GetGroup() throws JSONException {

    // Groups created in setup
    val groupId = groupService.getByName("Group One").getId();

    HttpEntity<String> entity = new HttpEntity<String>(null, headers);

    ResponseEntity<String> response = restTemplate.exchange(
        createURLWithPort(String.format("/groups/%s", groupId)),
        HttpMethod.GET, entity, String.class);

    HttpStatus responseStatus = response.getStatusCode();
    String responseBody = response.getBody();

    String expected = String.format("{\"id\":\"%s\",\"name\":\"Group One\",\"description\":null,\"status\":null}", groupId);

    assertEquals(responseStatus, HttpStatus.OK);
    assertThatJson(responseBody).isEqualTo(expected);
  }

  @Test
  public void ListGroups() throws JSONException {
    HttpEntity<String> entity = new HttpEntity<String>(null, headers);

    ResponseEntity<String> response = restTemplate.exchange(
        createURLWithPort("/groups"),
        HttpMethod.GET, entity, String.class);

    HttpStatus responseStatus = response.getStatusCode();
    String responseBody = response.getBody();

    String expected = String.format(
        "[{\"id\":\"%s\",\"name\":\"Group One\",\"description\":null,\"status\":null}, {\"id\":\"%s\",\"name\":\"Group Two\",\"description\":null,\"status\":null}, {\"id\":\"%s\",\"name\":\"Group Three\",\"description\":null,\"status\":null}]",
        groupService.getByName("Group One").getId(),
        groupService.getByName("Group Two").getId(),
        groupService.getByName("Group Three").getId()
    );

    assertEquals(HttpStatus.OK, responseStatus);
    assertThatJson(responseBody).when(IGNORING_EXTRA_ARRAY_ITEMS, IGNORING_ARRAY_ORDER).node("resultSet").isEqualTo(expected);
  }

  private String createURLWithPort(String uri) {
    return "http://localhost:" + port + uri;
  }
}
