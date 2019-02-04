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

import static org.assertj.core.api.Assertions.assertThat;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.service.PolicyService;
import bio.overture.ego.utils.EntityGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
public class PolicyControllerTest {

  /** Constants */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** State */
  @LocalServerPort private int port;

  private TestRestTemplate restTemplate = new TestRestTemplate();
  private HttpHeaders headers = new HttpHeaders();
  private static boolean hasRunEntitySetup = false;

  /** Dependencies */
  @Autowired private EntityGenerator entityGenerator;

  @Autowired PolicyService policyService;

  @Before
  public void setup() {
    // Initial setup of entities (run once
    if (!hasRunEntitySetup) {
      entityGenerator.setupTestUsers();
      entityGenerator.setupTestGroups();
      entityGenerator.setupTestPolicies();
      hasRunEntitySetup = true;
    }

    headers.add("Authorization", "Bearer TestToken");
    headers.setContentType(MediaType.APPLICATION_JSON);
  }

  @Test
  @SneakyThrows
  public void addPolicy() {
    val policy = Policy.builder().name("AddPolicy").build();

    val entity = new HttpEntity<Policy>(policy, headers);

    val response =
        restTemplate.exchange(
            createURLWithPort("/policies"), HttpMethod.POST, entity, String.class);

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    val responseJson = MAPPER.readTree(response.getBody());

    log.info(response.getBody());

    assertThat(responseJson.get("name").asText()).isEqualTo("AddPolicy");
  }

  @Test
  @SneakyThrows
  public void addUniquePolicy() {
    val policy1 = Policy.builder().name("PolicyUnique").build();
    val policy2 = Policy.builder().name("PolicyUnique").build();

    val entity1 = new HttpEntity<Policy>(policy1, headers);
    val response1 =
        restTemplate.exchange(
            createURLWithPort("/policies"), HttpMethod.POST, entity1, String.class);

    val responseStatus1 = response1.getStatusCode();
    assertThat(responseStatus1).isEqualTo(HttpStatus.OK);

    val entity2 = new HttpEntity<Policy>(policy2, headers);
    val response2 =
        restTemplate.exchange(
            createURLWithPort("/policies"), HttpMethod.POST, entity2, String.class);
    val responseStatus2 = response2.getStatusCode();
    assertThat(responseStatus2).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  @SneakyThrows
  public void getPolicy() {
    val policyId = policyService.getByName("Study001").getId();
    val entity = new HttpEntity<String>(null, headers);
    val response =
        restTemplate.exchange(
            createURLWithPort(String.format("/policies/%s", policyId)),
            HttpMethod.GET,
            entity,
            String.class);

    val responseStatus = response.getStatusCode();
    val responseJson = MAPPER.readTree(response.getBody());

    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    assertThat(responseJson.get("name").asText()).isEqualTo("Study001");
  }

  @Test
  @SneakyThrows
  public void addGroupPermission() {
    val policyId = entityGenerator.setupSinglePolicy("AddGroupPermission").getId().toString();
    val groupId = entityGenerator.setupGroup("GroupPolicyAdd").getId().toString();

    val entity = new HttpEntity<String>("WRITE", headers);
    val response =
        restTemplate.exchange(
            createURLWithPort(String.format("/policies/%s/permission/group/%s", policyId, groupId)),
            HttpMethod.POST,
            entity,
            String.class);

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    // TODO: Fix it so that POST returns JSON, not just random string message

    val getResponse =
        restTemplate.exchange(
            createURLWithPort(String.format("/policies/%s/groups", policyId)),
            HttpMethod.GET,
            new HttpEntity<String>(null, headers),
            String.class);

    val getResponseStatus = getResponse.getStatusCode();
    val getResponseJson = MAPPER.readTree(getResponse.getBody());
    val groupPermissionJson = getResponseJson.get(0);

    assertThat(getResponseStatus).isEqualTo(HttpStatus.OK);
    assertThat(groupPermissionJson.get("id").asText()).isEqualTo(groupId);
    assertThat(groupPermissionJson.get("mask").asText()).isEqualTo("WRITE");
  }

  @Test
  @SneakyThrows
  public void deleteGroupPermission() {
    val policyId = entityGenerator.setupSinglePolicy("DeleteGroupPermission").getId().toString();
    val groupId = entityGenerator.setupGroup("GroupPolicyDelete").getId().toString();

    val entity = new HttpEntity<String>("WRITE", headers);
    val response =
        restTemplate.exchange(
            createURLWithPort(String.format("/policies/%s/permission/group/%s", policyId, groupId)),
            HttpMethod.POST,
            entity,
            String.class);

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    // TODO: Fix it so that POST returns JSON, not just random string message

    val deleteResponse =
        restTemplate.exchange(
            createURLWithPort(String.format("/policies/%s/permission/group/%s", policyId, groupId)),
            HttpMethod.DELETE,
            new HttpEntity<String>(null, headers),
            String.class);

    val deleteResponseStatus = deleteResponse.getStatusCode();
    assertThat(deleteResponseStatus).isEqualTo(HttpStatus.OK);

    val getResponse =
        restTemplate.exchange(
            createURLWithPort(String.format("/policies/%s/groups", policyId)),
            HttpMethod.GET,
            new HttpEntity<String>(null, headers),
            String.class);

    val getResponseStatus = getResponse.getStatusCode();
    val getResponseJson = (ArrayNode) MAPPER.readTree(getResponse.getBody());

    assertThat(getResponseStatus).isEqualTo(HttpStatus.OK);
    assertThat(getResponseJson.size()).isEqualTo(0);
  }

  @Test
  @SneakyThrows
  public void addUserPermission() {
    val policyId = entityGenerator.setupSinglePolicy("AddUserPermission").getId().toString();
    val userId = entityGenerator.setupUser("UserPolicy Add").getId().toString();

    val entity = new HttpEntity<String>("READ", headers);
    val response =
        restTemplate.exchange(
            createURLWithPort(String.format("/policies/%s/permission/user/%s", policyId, userId)),
            HttpMethod.POST,
            entity,
            String.class);

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    // TODO: Fix it so that POST returns JSON, not just random string message

    val getResponse =
        restTemplate.exchange(
            createURLWithPort(String.format("/policies/%s/users", policyId)),
            HttpMethod.GET,
            new HttpEntity<String>(null, headers),
            String.class);

    val getResponseStatus = getResponse.getStatusCode();
    val getResponseJson = MAPPER.readTree(getResponse.getBody());
    val groupPermissionJson = getResponseJson.get(0);

    assertThat(getResponseStatus).isEqualTo(HttpStatus.OK);
    assertThat(groupPermissionJson.get("id").asText()).isEqualTo(userId);
    assertThat(groupPermissionJson.get("mask").asText()).isEqualTo("READ");
  }

  @Test
  @SneakyThrows
  public void deleteUserPermission() {
    val policyId = entityGenerator.setupSinglePolicy("DeleteGroupPermission").getId().toString();
    val userId = entityGenerator.setupUser("UserPolicy Delete").getId().toString();

    val entity = new HttpEntity<String>("WRITE", headers);
    val response =
        restTemplate.exchange(
            createURLWithPort(String.format("/policies/%s/permission/user/%s", policyId, userId)),
            HttpMethod.POST,
            entity,
            String.class);

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    // TODO: Fix it so that POST returns JSON, not just random string message

    val deleteResponse =
        restTemplate.exchange(
            createURLWithPort(String.format("/policies/%s/permission/user/%s", policyId, userId)),
            HttpMethod.DELETE,
            new HttpEntity<String>(null, headers),
            String.class);

    val deleteResponseStatus = deleteResponse.getStatusCode();
    assertThat(deleteResponseStatus).isEqualTo(HttpStatus.OK);

    val getResponse =
        restTemplate.exchange(
            createURLWithPort(String.format("/policies/%s/users", policyId)),
            HttpMethod.GET,
            new HttpEntity<String>(null, headers),
            String.class);

    val getResponseStatus = getResponse.getStatusCode();
    val getResponseJson = (ArrayNode) MAPPER.readTree(getResponse.getBody());

    assertThat(getResponseStatus).isEqualTo(HttpStatus.OK);
    assertThat(getResponseJson.size()).isEqualTo(0);
  }

  private String createURLWithPort(String uri) {
    return "http://localhost:" + port + uri;
  }
}
