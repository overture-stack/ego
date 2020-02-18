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

import static bio.overture.ego.controller.AbstractPermissionControllerTest.createMaskJson;
import static bio.overture.ego.model.enums.AccessLevel.READ;
import static bio.overture.ego.model.enums.AccessLevel.WRITE;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.OK;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.PolicyRequest;
import bio.overture.ego.service.PolicyService;
import bio.overture.ego.utils.EntityGenerator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PolicyControllerTest extends AbstractControllerTest {

  private static boolean hasRunEntitySetup = false;

  /** Dependencies */
  @Autowired private EntityGenerator entityGenerator;

  @Autowired private PolicyService policyService;

  @Value("${logging.test.controller.enable}")
  private boolean enableLogging;

  @Override
  protected boolean enableLogging() {
    return enableLogging;
  }

  @Override
  protected void beforeTest() {
    // Initial setup of entities (run once
    if (!hasRunEntitySetup) {
      entityGenerator.setupTestUsers();
      entityGenerator.setupTestGroups();
      entityGenerator.setupTestPolicies();
      hasRunEntitySetup = true;
    }
  }

  @Test
  @SneakyThrows
  public void addpolicy_Success() {
    val policy = PolicyRequest.builder().name("AddPolicy").build();

    val response = initStringRequest().endpoint("/policies").body(policy).post();

    val responseStatus = response.getStatusCode();
    assertEquals(responseStatus, OK);
    val responseJson = MAPPER.readTree(response.getBody());

    log.info(response.getBody());

    assertEquals(responseJson.get("name").asText(), "AddPolicy");
  }

  @Test
  @SneakyThrows
  public void addDuplicatePolicy_Conflict() {
    val policy1 = PolicyRequest.builder().name("PolicyUnique").build();
    val policy2 = PolicyRequest.builder().name("PolicyUnique").build();

    val response1 = initStringRequest().endpoint("/policies").body(policy1).post();

    val responseStatus1 = response1.getStatusCode();
    assertEquals(responseStatus1, OK);

    val response2 = initStringRequest().endpoint("/policies").body(policy2).post();

    val responseStatus2 = response2.getStatusCode();
    assertEquals(responseStatus2, CONFLICT);
  }

  @Test
  @SneakyThrows
  public void getPolicy_Success() {
    val policyId = policyService.getByName("Study001").getId();
    val response = initStringRequest().endpoint("/policies/%s", policyId).get();

    val responseStatus = response.getStatusCode();
    val responseJson = MAPPER.readTree(response.getBody());

    assertEquals(responseStatus, OK);
    assertEquals(responseJson.get("name").asText(), "Study001");
  }

  @Test
  @SneakyThrows
  public void associatePermissionsWithGroup_ExistingEntitiesButNonExistingRelationship_Success() {
    val policyId = entityGenerator.setupSinglePolicy("AddGroupPermission").getId().toString();
    val groupId = entityGenerator.setupGroup("GroupPolicyAdd").getId().toString();

    val response =
        initStringRequest()
            .endpoint("/policies/%s/permission/group/%s", policyId, groupId)
            .body(createMaskJson(WRITE.toString()))
            .post();

    val responseStatus = response.getStatusCode();
    assertEquals(responseStatus, OK);

    val getResponse = initStringRequest().endpoint("/policies/%s/groups", policyId).get();

    val getResponseStatus = getResponse.getStatusCode();
    val getResponseJson = MAPPER.readTree(getResponse.getBody());
    val groupPermissionJson = getResponseJson.get("resultSet").get(0);

    assertEquals(getResponseStatus, OK);
    assertEquals(groupPermissionJson.get("id").asText(), groupId);
    assertEquals(groupPermissionJson.get("mask").asText(), "WRITE");
  }

  @Test
  @SneakyThrows
  public void disassociatePermissionsFromGroup_EntitiesAndRelationshipsExisting_Success() {
    val policyId = entityGenerator.setupSinglePolicy("DeleteGroupPermission").getId().toString();
    val groupId = entityGenerator.setupGroup("GroupPolicyDelete").getId().toString();

    val response =
        initStringRequest()
            .endpoint("/policies/%s/permission/group/%s", policyId, groupId)
            .body(createMaskJson(WRITE.toString()))
            .post();

    val responseStatus = response.getStatusCode();
    assertEquals(responseStatus, OK);

    val deleteResponse =
        initStringRequest()
            .endpoint("/policies/%s/permission/group/%s", policyId, groupId)
            .delete();

    val deleteResponseStatus = deleteResponse.getStatusCode();
    assertEquals(deleteResponseStatus, OK);

    val getResponse = initStringRequest().endpoint("/policies/%s/groups", policyId).get();

    val getResponseStatus = getResponse.getStatusCode();
    val getResponseJson = MAPPER.readTree(getResponse.getBody());

    assertEquals(getResponseStatus, OK);
    assertEquals(getResponseJson.get("resultSet").size(), 0);
  }

  @Test
  @SneakyThrows
  public void associatePermissionsWithUser_ExistingEntitiesButNoRelationship_Success() {
    val policyId = entityGenerator.setupSinglePolicy("AddUserPermission").getId().toString();
    val userId = entityGenerator.setupUser("UserPolicy Add").getId().toString();

    val response =
        initStringRequest()
            .endpoint("/policies/%s/permission/user/%s", policyId, userId)
            .body(createMaskJson(READ.toString()))
            .post();

    val responseStatus = response.getStatusCode();
    assertEquals(responseStatus, OK);
    // TODO: Fix it so that POST returns JSON, not just random string message

    val getResponse = initStringRequest().endpoint("/policies/%s/users", policyId).get();

    val getResponseStatus = getResponse.getStatusCode();
    val getResponseJson = MAPPER.readTree(getResponse.getBody());
    val groupPermissionJson = getResponseJson.get("resultSet").get(0);

    assertEquals(getResponseStatus, OK);
    assertEquals(groupPermissionJson.get("id").asText(), userId);
    assertEquals(groupPermissionJson.get("mask").asText(), "READ");
  }

  @Test
  @SneakyThrows
  public void disassociatePermissionsFromUser_ExistingEntitiesAndRelationships_Success() {
    val policyId = entityGenerator.setupSinglePolicy("DeleteGroupPermission").getId().toString();
    val userId = entityGenerator.setupUser("UserPolicy Delete").getId().toString();

    val response =
        initStringRequest()
            .endpoint("/policies/%s/permission/user/%s", policyId, userId)
            .body(createMaskJson(WRITE.toString()))
            .post();

    val responseStatus = response.getStatusCode();
    assertEquals(responseStatus, OK);
    // TODO: Fix it so that POST returns JSON, not just random string message

    val deleteResponse =
        initStringRequest().endpoint("/policies/%s/permission/user/%s", policyId, userId).delete();

    val deleteResponseStatus = deleteResponse.getStatusCode();
    assertEquals(deleteResponseStatus, OK);

    val getResponse = initStringRequest().endpoint("/policies/%s/users", policyId).get();

    val getResponseStatus = getResponse.getStatusCode();
    val getResponseJson = MAPPER.readTree(getResponse.getBody());

    assertEquals(getResponseStatus, OK);
    assertEquals(0, getResponseJson.get("count").asInt());
  }
}
