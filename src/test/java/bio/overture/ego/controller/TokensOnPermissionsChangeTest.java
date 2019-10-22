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
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TokensOnPermissionsChangeTest extends AbstractControllerTest {

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
   * Scenario: Delete a user permission for a user who as an active access token using a scope from
   * that permission. Expected Behavior: Token should be revoked.
   */
  @Test
  @SneakyThrows
  public void deletePermissionFromUser_ExistingToken_RevokeSuccess() {
    val user = entityGenerator.setupUser("UserFoo DeletePermission");
    val policy = entityGenerator.setupSinglePolicy("PolicyForSingleUserDeletePermission");
    val accessToken = userPermissionTestSetup(user, policy, AccessLevel.WRITE, "WRITE");

    val getPermissionsResponse =
        initStringRequest().endpoint("/users/%s/permissions", user.getId()).get();
    val permissionJson = MAPPER.readTree(getPermissionsResponse.getBody());
    val results = (ArrayNode) permissionJson.get("resultSet");
    val permissionId = results.elements().next().get("id").asText();

    val deletePermissionResponse =
        initStringRequest()
            .endpoint("/users/%s/permissions/%s", user.getId(), permissionId)
            .delete();
    val deleteStatusCode = deletePermissionResponse.getStatusCode();
    assertEquals(deleteStatusCode, HttpStatus.OK);

    val checkTokenAfterDeleteResponse =
        initStringRequest(tokenHeaders).endpoint("/o/check_api_key?token=%s", accessToken).post();

    // Should be revoked
    assertEquals(checkTokenAfterDeleteResponse.getStatusCode(), HttpStatus.UNAUTHORIZED);
  }

  /**
   * Scenario: Upgrade a user permission from READ to WRITE. The user had a token using READ before
   * the upgrade. Expected Behavior: Token should be remain active.
   */
  @Test
  @SneakyThrows
  public void upgradePermissionFromUser_ExistingToken_KeepTokenSuccess() {
    val user = entityGenerator.setupUser("UserFoo UpgradePermission");
    val policy = entityGenerator.setupSinglePolicy("PolicyForSingleUserUpgradePermission");
    val accessToken = userPermissionTestSetup(user, policy, AccessLevel.READ, "READ");

    val permissionUpgradeRequest =
        ImmutableList.of(
            PermissionRequest.builder().policyId(policy.getId()).mask(AccessLevel.WRITE).build());
    val upgradeResponse =
        initStringRequest()
            .endpoint("/users/%s/permissions", user.getId().toString())
            .body(permissionUpgradeRequest)
            .post();

    val upgradeStatusCode = upgradeResponse.getStatusCode();
    assertEquals(upgradeStatusCode, HttpStatus.OK);

    val checkTokenAfterUpgradeResponse =
        initStringRequest(tokenHeaders).endpoint("/o/check_api_key?token=%s", accessToken).post();
    val statusCode = checkTokenAfterUpgradeResponse.getStatusCode();

    // Should be valid
    assertEquals(statusCode, HttpStatus.MULTI_STATUS);
  }

  /**
   * Scenario: Downgrade a user permission from WRITE to READ. The user had a token using WRITE
   * before the upgrade. Expected Behavior: Token should be revoked.
   */
  @Test
  @SneakyThrows
  public void downgradePermissionFromUser_ExistingToken_RevokeTokenSuccess() {
    val user = entityGenerator.setupUser("UserFoo DowngradePermission");
    val policy = entityGenerator.setupSinglePolicy("PolicyForSingleUserDowngradePermission");
    val accessToken = userPermissionTestSetup(user, policy, AccessLevel.WRITE, "WRITE");

    val permissionDowngradeRequest =
        ImmutableList.of(
            PermissionRequest.builder().policyId(policy.getId()).mask(AccessLevel.READ).build());
    val upgradeResponse =
        initStringRequest()
            .endpoint("/users/%s/permissions", user.getId().toString())
            .body(permissionDowngradeRequest)
            .post();

    val downgradeStatusCode = upgradeResponse.getStatusCode();
    assertEquals(downgradeStatusCode, HttpStatus.OK);

    val checkTokenAfterUpgradeResponse =
        initStringRequest(tokenHeaders).endpoint("/o/check_api_key?token=%s", accessToken).post();
    val statusCode = checkTokenAfterUpgradeResponse.getStatusCode();

    // Should be revoked
    assertEquals(statusCode, HttpStatus.UNAUTHORIZED);
  }

  /**
   * Scenario: DENY a user on a policy. The user had a token using WRITE before the DENY. Expected
   * Behavior: Token should be revoked.
   */
  @Test
  @SneakyThrows
  public void denyPermissionFromUser_ExistingToken_RevokeTokenSuccess() {
    val user = entityGenerator.setupUser("UserFoo DenyPermission");
    val policy = entityGenerator.setupSinglePolicy("song.abc");
    val accessToken = userPermissionTestSetup(user, policy, AccessLevel.WRITE, "WRITE");

    val permissionDenyRequest =
        ImmutableList.of(
            PermissionRequest.builder().policyId(policy.getId()).mask(AccessLevel.DENY).build());
    val upgradeResponse =
        initStringRequest()
            .endpoint("/users/%s/permissions", user.getId().toString())
            .body(permissionDenyRequest)
            .post();

    val denyStatusCode = upgradeResponse.getStatusCode();
    assertEquals(denyStatusCode, HttpStatus.OK);

    val checkTokenAfterUpgradeResponse =
        initStringRequest(tokenHeaders).endpoint("/o/check_api_key?token=%s", accessToken).post();
    val statusCode = checkTokenAfterUpgradeResponse.getStatusCode();

    // Should be revoked
    assertEquals(statusCode, HttpStatus.UNAUTHORIZED);
  }

  /**
   * Scenario: User is part of a group that has a WRITE permission. User has a token using this
   * scope. The group then has the permission deleted. Behavior: Token should be revoked.
   */
  @Test
  @SneakyThrows
  public void deleteGroupPermission_ExistingToken_RevokeTokenSuccess() {
    val user = entityGenerator.setupUser("UserFoo DeleteGroupPermission");
    val group = entityGenerator.setupGroup("DeleteGroupPermission");
    val policy = entityGenerator.setupSinglePolicy("PolicyForGroupDeletePermission");

    val accessToken = groupPermissionTestSetup(user, group, policy, AccessLevel.WRITE, "READ");

    val getPermissionsResponse =
        initStringRequest().endpoint("/groups/%s/permissions", group.getId()).get();
    val permissionJson = MAPPER.readTree(getPermissionsResponse.getBody());
    val results = (ArrayNode) permissionJson.get("resultSet");
    val permissionId = results.elements().next().get("id").asText();

    val deletePermissionResponse =
        initStringRequest()
            .endpoint("/groups/%s/permissions/%s", group.getId(), permissionId)
            .delete();
    assertEquals(deletePermissionResponse.getStatusCode(), HttpStatus.OK);

    val checkTokenAfterDeleteResponse =
        initStringRequest(tokenHeaders).endpoint("/o/check_api_key?token=%s", accessToken).post();

    // Should be revoked
    assertEquals(checkTokenAfterDeleteResponse.getStatusCode(), HttpStatus.UNAUTHORIZED);
  }

  /**
   * Scenario: User is part of a group that has a READ permission. User has a token using this
   * scope. The group then has the permission upgraded to WRITE. Behavior: Token should remain
   * valid.
   */
  @Test
  @SneakyThrows
  public void upgradeGroupPermission_ExistingToken_KeepTokenSuccess() {
    val user = entityGenerator.setupUser("UserFoo UpgradeGroupPermission");
    val group = entityGenerator.setupGroup("UpgradeGroupPermission");
    val policy = entityGenerator.setupSinglePolicy("PolicyForGroupUpgradePermission");

    val accessToken = groupPermissionTestSetup(user, group, policy, AccessLevel.READ, "READ");

    val permissionUpgradeRequest =
        ImmutableList.of(
            PermissionRequest.builder().policyId(policy.getId()).mask(AccessLevel.WRITE).build());
    val upgradeResponse =
        initStringRequest()
            .endpoint("/groups/%s/permissions", group.getId().toString())
            .body(permissionUpgradeRequest)
            .post();
    assertEquals(upgradeResponse.getStatusCode(), HttpStatus.OK);

    val checkTokenAfterUpgradeResponse =
        initStringRequest(tokenHeaders).endpoint("/o/check_api_key?token=%s", accessToken).post();

    // Should be valid
    assertEquals(checkTokenAfterUpgradeResponse.getStatusCode(), HttpStatus.MULTI_STATUS);
  }

  /**
   * Scenario: User is part of a group that has a WRITE permission. User has a token using this
   * scope. The group then has the permission downgraded to READ. Behavior: Token should be revoked.
   */
  @Test
  @SneakyThrows
  public void downgradeGroupPermission_ExistingToken_RevokeTokenSuccess() {
    val user = entityGenerator.setupUser("UserFoo DowngradeGroupPermission");
    val group = entityGenerator.setupGroup("DowngradeGroupPermission");
    val policy = entityGenerator.setupSinglePolicy("PolicyForGroupDowngradePermission");

    val accessToken = groupPermissionTestSetup(user, group, policy, AccessLevel.WRITE, "WRITE");

    val permissionDowngradeRequest =
        ImmutableList.of(
            PermissionRequest.builder().policyId(policy.getId()).mask(AccessLevel.READ).build());
    val downgradeResponse =
        initStringRequest()
            .endpoint("/groups/%s/permissions", group.getId().toString())
            .body(permissionDowngradeRequest)
            .post();
    assertEquals(downgradeResponse.getStatusCode(), HttpStatus.OK);

    val checkTokenAfterUpgradeResponse =
        initStringRequest(tokenHeaders).endpoint("/o/check_api_key?token=%s", accessToken).post();

    // Should be revoked
    assertEquals(checkTokenAfterUpgradeResponse.getStatusCode(), HttpStatus.UNAUTHORIZED);
  }

  /**
   * Scenario: User is part of a group that has a WRITE permission. User has a token using this
   * scope. The group then has the permission downgraded to DENY. Behavior: Token should be revoked.
   */
  @Test
  @SneakyThrows
  public void denyGroupPermission_ExistingToken_RevokeTokenSuccess() {
    val user = entityGenerator.setupUser("UserFoo DenyGroupPermission");
    val group = entityGenerator.setupGroup("DenyGroupPermission");
    val policy = entityGenerator.setupSinglePolicy("PolicyForGroupDenyPermission");

    val accessToken = groupPermissionTestSetup(user, group, policy, AccessLevel.WRITE, "WRITE");

    val permissionDenyRequest =
        ImmutableList.of(
            PermissionRequest.builder().policyId(policy.getId()).mask(AccessLevel.DENY).build());
    val denyResponse =
        initStringRequest()
            .endpoint("/groups/%s/permissions", group.getId().toString())
            .body(permissionDenyRequest)
            .post();
    assertEquals(denyResponse.getStatusCode(), HttpStatus.OK);

    val checkTokenAfterUpgradeResponse =
        initStringRequest(tokenHeaders).endpoint("/o/check_api_key?token=%s", accessToken).post();

    // Should be revoked
    assertEquals(checkTokenAfterUpgradeResponse.getStatusCode(), HttpStatus.UNAUTHORIZED);
  }

  /**
   * Scenario: User is part of a group that has a WRITE permission. User has a token using this
   * scope. The user is then removed from this group. Behavior: Token should be revoked.
   */
  @Test
  @SneakyThrows
  public void removeUserFromGroupPermission_ExistingToken_RevokeTokenSuccess() {
    val user = entityGenerator.setupUser("UserFoo removeGroupPermission");
    val group = entityGenerator.setupGroup("RemoveGroupPermission");
    val policy = entityGenerator.setupSinglePolicy("PolicyForGroupRemovePermission");

    val accessToken = groupPermissionTestSetup(user, group, policy, AccessLevel.WRITE, "WRITE");

    val removeUserFromGroupResponse =
        initStringRequest()
            .endpoint("/users/%s/groups/%s", user.getId().toString(), group.getId().toString())
            .delete();
    assertEquals(removeUserFromGroupResponse.getStatusCode(), HttpStatus.OK);

    val checkTokenAfterUpgradeResponse =
        initStringRequest(tokenHeaders).endpoint("/o/check_api_key?token=%s", accessToken).post();

    // Should be revoked
    assertEquals(checkTokenAfterUpgradeResponse.getStatusCode(), HttpStatus.UNAUTHORIZED);
  }

  /**
   * Scenario: User is part of a group that has a WRITE permission. User has a token using this
   * scope. The user is then added to a group that has the DENY permission. Behavior: Token should
   * be revoked.
   */
  @Test
  @SneakyThrows
  public void addUserToDenyGroupPermission_ExistingToken_RevokeTokenSuccess() {
    val user = entityGenerator.setupUser("UserFoo addDenyGroupPermission");
    val group = entityGenerator.setupGroup("GoodExistingGroupPermission");
    val policy = entityGenerator.setupSinglePolicy("PolicyForDenyGroupAddPermission");

    val accessToken = groupPermissionTestSetup(user, group, policy, AccessLevel.WRITE, "WRITE");

    val groupDeny = entityGenerator.setupGroup("AddDenyGroupPermission");

    val permissionRequest =
        ImmutableList.of(
            PermissionRequest.builder().policyId(policy.getId()).mask(AccessLevel.DENY).build());
    initStringRequest()
        .endpoint("/groups/%s/permissions", groupDeny.getId().toString())
        .body(permissionRequest)
        .post();

    val groupRequest = ImmutableList.of(groupDeny.getId());
    val groupResponse =
        initStringRequest()
            .endpoint("/users/%s/groups", user.getId().toString())
            .body(groupRequest)
            .post();
    assertEquals(groupResponse.getStatusCode(), HttpStatus.OK);

    val checkTokenAfterUpgradeResponse =
        initStringRequest(tokenHeaders).endpoint("/o/check_api_key?token=%s", accessToken).post();

    // Should be revoked
    assertEquals(checkTokenAfterUpgradeResponse.getStatusCode(), HttpStatus.UNAUTHORIZED);
  }

  /**
   * Scenario: User is part of a group that has a READ permission. User has a token using this
   * scope. The user is then added to a group that has the WRITE permission. Behavior: Token should
   * remain valid.
   */
  @Test
  @SneakyThrows
  public void addUserToWriteGroupPermission_ExistingToken_KeepTokenSuccess() {
    val user = entityGenerator.setupUser("UserFoo addDenyGroupPermission");
    val group = entityGenerator.setupGroup("GoodExistingReadGroupPermission");
    val policy = entityGenerator.setupSinglePolicy("PolicyForWriteGroupUpgradeAddPermission");

    val accessToken = groupPermissionTestSetup(user, group, policy, AccessLevel.READ, "READ");

    val groupWrite = entityGenerator.setupGroup("AddWriteUpgradeGroupPermission");

    val permissionRequest =
        ImmutableList.of(
            PermissionRequest.builder().policyId(policy.getId()).mask(AccessLevel.WRITE).build());
    initStringRequest()
        .endpoint("/groups/%s/permissions", groupWrite.getId().toString())
        .body(permissionRequest)
        .post();

    val groupRequest = ImmutableList.of(groupWrite.getId());
    val groupResponse =
        initStringRequest()
            .endpoint("/users/%s/groups", user.getId().toString())
            .body(groupRequest)
            .post();
    assertEquals(groupResponse.getStatusCode(), HttpStatus.OK);

    val checkTokenAfterUpgradeResponse =
        initStringRequest(tokenHeaders).endpoint("/o/check_api_key?token=%s", accessToken).post();

    // Should be valid
    assertEquals(checkTokenAfterUpgradeResponse.getStatusCode(), HttpStatus.MULTI_STATUS);
  }

  /**
   * Scenario: User is part of a group that has a READ permission. User has a token using this
   * scope. The group is then deleted. Behavior: Token should be revoked.
   */
  @Test
  @SneakyThrows
  public void deleteGroupWithUserAndPermission_ExistingToken_RevokeTokenSuccess() {
    val user = entityGenerator.setupUser("UserFoo deleteGroupWithUserPermission");
    val group = entityGenerator.setupGroup("DeleteGroupWithUserPermission");
    val policy = entityGenerator.setupSinglePolicy("PolicyForDeleteGroupWithUserPermission");

    val accessToken = groupPermissionTestSetup(user, group, policy, AccessLevel.READ, "READ");

    val deleteGroupResponse =
        initStringRequest()
            .endpoint("/users/%s/groups/%s", user.getId().toString(), group.getId().toString())
            .delete();
    assertEquals(deleteGroupResponse.getStatusCode(), HttpStatus.OK);

    val checkTokenAfterGroupDeleteResponse =
        initStringRequest(tokenHeaders).endpoint("/o/check_api_key?token=%s", accessToken).post();

    // Should be revoked
    assertEquals(checkTokenAfterGroupDeleteResponse.getStatusCode(), HttpStatus.UNAUTHORIZED);
  }

  /**
   * This helper method is responsible for executing the pre-conditions of the scenario for user
   * permission mutations.
   *
   * @param user User that will have heir permissions mutated.
   * @param policy Policy that the permissions will be against.
   * @param initalAccess The initial access level (MASK) that a user will have to the policy.
   * @param tokenScopeSuffix The scope suffix that the token will be created with.
   * @return The access token
   */
  @SneakyThrows
  private String userPermissionTestSetup(
      User user, Policy policy, AccessLevel initalAccess, String tokenScopeSuffix) {
    val permissionRequest =
        ImmutableList.of(
            PermissionRequest.builder().policyId(policy.getId()).mask(initalAccess).build());
    initStringRequest()
        .endpoint("/users/%s/permissions", user.getId().toString())
        .body(permissionRequest)
        .post();

    val createTokenResponse =
        initStringRequest()
            .endpoint(
                "/o/api_key?user_id=%s&scopes=%s",
                user.getId().toString(), policy.getName() + "." + tokenScopeSuffix)
            .post();

    val tokenResponseJson = MAPPER.readTree(createTokenResponse.getBody());
    val accessToken = tokenResponseJson.get("accessToken").asText();

    val checkTokenResponse =
        initStringRequest(tokenHeaders).endpoint("/o/check_api_key?token=%s", accessToken).post();

    val checkStatusCode = checkTokenResponse.getStatusCode();
    assertEquals(checkStatusCode, HttpStatus.MULTI_STATUS);
    assertTrue(checkTokenResponse.getBody().contains(policy.getName() + "." + tokenScopeSuffix));

    return accessToken;
  }

  /**
   * This helper method is responsible for executing the pre-conditions of the scenario for group
   * permission mutations.
   *
   * @param user The user that will belong to the group which is having the permissions mutated.
   * @param group The group to which the group permissions are assigned.
   * @param policy The policy that the permission is relevant to.
   * @param initalAccess The initial access level (MASK) for the group on the policy.
   * @param tokenScopeSuffix The scope suffix that the token will be created with for the user.
   * @return The access token
   */
  @SneakyThrows
  private String groupPermissionTestSetup(
      User user, Group group, Policy policy, AccessLevel initalAccess, String tokenScopeSuffix) {

    // Associate User with Group
    val groupRequest = ImmutableList.of(group.getId());
    val groupResponse =
        initStringRequest()
            .endpoint("/users/%s/groups", user.getId().toString())
            .body(groupRequest)
            .post();
    assertEquals(groupResponse.getStatusCode(), HttpStatus.OK);

    // Create Group Permission
    val permissionRequest =
        ImmutableList.of(
            PermissionRequest.builder().policyId(policy.getId()).mask(initalAccess).build());
    initStringRequest()
        .endpoint("/groups/%s/permissions", group.getId().toString())
        .body(permissionRequest)
        .post();

    val createTokenResponse =
        initStringRequest()
            .endpoint(
                "/o/api_key?user_id=%s&scopes=%s",
                user.getId().toString(), policy.getName() + "." + tokenScopeSuffix)
            .post();

    val tokenResponseJson = MAPPER.readTree(createTokenResponse.getBody());
    val accessToken = tokenResponseJson.get("accessToken").asText();

    val checkTokenResponse =
        initStringRequest(tokenHeaders).endpoint("/o/check_api_key?token=%s", accessToken).post();

    val checkStatusCode = checkTokenResponse.getStatusCode();
    assertEquals(checkStatusCode, HttpStatus.MULTI_STATUS);
    assertTrue(checkTokenResponse.getBody().contains(policy.getName() + "." + tokenScopeSuffix));

    return accessToken;
  }
}
