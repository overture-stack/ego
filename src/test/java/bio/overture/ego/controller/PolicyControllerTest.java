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
import static bio.overture.ego.utils.Collectors.toImmutableList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.OK;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.PolicyRequest;
import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.service.PolicyService;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.Streams;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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

  @SneakyThrows
  @Test
  public void listUserPermission_findAllQuery_Success() {
    val policyId = entityGenerator.setupSinglePolicy("ListUserPermissions").getId().toString();
    val users =
        entityGenerator.setupUsers(
            "User 1", "User 2", "User 3", "User 4", "User 5", "User 6", "User 7", "User 8");

    val userIds = users.stream().map(user -> user.getId().toString()).collect(Collectors.toList());

    userIds.stream()
        .map(
            id ->
                initStringRequest()
                    .endpoint("/policies/%s/permission/user/%s", policyId, id)
                    .body(createMaskJson(READ.toString()))
                    .post())
        .collect(Collectors.toList());

    val response = initStringRequest().endpoint("/policies/%s/users", policyId).get();

    val responseStatus = response.getStatusCode();
    assertEquals(responseStatus, OK);

    val requestWithLimit =
        initStringRequest()
            .endpoint("/policies/%s/users", policyId)
            .queryParam("limit", 5)
            .queryParam("offset", 0);

    val responseWithLimit = requestWithLimit.get();
    assertEquals(responseWithLimit.getStatusCode(), OK);
    val responseJson = MAPPER.readTree(responseWithLimit.getBody());

    assertEquals(userIds.size(), responseJson.get("count").asInt());
    assertEquals(5, responseJson.get("resultSet").size());

    requestWithLimit.getAnd().assertOk().assertPageResultsOfType(PolicyResponse.class);
  }

  @Test
  @SneakyThrows
  public void findUserPermission_findSomeQuery_Success() {
    val policyId =
        entityGenerator.setupSinglePolicy("ShowUserPermissionsWithQuery").getId().toString();
    val users =
        entityGenerator.setupUsers("User 1", "User 2", "User 3", "User 4", "User 5", "User 6");

    val userIds = users.stream().map(user -> user.getId().toString()).collect(Collectors.toList());

    userIds.stream()
        .map(
            id ->
                initStringRequest()
                    .endpoint("/policies/%s/permission/user/%s", policyId, id)
                    .body(createMaskJson(READ.toString()))
                    .post())
        .collect(Collectors.toList());

    val response = initStringRequest().endpoint("/policies/%s/users", policyId).get();

    val responseStatus = response.getStatusCode();
    assertEquals(responseStatus, OK);

    val requestWithInvalidQuery =
        initStringRequest()
            .endpoint("/policies/%s/users", policyId)
            .queryParam("limit", 20)
            .queryParam("offset", 0)
            .queryParam("query", "write");

    val responseWithInvalidQuery = requestWithInvalidQuery.get();
    assertEquals(responseWithInvalidQuery.getStatusCode(), OK);
    val invalidQueryResponseJson = MAPPER.readTree(responseWithInvalidQuery.getBody());
    assertEquals(0, invalidQueryResponseJson.get("count").asInt());

    val requestWithValidQuery =
        initStringRequest()
            .endpoint("/policies/%s/users", policyId)
            .queryParam("limit", 20)
            .queryParam("offset", 0)
            .queryParam("query", "read");

    val responseWithValidQuery = requestWithValidQuery.get();
    assertEquals(responseWithValidQuery.getStatusCode(), OK);
    val validQueryResponseJson = MAPPER.readTree(responseWithValidQuery.getBody());
    assertEquals(6, validQueryResponseJson.get("count").asInt());

    requestWithInvalidQuery.getAnd().assertPageResultsOfType(PolicyResponse.class);

    val requestWithValidQueryAndLimit =
        initStringRequest()
            .endpoint("/policies/%s/users", policyId)
            .queryParam("limit", 5)
            .queryParam("offset", 0)
            .queryParam("query", "read");

    val responseWithValidQueryAndLimit = requestWithValidQueryAndLimit.get();
    assertEquals(responseWithValidQueryAndLimit.getStatusCode(), OK);
    val validQueryAndLimitResponseJson = MAPPER.readTree(responseWithValidQueryAndLimit.getBody());
    assertEquals(6, validQueryAndLimitResponseJson.get("count").asInt());
    assertEquals(5, validQueryAndLimitResponseJson.get("resultSet").size());

    requestWithInvalidQuery.getAnd().assertPageResultsOfType(PolicyResponse.class);
  }

  @SneakyThrows
  @Test
  public void listUserPermission_sorted_Success() {
    val policyId =
        entityGenerator.setupSinglePolicy("ListSortedUserPermissions").getId().toString();
    val usersWithRead =
        entityGenerator.setupUsers(
            "user 1", "Atticus Finch", "bomba John", "Barry White", "User Five", "user six");
    val usersWithWrite = entityGenerator.setupUsers("Julia Child", "Judith Jones");

    val userWithReadIds =
        usersWithRead.stream().map(user -> user.getId().toString()).collect(Collectors.toList());

    userWithReadIds.stream()
        .map(
            id ->
                initStringRequest()
                    .endpoint("/policies/%s/permission/user/%s", policyId, id)
                    .body(createMaskJson(READ.toString()))
                    .post())
        .collect(Collectors.toList());

    val usersWithWriteIds =
        usersWithWrite.stream().map(user -> user.getId().toString()).collect(Collectors.toList());

    usersWithWriteIds.stream()
        .map(
            id ->
                initStringRequest()
                    .endpoint("/policies/%s/permission/user/%s", policyId, id)
                    .body(createMaskJson(WRITE.toString()))
                    .post())
        .collect(Collectors.toList());

    val response = initStringRequest().endpoint("/policies/%s/users", policyId).get();

    val responseStatus = response.getStatusCode();
    assertEquals(responseStatus, OK);

    // test with non nested sort value
    val requestWithNonNestedSort =
        initStringRequest()
            .endpoint("/policies/%s/users", policyId)
            .queryParam("sort", "accessLevel")
            .queryParam("sortOrder", "ASC");

    val responseWithNonNestedSort = requestWithNonNestedSort.get();
    assertEquals(responseWithNonNestedSort.getStatusCode(), OK);
    val responseJson = MAPPER.readTree(responseWithNonNestedSort.getBody());

    assertEquals(8, responseJson.get("count").asInt());

    val resultSet = responseJson.get("resultSet");
    assertTrue(resultSet.isArray());
    requestWithNonNestedSort.getAnd().assertOk().assertPageResultsOfType(PolicyResponse.class);
    assertEquals(resultSet.get(0).get("mask").asText(), READ.toString());
    assertEquals(resultSet.get(7).get("mask").asText(), WRITE.toString());

    // test with nested sort value
    val requestWithNestedSort =
        initStringRequest()
            .endpoint("/policies/%s/users", policyId)
            .queryParam("sort", "owner.name")
            .queryParam("sortOrder", "ASC");

    val responseWithNestedSort = requestWithNestedSort.get();
    assertEquals(responseWithNestedSort.getStatusCode(), OK);
    val nestedSortResponseJson = MAPPER.readTree(responseWithNestedSort.getBody());

    assertEquals(8, nestedSortResponseJson.get("count").asInt());

    val nestedSortResultSet = nestedSortResponseJson.get("resultSet");
    assertTrue(nestedSortResultSet.isArray());
    requestWithNestedSort.getAnd().assertOk().assertPageResultsOfType(PolicyResponse.class);

    val responseNames =
        Streams.stream(nestedSortResultSet.iterator())
            .map(r -> r.get("name").asText())
            .collect(toImmutableList());

    List<User> combinedUserList = new ArrayList(usersWithRead);
    combinedUserList.addAll(usersWithWrite);

    List<String> sortedList =
        combinedUserList.stream()
            .map(User::getEmail)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(toImmutableList());

    assertEquals(responseNames, sortedList);
  }

  @SneakyThrows
  @Test
  public void listGroupPermission_findAllQuery_Success() {
    val policyId = entityGenerator.setupSinglePolicy("ListGroupPermissions").getId().toString();
    val groups =
        entityGenerator.setupGroups(
            "Group 1", "Group 2", "Group 3", "Group 4", "Group 5", "Group 6", "Group 7", "Group 8");

    val groupIds =
        groups.stream().map(group -> group.getId().toString()).collect(Collectors.toList());

    groupIds.stream()
        .map(
            id ->
                initStringRequest()
                    .endpoint("/policies/%s/permission/group/%s", policyId, id)
                    .body(createMaskJson(READ.toString()))
                    .post())
        .collect(Collectors.toList());

    val response = initStringRequest().endpoint("/policies/%s/groups", policyId).get();

    val responseStatus = response.getStatusCode();
    assertEquals(responseStatus, OK);

    val requestWithLimit =
        initStringRequest()
            .endpoint("/policies/%s/groups", policyId)
            .queryParam("limit", 5)
            .queryParam("offset", 0);

    val responseWithLimit = requestWithLimit.get();
    assertEquals(responseWithLimit.getStatusCode(), OK);
    val responseJson = MAPPER.readTree(responseWithLimit.getBody());

    assertEquals(groupIds.size(), responseJson.get("count").asInt());
    assertEquals(5, responseJson.get("resultSet").size());

    requestWithLimit.getAnd().assertOk().assertPageResultsOfType(PolicyResponse.class);
  }

  @Test
  @SneakyThrows
  public void findGroupPermission_findSomeQuery_Success() {
    val policyId =
        entityGenerator.setupSinglePolicy("ShowGroupPermissionsWithQuery").getId().toString();
    val groups =
        entityGenerator.setupGroups(
            "Group 1", "Group 2", "Group 3", "Group 4", "Group 5", "Group 6");

    val groupIds =
        groups.stream().map(group -> group.getId().toString()).collect(Collectors.toList());

    groupIds.stream()
        .map(
            id ->
                initStringRequest()
                    .endpoint("/policies/%s/permission/group/%s", policyId, id)
                    .body(createMaskJson(READ.toString()))
                    .post())
        .collect(Collectors.toList());

    val response = initStringRequest().endpoint("/policies/%s/groups", policyId).get();

    val responseStatus = response.getStatusCode();
    assertEquals(responseStatus, OK);

    val requestWithInvalidQuery =
        initStringRequest()
            .endpoint("/policies/%s/groups", policyId)
            .queryParam("limit", 20)
            .queryParam("offset", 0)
            .queryParam("query", "write");

    val responseWithInvalidQuery = requestWithInvalidQuery.get();
    assertEquals(responseWithInvalidQuery.getStatusCode(), OK);
    val invalidQueryResponseJson = MAPPER.readTree(responseWithInvalidQuery.getBody());
    assertEquals(0, invalidQueryResponseJson.get("count").asInt());

    val requestWithValidQuery =
        initStringRequest()
            .endpoint("/policies/%s/groups", policyId)
            .queryParam("limit", 20)
            .queryParam("offset", 0)
            .queryParam("query", "read");

    val responseWithValidQuery = requestWithValidQuery.get();
    assertEquals(responseWithValidQuery.getStatusCode(), OK);
    val validQueryResponseJson = MAPPER.readTree(responseWithValidQuery.getBody());
    assertEquals(6, validQueryResponseJson.get("count").asInt());

    requestWithInvalidQuery.getAnd().assertPageResultsOfType(PolicyResponse.class);

    // test with query and limit less than total count
    val requestWithValidQueryAndLimit =
        initStringRequest()
            .endpoint("/policies/%s/groups", policyId)
            .queryParam("limit", 4)
            .queryParam("offset", 0)
            .queryParam("query", "read");

    val responseWithValidQueryAndLimit = requestWithValidQueryAndLimit.get();
    assertEquals(responseWithValidQueryAndLimit.getStatusCode(), OK);
    val validQueryAndLimitResponseJson = MAPPER.readTree(responseWithValidQueryAndLimit.getBody());
    assertEquals(6, validQueryAndLimitResponseJson.get("count").asInt());
    assertEquals(4, validQueryAndLimitResponseJson.get("resultSet").size());

    requestWithValidQueryAndLimit.getAnd().assertPageResultsOfType(PolicyResponse.class);
  }

  @SneakyThrows
  @Test
  public void listGroupPermission_sorted_Success() {
    val policyId =
        entityGenerator.setupSinglePolicy("ListSortedGroupPermissions").getId().toString();
    val groupsWithRead =
        entityGenerator.setupGroups(
            "group 1", "Test Group", "B Group", "A Group", "ZZ Group", "xylophones");
    val groupsWithWrite =
        entityGenerator.setupGroups("Write Group One", "write permission group", "Writers");

    val groupsWithReadIds =
        groupsWithRead.stream().map(group -> group.getId().toString()).collect(Collectors.toList());

    groupsWithReadIds.stream()
        .map(
            id ->
                initStringRequest()
                    .endpoint("/policies/%s/permission/group/%s", policyId, id)
                    .body(createMaskJson(READ.toString()))
                    .post())
        .collect(Collectors.toList());

    val groupsWithWriteIds =
        groupsWithWrite.stream()
            .map(group -> group.getId().toString())
            .collect(Collectors.toList());

    groupsWithWriteIds.stream()
        .map(
            id ->
                initStringRequest()
                    .endpoint("/policies/%s/permission/group/%s", policyId, id)
                    .body(createMaskJson(WRITE.toString()))
                    .post())
        .collect(Collectors.toList());

    val response = initStringRequest().endpoint("/policies/%s/groups", policyId).get();

    val responseStatus = response.getStatusCode();
    assertEquals(responseStatus, OK);

    // test with non nested sort value
    val requestWithNonNestedSort =
        initStringRequest()
            .endpoint("/policies/%s/groups", policyId)
            .queryParam("sort", "accessLevel")
            .queryParam("sortOrder", "ASC");

    val responseWithNonNestedSort = requestWithNonNestedSort.get();
    assertEquals(responseWithNonNestedSort.getStatusCode(), OK);
    val responseJson = MAPPER.readTree(responseWithNonNestedSort.getBody());

    assertEquals(9, responseJson.get("count").asInt());

    val resultSet = responseJson.get("resultSet");
    assertTrue(resultSet.isArray());
    requestWithNonNestedSort.getAnd().assertOk().assertPageResultsOfType(PolicyResponse.class);

    assertEquals(resultSet.get(0).get("mask").asText(), READ.toString());
    assertEquals(resultSet.get(8).get("mask").asText(), WRITE.toString());

    // test with nested sort value
    val requestWithNestedSort =
        initStringRequest()
            .endpoint("/policies/%s/groups", policyId)
            .queryParam("sort", "owner.name")
            .queryParam("sortOrder", "ASC");

    val responseWithNestedSort = requestWithNestedSort.get();
    assertEquals(responseWithNestedSort.getStatusCode(), OK);
    val nestedSortResponseJson = MAPPER.readTree(responseWithNestedSort.getBody());

    assertEquals(9, nestedSortResponseJson.get("count").asInt());

    val nestedSortResultSet = nestedSortResponseJson.get("resultSet");
    assertTrue(nestedSortResultSet.isArray());
    requestWithNestedSort.getAnd().assertOk().assertPageResultsOfType(PolicyResponse.class);

    val responseNames =
        Streams.stream(nestedSortResultSet.iterator())
            .map(r -> r.get("name").asText())
            .collect(toImmutableList());

    List<Group> combinedGroupList = new ArrayList(groupsWithRead);
    combinedGroupList.addAll(groupsWithWrite);

    List<String> sortedList =
        combinedGroupList.stream()
            .map(Group::getName)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .collect(toImmutableList());

    assertEquals(responseNames, sortedList);
  }
}
