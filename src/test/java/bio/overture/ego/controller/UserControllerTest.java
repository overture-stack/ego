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

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @LocalServerPort private int port;
  private TestRestTemplate restTemplate = new TestRestTemplate();
  private HttpHeaders headers = new HttpHeaders();

  private static boolean hasRunEntitySetup = false;

  @Autowired private EntityGenerator entityGenerator;
  @Autowired private GroupService groupService;
  @Autowired private UserService userService;
  @Autowired private ApplicationService applicationService;

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
    Iterable<JsonNode> resultSetIterable =
        () -> ((ArrayNode) responseJson.get("resultSet")).iterator();
    val userNames =
        StreamSupport.stream(resultSetIterable.spliterator(), false)
            .map(j -> j.get("name").asText())
            .collect(toList());
    assertThat(userNames)
        .contains("FirstUser@domain.com", "SecondUser@domain.com", "ThirdUser@domain.com");
  }

  @Test
  @SneakyThrows
  public void listUsersWithFilter() {}

  private String createURLWithPort(String uri) {
    return "http://localhost:" + port + uri;
  }
}
