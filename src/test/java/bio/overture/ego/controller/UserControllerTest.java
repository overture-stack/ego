/*
 * Copyright (c) 2019. The Ontario Institute for Cancer Research. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package bio.overture.ego.controller;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.StreamSupport;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserControllerTest {

  /** Constants */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** State */
  @LocalServerPort private int port;

  private TestRestTemplate restTemplate = new TestRestTemplate();
  private HttpHeaders headers = new HttpHeaders();

  private static boolean hasRunEntitySetup = false;

  /** Dependencies */
  @Autowired private EntityGenerator entityGenerator;

  @Autowired private UserService userService;

  @Before
  public void setup() {

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
  public void addUser() {

    val user =
        User.builder()
            .firstName("foo")
            .lastName("bar")
            .email("foobar@foo.bar")
            .preferredLanguage("English")
            .role("USER")
            .status("Approved")
            .build();

    val entity = new HttpEntity<User>(user, headers);

    val response =
        restTemplate.exchange(createURLWithPort("/users"), HttpMethod.POST, entity, String.class);

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void addUniqueUser() {
    val user1 =
        User.builder()
            .firstName("unique")
            .lastName("unique")
            .email("unique@unique.com")
            .preferredLanguage("English")
            .role("USER")
            .status("Approved")
            .build();
    val user2 =
        User.builder()
            .firstName("unique")
            .lastName("unique")
            .email("unique@unique.com")
            .preferredLanguage("English")
            .role("USER")
            .status("Approved")
            .build();

    val entity1 = new HttpEntity<User>(user1, headers);
    val response1 =
        restTemplate.exchange(createURLWithPort("/users"), HttpMethod.POST, entity1, String.class);
    val responseStatus1 = response1.getStatusCode();

    assertThat(responseStatus1).isEqualTo(HttpStatus.OK);

    // Return a 409 conflict because email already exists for a registered user.
    val entity2 = new HttpEntity<User>(user2, headers);
    val response2 =
        restTemplate.exchange(createURLWithPort("/users"), HttpMethod.POST, entity2, String.class);
    val responseStatus2 = response2.getStatusCode();
    assertThat(responseStatus2).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  @SneakyThrows
  public void getUser() {

    // Users created in setup
    val userId = userService.getByName("FirstUser@domain.com").getId();
    val entity = new HttpEntity<String>(null, headers);
    val response =
        restTemplate.exchange(
            createURLWithPort(String.format("/users/%s", userId)),
            HttpMethod.GET,
            entity,
            String.class);

    val responseStatus = response.getStatusCode();
    val responseJson = MAPPER.readTree(response.getBody());

    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    assertThat(responseJson.get("firstName").asText()).isEqualTo("First");
    assertThat(responseJson.get("lastName").asText()).isEqualTo("User");
    assertThat(responseJson.get("name").asText()).isEqualTo("FirstUser@domain.com");
    assertThat(responseJson.get("preferredLanguage").asText()).isEqualTo("English");
    assertThat(responseJson.get("status").asText()).isEqualTo("Approved");
    assertThat(responseJson.get("id").asText()).isEqualTo(userId.toString());
  }

  @Test
  public void getUser404() {
    val entity = new HttpEntity<String>(null, headers);
    val response =
        restTemplate.exchange(
            createURLWithPort(String.format("/users/%s", UUID.randomUUID().toString())),
            HttpMethod.GET,
            entity,
            String.class);

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @SneakyThrows
  public void listUsersNoFilter() {
    val entity = new HttpEntity<String>(null, headers);
    val response =
        restTemplate.exchange(createURLWithPort("/users/"), HttpMethod.GET, entity, String.class);

    val responseStatus = response.getStatusCode();
    val responseJson = MAPPER.readTree(response.getBody());

    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    assertThat(responseJson.get("count").asInt()).isGreaterThanOrEqualTo(3);
    assertThat(responseJson.get("resultSet").isArray()).isTrue();

    // Verify that the returned Users are the ones from the setup.
    Iterable<JsonNode> resultSetIterable = () -> responseJson.get("resultSet").iterator();
    val userNames =
        StreamSupport.stream(resultSetIterable.spliterator(), false)
            .map(j -> j.get("name").asText())
            .collect(toList());
    assertThat(userNames)
        .contains("FirstUser@domain.com", "SecondUser@domain.com", "ThirdUser@domain.com");
  }

  @Test
  @SneakyThrows
  public void listUsersWithQuery() {
    val entity = new HttpEntity<String>(null, headers);
    val response =
        restTemplate.exchange(
            createURLWithPort("/users?query=FirstUser"), HttpMethod.GET, entity, String.class);

    val responseStatus = response.getStatusCode();
    val responseJson = MAPPER.readTree(response.getBody());

    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    assertThat(responseJson.get("count").asInt()).isEqualTo(1);
    assertThat(responseJson.get("resultSet").isArray()).isTrue();
    assertThat(responseJson.get("resultSet").elements().next().get("name").asText())
        .isEqualTo("FirstUser@domain.com");
  }

  @Test
  public void updateUser() {
    val user = entityGenerator.setupUser("update test");
    val update = User.builder().id(user.getId()).status("Rejected").build();

    val entity = new HttpEntity<User>(update, headers);
    val response =
        restTemplate.exchange(
            createURLWithPort(String.format("/users/%s", user.getId())),
            HttpMethod.PUT,
            entity,
            String.class);

    val responseBody = response.getBody();

    HttpStatus responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    assertThatJson(responseBody).node("id").isEqualTo(user.getId());
    assertThatJson(responseBody).node("status").isEqualTo("Rejected");
  }

  @Test
  @SneakyThrows
  public void addGroupToUser() {
    val userId = entityGenerator.setupUser("Group1 User").getId();
    val groupId = entityGenerator.setupGroup("Addone Group").getId().toString();

    val entity = new HttpEntity<>(singletonList(groupId), headers);
    val response =
        restTemplate.exchange(
            createURLWithPort(String.format("/users/%s/groups", userId)),
            HttpMethod.POST,
            entity,
            String.class);

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);

    val groupResponse =
        restTemplate.exchange(
            createURLWithPort(String.format("/users/%s/groups", userId)),
            HttpMethod.GET,
            entity,
            String.class);

    val groupResponseStatus = groupResponse.getStatusCode();
    assertThat(groupResponseStatus).isEqualTo(HttpStatus.OK);

    val groupResponseJson = MAPPER.readTree(groupResponse.getBody());
    assertThat(groupResponseJson.get("count").asInt()).isEqualTo(1);
    assertThat(groupResponseJson.get("resultSet").elements().next().get("id").asText())
        .isEqualTo(groupId);
  }

  @Test
  @SneakyThrows
  public void deleteGroupFromUser() {
    val userId = entityGenerator.setupUser("DeleteGroup User").getId();
    val deleteGroup = entityGenerator.setupGroup("Delete One Group").getId().toString();
    val remainGroup = entityGenerator.setupGroup("Don't Delete This One").getId().toString();

    val entity = new HttpEntity<>(asList(deleteGroup, remainGroup), headers);
    restTemplate.exchange(
        createURLWithPort(String.format("/users/%s/groups", userId)),
        HttpMethod.POST,
        entity,
        String.class);
    val groupResponse =
        restTemplate.exchange(
            createURLWithPort(String.format("/users/%s/groups", userId)),
            HttpMethod.GET,
            entity,
            String.class);

    val groupResponseStatus = groupResponse.getStatusCode();
    assertThat(groupResponseStatus).isEqualTo(HttpStatus.OK);
    val groupResponseJson = MAPPER.readTree(groupResponse.getBody());
    assertThat(groupResponseJson.get("count").asInt()).isEqualTo(2);

    val deleteEntity = new HttpEntity<String>(null, headers);
    val deleteResponse =
        restTemplate.exchange(
            createURLWithPort(String.format("/users/%s/groups/%s", userId, deleteGroup)),
            HttpMethod.DELETE,
            deleteEntity,
            String.class);

    val deleteResponseStatus = deleteResponse.getStatusCode();
    assertThat(deleteResponseStatus).isEqualTo(HttpStatus.OK);

    val secondGetResponse =
        restTemplate.exchange(
            createURLWithPort(String.format("/users/%s/groups", userId)),
            HttpMethod.GET,
            entity,
            String.class);
    val secondGetResponseStatus = deleteResponse.getStatusCode();
    assertThat(secondGetResponseStatus).isEqualTo(HttpStatus.OK);
    val secondGetResponseJson = MAPPER.readTree(secondGetResponse.getBody());
    assertThat(secondGetResponseJson.get("count").asInt()).isEqualTo(1);
    assertThat(secondGetResponseJson.get("resultSet").elements().next().get("id").asText())
        .isEqualTo(remainGroup);
  }

  @Test
  @SneakyThrows
  public void addApplicationToUser() {
    val userId = entityGenerator.setupUser("AddApp1 User").getId();
    val appId = entityGenerator.setupApplication("app1").getId().toString();

    val entity = new HttpEntity<>(singletonList(appId), headers);
    val response =
        restTemplate.exchange(
            createURLWithPort(String.format("/users/%s/applications", userId)),
            HttpMethod.POST,
            entity,
            String.class);

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);

    val appResponse =
        restTemplate.exchange(
            createURLWithPort(String.format("/users/%s/applications", userId)),
            HttpMethod.GET,
            entity,
            String.class);

    val appResponseStatus = appResponse.getStatusCode();
    assertThat(appResponseStatus).isEqualTo(HttpStatus.OK);

    val groupResponseJson = MAPPER.readTree(appResponse.getBody());
    assertThat(groupResponseJson.get("count").asInt()).isEqualTo(1);
    assertThat(groupResponseJson.get("resultSet").elements().next().get("id").asText())
        .isEqualTo(appId);
  }

  @Test
  @SneakyThrows
  public void deleteApplicationFromUser() {
    val userId = entityGenerator.setupUser("App2 User").getId();
    val deleteApp = entityGenerator.setupApplication("deleteApp").getId().toString();
    val remainApp = entityGenerator.setupApplication("remainApp").getId().toString();

    val entity = new HttpEntity<>(asList(deleteApp, remainApp), headers);
    val appResponse =
        restTemplate.exchange(
            createURLWithPort(String.format("/users/%s/applications", userId)),
            HttpMethod.POST,
            entity,
            String.class);

    log.info(appResponse.getBody());

    val appResponseStatus = appResponse.getStatusCode();
    assertThat(appResponseStatus).isEqualTo(HttpStatus.OK);

    val deleteEntity = new HttpEntity<String>(null, headers);
    val deleteResponse =
        restTemplate.exchange(
            createURLWithPort(String.format("/users/%s/applications/%s", userId, deleteApp)),
            HttpMethod.DELETE,
            deleteEntity,
            String.class);

    val deleteResponseStatus = deleteResponse.getStatusCode();
    assertThat(deleteResponseStatus).isEqualTo(HttpStatus.OK);

    val secondGetResponse =
        restTemplate.exchange(
            createURLWithPort(String.format("/users/%s/applications", userId)),
            HttpMethod.GET,
            entity,
            String.class);
    val secondGetResponseStatus = deleteResponse.getStatusCode();
    assertThat(secondGetResponseStatus).isEqualTo(HttpStatus.OK);
    val secondGetResponseJson = MAPPER.readTree(secondGetResponse.getBody());
    assertThat(secondGetResponseJson.get("count").asInt()).isEqualTo(1);
    assertThat(secondGetResponseJson.get("resultSet").elements().next().get("id").asText())
        .isEqualTo(remainApp);
  }

  private String createURLWithPort(String uri) {
    return "http://localhost:" + port + uri;
  }
}
