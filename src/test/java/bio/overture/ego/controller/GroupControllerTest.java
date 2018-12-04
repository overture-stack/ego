package bio.overture.ego.controller;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.utils.EntityGenerator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

@Slf4j
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(classes = AuthorizationServiceMain.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GroupControllerTest {

  @LocalServerPort private int port;
  private TestRestTemplate restTemplate = new TestRestTemplate();
  private HttpHeaders headers = new HttpHeaders();

  @Autowired private GroupService groupService;
  @Autowired private EntityGenerator entityGenerator;

  @Before
  public void Setup() {
    headers.add("Authorization", "Bearer TestToken");
    headers.setContentType(MediaType.APPLICATION_JSON);

    // Start Hibernate Session
    SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
    Session session = sessionFactory.openSession();
    Transaction tx = session.beginTransaction();
  }

  @Test
  public void AddGroup() {

    Group group = new Group("Wizards");

    HttpEntity<Group> entity = new HttpEntity<Group>(group, headers);

    ResponseEntity<String> response = restTemplate.exchange(
        createURLWithPort("/groups"),
        HttpMethod.POST, entity, String.class);

    HttpStatus responseStatus = response.getStatusCode();

    // TODO: Proper response description and testing
    assertEquals(responseStatus, HttpStatus.OK);
  }

  @Test
  public void GetGroup() throws JSONException {

    val group = groupService.create(entityGenerator.createGroup("Group Zero"));
    val groupId = group.getId();

    HttpEntity<String> entity = new HttpEntity<String>(null, headers);

    ResponseEntity<String> response = restTemplate.exchange(
        createURLWithPort(String.format("/groups/%s", groupId)),
        HttpMethod.GET, entity, String.class);

    HttpStatus responseStatus = response.getStatusCode();
    String responseBody = response.getBody();

    String expected = String.format("{\"id\":\"%s\",\"name\":\"Group Zero\",\"description\":null,\"status\":null}", groupId);

    // TODO: Proper response description and testing
    assertEquals(responseStatus, HttpStatus.OK);
    JSONAssert.assertEquals(expected, responseBody, true);
  }

  @Test
  public void ListGroups() throws JSONException {
    entityGenerator.setupTestGroups();

    HttpEntity<String> entity = new HttpEntity<String>(null, headers);

    ResponseEntity<String> response = restTemplate.exchange(
        createURLWithPort("/groups"),
        HttpMethod.GET, entity, String.class);

    HttpStatus responseStatus = response.getStatusCode();
    val responseBody = JSONParser.parseJSON(response.getBody());

    String expected = "";

    // TODO: Proper response description and testing
    assertEquals(responseStatus, HttpStatus.OK);
    JSONAssert.assertEquals(expected, responseBody, true);
  }

  private String createURLWithPort(String uri) {
    return "http://localhost:" + port + uri;
  }
}
