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
import static org.assertj.core.api.Assertions.assertThat;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.service.PolicyService;
import bio.overture.ego.utils.EntityGenerator;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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
public class PolicyControllerTest extends AbstractControllerTest {

  private static boolean hasRunEntitySetup = false;

  /** Dependencies */
  @Autowired private EntityGenerator entityGenerator;

  @Autowired private PolicyService policyService;

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
    val policy = Policy.builder().name("AddPolicy").build();

    val response = initStringRequest().endpoint("/policies").body(policy).post();

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    val responseJson = MAPPER.readTree(response.getBody());

    log.info(response.getBody());

    assertThat(responseJson.get("name").asText()).isEqualTo("AddPolicy");
  }

  @Test
  @SneakyThrows
  public void addDuplicatePolicy_Conflict() {
    val policy1 = Policy.builder().name("PolicyUnique").build();
    val policy2 = Policy.builder().name("PolicyUnique").build();

    val response1 = initStringRequest().endpoint("/policies").body(policy1).post();

    val responseStatus1 = response1.getStatusCode();
    assertThat(responseStatus1).isEqualTo(HttpStatus.OK);

    val response2 = initStringRequest().endpoint("/policies").body(policy2).post();

    val responseStatus2 = response2.getStatusCode();
    assertThat(responseStatus2).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  @SneakyThrows
  public void getPolicy_Success() {
    val policyId = policyService.getByName("Study001").getId();
    val response = initStringRequest().endpoint("/policies/%s", policyId).get();

    val responseStatus = response.getStatusCode();
    val responseJson = MAPPER.readTree(response.getBody());

    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    assertThat(responseJson.get("name").asText()).isEqualTo("Study001");
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
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);

    val getResponse = initStringRequest().endpoint("/policies/%s/groups", policyId).get();

    val getResponseStatus = getResponse.getStatusCode();
    val getResponseJson = MAPPER.readTree(getResponse.getBody());
    val groupPermissionJson = getResponseJson.get(0);

    assertThat(getResponseStatus).isEqualTo(HttpStatus.OK);
    assertThat(groupPermissionJson.get("id").asText()).isEqualTo(groupId);
    assertThat(groupPermissionJson.get("mask").asText()).isEqualTo("WRITE");
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
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);

    val deleteResponse =
        initStringRequest()
            .endpoint("/policies/%s/permission/group/%s", policyId, groupId)
            .delete();

    val deleteResponseStatus = deleteResponse.getStatusCode();
    assertThat(deleteResponseStatus).isEqualTo(HttpStatus.OK);

    val getResponse = initStringRequest().endpoint("/policies/%s/groups", policyId).get();

    val getResponseStatus = getResponse.getStatusCode();
    val getResponseJson = (ArrayNode) MAPPER.readTree(getResponse.getBody());

    assertThat(getResponseStatus).isEqualTo(HttpStatus.OK);
    assertThat(getResponseJson.size()).isEqualTo(0);
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
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    // TODO: Fix it so that POST returns JSON, not just random string message

    val getResponse = initStringRequest().endpoint("/policies/%s/users", policyId).get();

    val getResponseStatus = getResponse.getStatusCode();
    val getResponseJson = MAPPER.readTree(getResponse.getBody());
    val groupPermissionJson = getResponseJson.get(0);

    assertThat(getResponseStatus).isEqualTo(HttpStatus.OK);
    assertThat(groupPermissionJson.get("id").asText()).isEqualTo(userId);
    assertThat(groupPermissionJson.get("mask").asText()).isEqualTo("READ");
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
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    // TODO: Fix it so that POST returns JSON, not just random string message

    val deleteResponse =
        initStringRequest().endpoint("/policies/%s/permission/user/%s", policyId, userId).delete();

    val deleteResponseStatus = deleteResponse.getStatusCode();
    assertThat(deleteResponseStatus).isEqualTo(HttpStatus.OK);

    val getResponse = initStringRequest().endpoint("/policies/%s/users", policyId).get();

    val getResponseStatus = getResponse.getStatusCode();
    val getResponseJson = (ArrayNode) MAPPER.readTree(getResponse.getBody());

    assertThat(getResponseStatus).isEqualTo(HttpStatus.OK);
    assertThat(getResponseJson.size()).isEqualTo(0);
  }
}
