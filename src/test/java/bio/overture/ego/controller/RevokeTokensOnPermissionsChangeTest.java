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

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.enums.ApplicationType;
import bio.overture.ego.utils.EntityGenerator;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableList;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
  classes = AuthorizationServiceMain.class,
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RevokeTokensOnPermissionsChangeTest extends AbstractControllerTest {

  private boolean hasRunEntitySetup = false;

  /**
   * Dependencies
   */
  @Autowired
  private EntityGenerator entityGenerator;

  private HttpHeaders tokenHeaders = new HttpHeaders();

  @Override
  protected void beforeTest() {
    entityGenerator.setupApplication("tokenClient", "tokenSecret", ApplicationType.ADMIN);
    tokenHeaders.add(AUTHORIZATION, "Basic dG9rZW5DbGllbnQ6dG9rZW5TZWNyZXQ=");
    tokenHeaders.setContentType(APPLICATION_JSON);

    hasRunEntitySetup = true;
  }

  /**
   * Scenario: Delete a userpermission for a user who as an active access token using a scope from that permission.
   * Expected Behavior: Token should be revoked.
   */
  @Test
  @SneakyThrows
  public void deletePermissionFromUser_ExistingToken_RevokeSuccess() {
    val user = entityGenerator.setupUser("UserFoo DeletePermission");
    val policy = entityGenerator.setupSinglePolicy("PolicyForSingleUserDeletePermission");

    val permissionRequest = ImmutableList.of(PermissionRequest.builder().policyId(policy.getId()).mask(AccessLevel.WRITE).build());

    val createPermissionResponse = initStringRequest()
      .endpoint("/users/%s/permissions", user.getId().toString())
      .body(permissionRequest).post();

    val statusCode = createPermissionResponse.getStatusCode();
    assertThat(statusCode).isEqualTo(HttpStatus.OK);

    val createTokenResponse = initStringRequest()
      .endpoint("/o/token?user_id=%s&scopes=%s", user.getId().toString(), policy.getName() + ".WRITE")
      .post();

    val tokenStatusCode = createTokenResponse.getStatusCode();
    assertThat(tokenStatusCode).isEqualTo(HttpStatus.OK);
    val tokenResponseJson = MAPPER.readTree(createTokenResponse.getBody());
    val accessToken = tokenResponseJson.get("accessToken").asText();

    log.info(accessToken);

    val checkTokenResponse = initStringRequest(tokenHeaders)
      .endpoint("/o/check_token?token=%s", accessToken).post();

    val checkStatusCode = checkTokenResponse.getStatusCode();
    assertThat(checkStatusCode).isEqualTo(HttpStatus.MULTI_STATUS);
    assertThat(checkTokenResponse.getBody()).contains(policy.getName() + ".WRITE");
    assertThat(checkTokenResponse.getBody()).contains(policy.getName() + ".READ");

    val getPermissionsResponse = initStringRequest().endpoint("/users/%s/permissions", user.getId()).get();
    val permissionJson = MAPPER.readTree(getPermissionsResponse.getBody());
    val results = (ArrayNode) permissionJson.get("resultSet");
    val permissionId = results.elements().next().get("id").asText();


    val deletePermissionResponse = initStringRequest().endpoint("/users/%s/permissions/%s", user.getId(), permissionId).delete();
    val deleteStatusCode = deletePermissionResponse.getStatusCode();
    assertThat(deleteStatusCode).isEqualTo(HttpStatus.OK);

    val checkTokenAfterDeleteResponse = initStringRequest(tokenHeaders)
      .endpoint("/o/check_token?token=%s", accessToken).post();
    assertThat(checkTokenAfterDeleteResponse.getStatusCode()).isNotEqualTo(HttpStatus.OK);
  }

  /**
   * Scenario: Upgrade a user permission from READ to WRITE. The user had a token using READ before the upgrade.
   * Expected Behavior: Token should be remain active.
   */
  @Test
  @SneakyThrows
  public void upgradePermissionFromUser_ExistingToken_KeepTokenSuccess() {
    val user = entityGenerator.setupUser("UserFoo UpgradePermission");
    val policy = entityGenerator.setupSinglePolicy("PolicyForSingleUserUpgradePermission");

    val permissionRequest = ImmutableList.of(PermissionRequest.builder().policyId(policy.getId()).mask(AccessLevel.READ).build());
    initStringRequest()
      .endpoint("/users/%s/permissions", user.getId().toString())
      .body(permissionRequest).post();

    val createTokenResponse = initStringRequest()
      .endpoint("/o/token?user_id=%s&scopes=%s", user.getId().toString(), policy.getName() + ".READ")
      .post();

    val tokenResponseJson = MAPPER.readTree(createTokenResponse.getBody());
    val accessToken = tokenResponseJson.get("accessToken").asText();

    val checkTokenResponse = initStringRequest(tokenHeaders)
      .endpoint("/o/check_token?token=%s", accessToken).post();

    val checkStatusCode = checkTokenResponse.getStatusCode();
    assertThat(checkStatusCode).isEqualTo(HttpStatus.MULTI_STATUS);
    assertThat(checkTokenResponse.getBody()).contains(policy.getName() + ".READ");

    val permissionUpgradeRequest = ImmutableList.of(PermissionRequest.builder().policyId(policy.getId()).mask(AccessLevel.WRITE).build());
    val upgradeResponse = initStringRequest()
      .endpoint("/users/%s/permissions", user.getId().toString())
      .body(permissionUpgradeRequest).post();

    val upgradeStatusCode = upgradeResponse.getStatusCode();
    assertThat(upgradeStatusCode).isEqualTo(HttpStatus.OK);

    val checkTokenAfterUpgradeResponse = initStringRequest(tokenHeaders)
      .endpoint("/o/check_token?token=%s", accessToken).post();
    val statusCode = checkTokenAfterUpgradeResponse.getStatusCode();
    assertThat(statusCode).isEqualTo(HttpStatus.MULTI_STATUS);
    log.info(checkTokenAfterUpgradeResponse.getBody());
  }

  /**
   * Scenario: Downgrade a user permission from WRITE to READ. The user had a token using WRITE before the upgrade.
   * Expected Behavior: Token should be revoked.
   */
  @Test
  @SneakyThrows
  public void downgradePermissionFromUser_ExistingToken_RevokeTokenSuccess() {
    val user = entityGenerator.setupUser("UserFoo UpgradePermission");
    val policy = entityGenerator.setupSinglePolicy("PolicyForSingleUserUpgradePermission");

    val permissionRequest = ImmutableList.of(PermissionRequest.builder().policyId(policy.getId()).mask(AccessLevel.WRITE).build());
    initStringRequest()
      .endpoint("/users/%s/permissions", user.getId().toString())
      .body(permissionRequest).post();

    val createTokenResponse = initStringRequest()
      .endpoint("/o/token?user_id=%s&scopes=%s", user.getId().toString(), policy.getName() + ".READ")
      .post();

    val tokenResponseJson = MAPPER.readTree(createTokenResponse.getBody());
    val accessToken = tokenResponseJson.get("accessToken").asText();

    val checkTokenResponse = initStringRequest(tokenHeaders)
      .endpoint("/o/check_token?token=%s", accessToken).post();

    val checkStatusCode = checkTokenResponse.getStatusCode();
    assertThat(checkStatusCode).isEqualTo(HttpStatus.MULTI_STATUS);
    assertThat(checkTokenResponse.getBody()).contains(policy.getName() + ".READ");

    val permissionUpgradeRequest = ImmutableList.of(PermissionRequest.builder().policyId(policy.getId()).mask(AccessLevel.WRITE).build());
    val upgradeResponse = initStringRequest()
      .endpoint("/users/%s/permissions", user.getId().toString())
      .body(permissionUpgradeRequest).post();

    val upgradeStatusCode = upgradeResponse.getStatusCode();
    assertThat(upgradeStatusCode).isEqualTo(HttpStatus.OK);

    val checkTokenAfterUpgradeResponse = initStringRequest(tokenHeaders)
      .endpoint("/o/check_token?token=%s", accessToken).post();
    val statusCode = checkTokenAfterUpgradeResponse.getStatusCode();
    assertThat(statusCode).isEqualTo(HttpStatus.MULTI_STATUS);
    log.info(checkTokenAfterUpgradeResponse.getBody());
  }

}
