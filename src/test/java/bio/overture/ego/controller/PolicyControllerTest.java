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
import static org.springframework.http.HttpStatus.*;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.PolicyRequest;
import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.service.PolicyService;
import bio.overture.ego.utils.EntityGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    // Initial setup of entities (run once)
    if (!hasRunEntitySetup) {
      entityGenerator.setupTestUsers();
      entityGenerator.setupTestGroups();
      entityGenerator.setupTestPolicies();
      hasRunEntitySetup = true;
    }
  }

  @Test
  @SneakyThrows
  public void addPolicy_Success() {
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
  public void addPolicy_invalidCharacter_badRequest() {
    val policy = PolicyRequest.builder().name("AddPolicy!").build();

    val response = initStringRequest().endpoint("/policies").body(policy).post();

    val responseStatus = response.getStatusCode();
    assertEquals(responseStatus, BAD_REQUEST);
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

  public void associatePermissionsWithEntity(Identifiable entity, String entityName) {
    val policyName = String.format("AddPermission_%s", entityName);
    val policyId = entityGenerator.setupSinglePolicy(policyName).getId().toString();
    val entityId = entity.getId().toString();

    val response =
        initStringRequest()
            .endpoint("/policies/%s/permission/%s/%s", policyId, entityName, entityId)
            .body(createMaskJson(READ.toString()))
            .post();

    val responseStatus = response.getStatusCode();
    assertEquals(responseStatus, OK);
    // TODO: Fix it so that POST returns JSON, not just random string message

    val getResponse = initStringRequest().endpoint("/policies/%s/%ss", policyId, entityName).get();

    val getResponseStatus = getResponse.getStatusCode();
    try {
      val getResponseJson = MAPPER.readTree(getResponse.getBody());
      val permissionJson = getResponseJson.get("resultSet").get(0);

      assertEquals(getResponseStatus, OK);
      assertEquals(permissionJson.get("id").asText(), entityId);
      assertEquals(permissionJson.get("mask").asText(), "READ");
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

  public void disassociatePermissionsFromEntity(Identifiable entity, String entityName) {
    val policyName = String.format("DeletePermission_%s", entityName);
    val policyId = entityGenerator.setupSinglePolicy(policyName).getId().toString();
    val entityId = entity.getId().toString();

    val response =
        initStringRequest()
            .endpoint("/policies/%s/permission/%s/%s", policyId, entityName, entityId)
            .body(createMaskJson(WRITE.toString()))
            .post();

    val responseStatus = response.getStatusCode();
    assertEquals(responseStatus, OK);
    // TODO: Fix it so that POST returns JSON, not just random string message

    val deleteResponse =
        initStringRequest()
            .endpoint("/policies/%s/permission/%s/%s", policyId, entityName, entityId)
            .delete();

    val deleteResponseStatus = deleteResponse.getStatusCode();
    assertEquals(deleteResponseStatus, OK);

    val getResponse = initStringRequest().endpoint("/policies/%s/%ss", policyId, entityName).get();

    val getResponseStatus = getResponse.getStatusCode();
    try {
      val getResponseJson = MAPPER.readTree(getResponse.getBody());
      assertEquals(getResponseStatus, OK);
      assertEquals(0, getResponseJson.get("count").asInt());
    } catch (JsonProcessingException e) {
      e.printStackTrace();
    }
  }

  @Test
  @SneakyThrows
  public void associatePermissionsWithGroup_ExistingEntitiesButNonExistingRelationship_Success() {
    val testGroup = entityGenerator.setupGroup("GroupPolicy Add");
    associatePermissionsWithEntity(testGroup, "group");
  }

  @Test
  @SneakyThrows
  public void disassociatePermissionsFromGroup_EntitiesAndRelationshipsExisting_Success() {
    val testGroup = entityGenerator.setupGroup("GroupPolicyDelete");
    disassociatePermissionsFromEntity(testGroup, "group");
  }

  @Test
  @SneakyThrows
  public void associatePermissionsWithUser_ExistingEntitiesButNoRelationship_Success() {
    val testUser = entityGenerator.setupUser("UserPolicy Add");
    associatePermissionsWithEntity(testUser, "user");
  }

  @Test
  @SneakyThrows
  public void disassociatePermissionsFromUser_ExistingEntitiesAndRelationships_Success() {
    val testUser = entityGenerator.setupUser("UserPolicy Delete");
    disassociatePermissionsFromEntity(testUser, "user");
  }

  @Test
  @SneakyThrows
  public void associatePermissionsWithApplication_ExistingEntitiesButNoRelationship_Success() {
    val testApp = entityGenerator.setupApplication("AppPolicyAdd");
    associatePermissionsWithEntity(testApp, "application");
  }

  @Test
  @SneakyThrows
  public void disassociatePermissionsFromApplication_ExistingEntitiesAndRelationships_Success() {
    val testApp = entityGenerator.setupApplication("AppPolicyDelete");
    disassociatePermissionsFromEntity(testApp, "application");
  }
}
