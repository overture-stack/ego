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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.enums.ApplicationType;
import bio.overture.ego.utils.EntityGenerator;
import com.google.common.collect.ImmutableList;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TokensOnUserAndPolicyDeletes extends AbstractControllerTest {

  /** Config */
  @Value("${logging.test.controller.enable}")
  private boolean enableLogging;

  /** Dependencies */
  @Autowired private EntityGenerator entityGenerator;

  private HttpHeaders tokenHeaders = new HttpHeaders();

  @Override
  protected void beforeTest() {
    entityGenerator.setupApplication("tokenClient", "tokenSecret", ApplicationType.ADMIN);
    tokenHeaders.add(AUTHORIZATION, "Basic dG9rZW5DbGllbnQ6dG9rZW5TZWNyZXQ=");
    tokenHeaders.setContentType(APPLICATION_JSON);
  }

  @Override
  protected boolean enableLogging() {
    return enableLogging;
  }

  /**
   * Scenario: Two users with tokens that have a scope on a policy. Delete one user. Expected
   * Behavior: Deleted user shold also have tokens deleted, other user's tokens should remain valid.
   */
  @Test
  public void deleteUser_ExistingTokens_TokensDeletedSuccess() {
    val userDelete = entityGenerator.setupUser("UserTokens DeleteUser");
    val userKeep = entityGenerator.setupUser("UserTokens DontDeleteUser");
    val policy = entityGenerator.setupSinglePolicy("PolicyForUserDeleteTest");

    val tokenToDelete = setupUserWithToken(userDelete, policy);
    val tokenToKeep = setupUserWithToken(userKeep, policy);

    val deleteUserResponse = initStringRequest().endpoint("/users/%s", userDelete.getId()).delete();

    val deleteStatusCode = deleteUserResponse.getStatusCode();
    assertEquals(deleteStatusCode, HttpStatus.OK);

    val checkTokenAfterDeleteResponse = checkToken(tokenToDelete);
    // Should be revoked
    assertEquals(checkTokenAfterDeleteResponse.getStatusCode(), HttpStatus.UNAUTHORIZED);

    val checkTokenRemainedAfterDeleteResponse = checkToken(tokenToKeep);
    // Should be valid
    assertEquals(checkTokenRemainedAfterDeleteResponse.getStatusCode(), HttpStatus.MULTI_STATUS);
  }

  /**
   * Scenario: User1 has token for policy1. User2 has token for policy2. Delete policy1. Expected
   * Behavior: User1 should have token deleted, user2's token should remain valid.
   */
  @Test
  public void deletePolicy_ExistingTokens_TokensDeletedSuccess() {
    val user1 = entityGenerator.setupUser("UserTokens ForDeletedPolicy");
    val user2 = entityGenerator.setupUser("UserTokens ForKeptPolicy");
    val policy1 = entityGenerator.setupSinglePolicy("PolicyToBeDeletedForTokens");
    val policy2 = entityGenerator.setupSinglePolicy("PolicyToBeKeptForTokens");

    val tokenToDelete = setupUserWithToken(user1, policy1);
    val tokenToKeep = setupUserWithToken(user2, policy2);

    val deletePolicyResponse =
        initStringRequest().endpoint("/policies/%s", policy1.getId()).delete();
    val deleteStatusCode = deletePolicyResponse.getStatusCode();
    assertEquals(deleteStatusCode, HttpStatus.OK);

    val checkTokenAfterDeleteResponse = checkToken(tokenToDelete);
    // Should be revoked
    assertEquals(checkTokenAfterDeleteResponse.getStatusCode(), HttpStatus.UNAUTHORIZED);

    val checkTokenRemainedAfterDeleteResponse = checkToken(tokenToKeep);
    // Should be valid
    assertEquals(checkTokenRemainedAfterDeleteResponse.getStatusCode(), HttpStatus.MULTI_STATUS);
  }

  /**
   * This helper method is responsible for executing the pre-conditions of the scenario for user
   * permission mutations.
   *
   * @param user User that will have heir permissions mutated.
   * @param policy Policy that the permissions will be against.
   * @return The access token for the user.
   */
  @SneakyThrows
  private String setupUserWithToken(User user, Policy policy) {
    val permissionRequest =
        ImmutableList.of(
            PermissionRequest.builder().policyId(policy.getId()).mask(AccessLevel.WRITE).build());
    initStringRequest()
        .endpoint("/users/%s/permissions", user.getId().toString())
        .body(permissionRequest)
        .post();

    // TODO: [anncatton] update endpoint to "/o/api_key"
    val createTokenResponse =
        initStringRequest()
            .endpoint(
                "/o/token?user_id=%s&scopes=%s",
                user.getId().toString(), policy.getName() + "." + "WRITE")
            .post();

    val tokenResponseJson = MAPPER.readTree(createTokenResponse.getBody());
    val accessToken = tokenResponseJson.get("accessToken").asText();

    val checkTokenResponse = checkToken(accessToken);

    val checkStatusCode = checkTokenResponse.getStatusCode();
    assertEquals(checkStatusCode, HttpStatus.MULTI_STATUS);
    assertTrue(checkTokenResponse.getBody().contains(policy.getName() + "." + "WRITE"));

    return accessToken;
  }

  private ResponseEntity<String> checkToken(String token) {
    return initStringRequest(tokenHeaders).endpoint("/o/check_token?token=%s", token).post();
  }
}
