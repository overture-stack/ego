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

import static bio.overture.ego.controller.resolver.PageableResolver.LIMIT;
import static bio.overture.ego.controller.resolver.PageableResolver.OFFSET;
import static bio.overture.ego.model.enums.JavaFields.*;
import static bio.overture.ego.model.enums.LanguageType.*;
import static bio.overture.ego.model.enums.ProviderType.*;
import static bio.overture.ego.model.enums.StatusType.DISABLED;
import static bio.overture.ego.model.enums.UserType.USER;
import static bio.overture.ego.utils.CollectionUtils.mapToImmutableSet;
import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static bio.overture.ego.utils.CollectionUtils.repeatedCallsOf;
import static bio.overture.ego.utils.Collectors.toImmutableList;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Converters.convertToIds;
import static bio.overture.ego.utils.EntityGenerator.*;
import static bio.overture.ego.utils.Joiners.COMMA;
import static bio.overture.ego.utils.Streams.stream;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.*;
import static org.springframework.http.HttpStatus.FORBIDDEN;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.UpdateUserRequest;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.LanguageType;
import bio.overture.ego.model.enums.StatusType;
import bio.overture.ego.model.enums.UserType;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.NotImplementedException;
import org.hibernate.LazyInitializationException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class UserControllerTest extends AbstractControllerTest {

  private static boolean hasRunEntitySetup = false;

  /** Dependencies */
  @Autowired private EntityGenerator entityGenerator;

  @Autowired private UserService userService;
  @Autowired private ApplicationService applicationService;
  @Autowired private GroupService groupService;

  @Rule public ExpectedException exceptionRule = ExpectedException.none();

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
      entityGenerator.setupTestApplications();
      entityGenerator.setupTestGroups();
      hasRunEntitySetup = true;
    }
  }

  @Test
  @SneakyThrows
  public void listUsersNoFilter() {
    val numUsers = userService.getRepository().count();

    // Since previous test may introduce new users. If there are more users than the
    // default page
    // size, only a subset will be returned and could cause a test failure.
    val response = initStringRequest().endpoint("/users?offset=0&limit=%s", numUsers).get();

    val responseStatus = response.getStatusCode();
    val responseJson = MAPPER.readTree(response.getBody());

    assertEquals(responseStatus, HttpStatus.OK);
    assertTrue(responseJson.get("count").asInt() >= 3);
    assertTrue(responseJson.get("resultSet").isArray());

    // Verify that the returned Users are the ones from the setup.
    Iterable<JsonNode> resultSetIterable = () -> responseJson.get("resultSet").iterator();
    val actualUserNames =
        stream(resultSetIterable).map(j -> j.get("name").asText()).collect(toImmutableList());
    assertTrue(
        actualUserNames.containsAll(
            Set.of("FirstUser@domain.com", "SecondUser@domain.com", "ThirdUser@domain.com")));
  }

  @Test
  @SneakyThrows
  public void listUsersWithQuery() {
    val response = initStringRequest().endpoint("/users?query=FirstUser").get();

    val responseStatus = response.getStatusCode();
    val responseJson = MAPPER.readTree(response.getBody());

    assertEquals(responseStatus, HttpStatus.OK);
    assertEquals(responseJson.get("count").asInt(), 1);
    assertTrue(responseJson.get("resultSet").isArray());
    assertEquals(
        responseJson.get("resultSet").elements().next().get("name").asText(),
        "FirstUser@domain.com");
  }

  @Test
  public void findUsers_FindAllQuery_Success() {
    // Generate data
    val data = generateUniqueTestUserData();

    val numUsers = userService.getRepository().count();

    // Assert that you can page users that were created
    listUsersEndpointAnd()
        .queryParam(LIMIT, numUsers)
        .queryParam(OFFSET, 0)
        .getAnd()
        .assertPageResultsOfType(User.class)
        .containsAll(data.getUsers());
  }

  @Test
  @Ignore("low priority, but should be implemented")
  public void findUsers_FindSomeQuery_Success() {
    throw new NotImplementedException(
        "need to implement the test 'findUsers_FindSomeQuery_Success'");
  }

  @Test
  public void validateUpdateRequest_ProviderIdDoesntMatch_Forbidden() {
    // create a user with providerInfo
    val data = generateUniqueTestUserData();
    val user = data.getUsers().get(0);

    val nonExistentProviderId = generateNonExistentProviderId(userService);

    // Assert update with different providerId
    val r1 =
        UpdateUserRequest.builder()
            .providerType(user.getProviderType())
            .providerId(nonExistentProviderId)
            .preferredLanguage(SPANISH)
            .build();

    initStringRequest()
        .endpoint("/users/%s", user.getId())
        .body(r1)
        .putAnd()
        .assertStatusCode(FORBIDDEN);
  }

  @Test
  public void validateUpdateRequest_ProviderTypeDoesntMatch_Forbidden() {
    // create a user with providerInfo
    val data = generateUniqueTestUserData();
    val user = data.getUsers().get(0);

    // Assert update with different providerType
    val r1 =
        UpdateUserRequest.builder()
            .providerType(GITHUB)
            .providerId(user.getProviderId())
            .preferredLanguage(FRENCH)
            .build();

    initStringRequest()
        .endpoint("/users/%s", user.getId())
        .body(r1)
        .putAnd()
        .assertStatusCode(FORBIDDEN);
  }

  @Test
  public void validateUpdateRequest_ProviderInfoMatches_Success() {
    // create a user with providerInfo
    val data = generateUniqueTestUserData();
    val user = data.getUsers().get(0);

    // Assert update with matching providerType and providerId
    val r1 =
        UpdateUserRequest.builder()
            .providerType(user.getProviderType())
            .providerId(user.getProviderId())
            .preferredLanguage(SPANISH)
            .build();

    initStringRequest().endpoint("/users/%s", user.getId()).body(r1).putAnd().assertOk();
  }

  @Test
  public void deleteUser_NonExisting_NotFound() {
    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    // Assert that you cannot delete a non-existent id
    deleteGroupDeleteRequestAnd(nonExistentId).assertNotFound();
  }

  @Test
  public void deleteUserAndRelationshipsOnly_AlreadyExisting_Success() {
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Add applications to user
    addApplicationsToUserPostRequestAnd(user0, data.getApplications()).assertOk();

    // Add groups to user
    addGroupsToUserPostRequestAnd(user0, data.getGroups()).assertOk();

    // Check applications were added
    getApplicationsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Application.class)
        .containsExactlyInAnyOrderElementsOf(data.getApplications());

    // Check groups were added
    getGroupsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Group.class)
        .containsExactlyInAnyOrderElementsOf(data.getGroups());

    // Delete user
    deleteUserDeleteRequestAnd(user0).assertOk();

    // Check user was deleted
    getUserEntityGetRequestAnd(user0).assertNotFound();

    // Check applications exist
    data.getApplications()
        .forEach(application -> getApplicationEntityGetRequestAnd(application).assertOk());

    // Check groups exist
    data.getGroups().forEach(group -> getGroupEntityGetRequestAnd(group).assertOk());

    // Check no users associated with applications
    data.getApplications()
        .forEach(
            a ->
                getUsersForApplicationGetRequestAnd(a)
                    .assertPageResultsOfType(User.class)
                    .isEmpty());

    // Check no users associated with groups
    data.getGroups()
        .forEach(
            g -> getUsersForGroupGetRequestAnd(g).assertPageResultsOfType(User.class).isEmpty());
  }

  @Test
  public void getUser_ExistingUser_Success() {
    // Generate user
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Assert actual and expected user are the same
    getUserEntityGetRequestAnd(user0.getId()).assertEntityOfType(User.class).isEqualTo(user0);
  }

  @Test
  public void getUser_NonExistentUser_NotFound() {
    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    // Assert that you cannot get a non-existent id
    getUserEntityGetRequestAnd(nonExistentId).assertNotFound();
  }

  @Test
  public void UUIDValidation_MalformedUUID_BadRequest() {
    val data = generateUniqueTestUserData();
    val badUUID = "123sksk";
    val applicationIds = convertToIds(data.getApplications());
    val groupIds = convertToIds(data.getGroups());
    val randomPermIds = repeatedCallsOf(UUID::randomUUID, 3);

    initStringRequest().endpoint("/users/%s", badUUID).deleteAnd().assertBadRequest();
    initStringRequest().endpoint("/users/%s", badUUID).getAnd().assertBadRequest();
    initStringRequest().endpoint("/users/%s", badUUID).putAnd().assertBadRequest();

    initStringRequest().endpoint("/users/%s/applications", badUUID).getAnd().assertBadRequest();
    initStringRequest().endpoint("/users/%s/applications", badUUID).postAnd().assertBadRequest();
    initStringRequest()
        .endpoint("/users/%s/applications/%s", badUUID, COMMA.join(applicationIds))
        .deleteAnd()
        .assertBadRequest();

    initStringRequest().endpoint("/users/%s/groups", badUUID).getAnd().assertBadRequest();
    initStringRequest().endpoint("/users/%s/groups", badUUID).postAnd().assertBadRequest();
    initStringRequest()
        .endpoint("/users/%s/groups/%s", badUUID, COMMA.join(groupIds))
        .deleteAnd()
        .assertBadRequest();

    initStringRequest().endpoint("/users/%s/permissions", badUUID).getAnd().assertBadRequest();
    initStringRequest().endpoint("/users/%s/permissions", badUUID).postAnd().assertBadRequest();
    initStringRequest()
        .endpoint("/users/%s/permissions/%s", badUUID, COMMA.join(randomPermIds))
        .deleteAnd()
        .assertBadRequest();
  }

  @Test
  @SneakyThrows
  public void getManyUsers_noRelations() {

    final int testUserCount = 3;

    // Creating new users cause we need their Ids as inputs in this test
    val users = repeatedCallsOf(() -> entityGenerator.generateRandomUser(), testUserCount);
    val userIds = mapToImmutableSet(users, User::getId);

    val results = userService.getMany(userIds, false, false, false);

    assertEquals(results.size(), testUserCount);

    val testUser = results.iterator().next();

    try {
      testUser.getUserGroups().iterator().next().getId();
      Assert.fail("No exception thrown accessing groups that were not fetched for user.");
    } catch (Exception e) {
      assertTrue(e instanceof LazyInitializationException);
    }

    try {
      testUser.getUserApplications().iterator().next().getId();
      Assert.fail("No exception thrown accessing applications that were not fetched for user.");
    } catch (Exception e) {
      assertTrue(e instanceof LazyInitializationException);
    }

    try {
      testUser.getUserPermissions().iterator().next().getId();
      Assert.fail("No exception thrown accessing permissions that were not fetched for user.");
    } catch (Exception e) {
      assertTrue(e instanceof LazyInitializationException);
    }
  }

  @Test
  @SneakyThrows
  public void getManyUsers_withRelations() {

    final int testUserCount = 3;

    // Creating new users cause we need their Ids as inputs in this test
    val users = repeatedCallsOf(() -> entityGenerator.generateRandomUser(), testUserCount);
    val userIds = mapToImmutableSet(users, User::getId);

    val results = userService.getMany(userIds, true, true, true);

    assertEquals(results.size(), testUserCount);

    results
        .iterator()
        .forEachRemaining(
            user -> {
              try {
                assertNotNull(user.getUserGroups());
                assertTrue(user.getUserGroups().isEmpty());
                assertNotNull(user.getUserPermissions());
                assertTrue(user.getUserPermissions().isEmpty());
                assertNotNull(user.getUserApplications());
                assertTrue(user.getUserApplications().isEmpty());
              } catch (Exception e) {
                e.printStackTrace();
              }
            });
  }

  @Test
  @SneakyThrows
  public void updateUser_ExistingUser_Success() {
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // create update request 1
    val uniqueName = generateNonExistentName(userService);
    val email = uniqueName + "@xyz.com";
    val r1 =
        UpdateUserRequest.builder()
            .firstName("aNewFirstName")
            .providerType(user0.getProviderType())
            .providerId(user0.getProviderId())
            .email(email)
            .build();

    // Update user
    partialUpdateUserPutRequestAnd(user0.getId(), r1).assertOk();

    // Assert update was correct
    val actualUser1 = getUserEntityGetRequestAnd(user0).extractOneEntity(User.class);
    assertEquals(actualUser1.getFirstName(), r1.getFirstName());
    assertEquals(actualUser1.getEmail(), r1.getEmail());
    assertEquals(actualUser1.getName(), r1.getEmail());

    // create update request 2
    val r2 =
        UpdateUserRequest.builder()
            .providerType(user0.getProviderType())
            .providerId(user0.getProviderId())
            .status(randomEnumExcluding(StatusType.class, user0.getStatus()))
            .type(randomEnumExcluding(UserType.class, user0.getType()))
            .preferredLanguage(
                randomEnumExcluding(LanguageType.class, user0.getPreferredLanguage()))
            .build();

    // Update user
    partialUpdateUserPutRequestAnd(user0.getId(), r2).assertOk();

    // Assert update was correct
    val actualUser2 = getUserEntityGetRequestAnd(user0).extractOneEntity(User.class);
    assertEquals(actualUser2.getStatus(), r2.getStatus());
    assertEquals(actualUser2.getType(), r2.getType());
    assertEquals(actualUser2.getPreferredLanguage(), r2.getPreferredLanguage());
  }

  @Test
  public void updateUser_NonExistentUser_NotFound() {
    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    val dummyUpdateUserRequest = UpdateUserRequest.builder().build();

    // Assert that you cannot get a non-existent id
    partialUpdateUserPutRequestAnd(nonExistentId, dummyUpdateUserRequest).assertNotFound();
  }

  @Test
  public void updateUser_EmailAlreadyExists_OK() {
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);
    val user1 = data.getUsers().get(1);

    // Assumptions
    assertEquals(user0.getName(), user0.getEmail());
    assertEquals(user1.getName(), user1.getEmail());

    // Create update request with same email
    val r1 =
        UpdateUserRequest.builder()
            .email(user1.getName())
            .status(randomEnumExcluding(StatusType.class, user0.getStatus()))
            .providerType(user0.getProviderType())
            .providerId(user0.getProviderId())
            .build();

    // Assert that an OK response when trying to update a user with a name that already exists
    partialUpdateUserPutRequestAnd(user0.getId(), r1).assertOk();
  }

  // TODO: FIX do not use /users POST
  // with new createUser implementation, status is not handled until update
  @Test
  public void statusValidation_MalformedStatus_BadRequest() {
    val invalidStatus = "something123";
    val match = stream(StatusType.values()).anyMatch(x -> x.toString().equals(invalidStatus));
    assertFalse(match);

    val data = generateUniqueTestUserData();
    val user = data.getUsers().get(0);

    // Assert createUsers
    //    val templateR1 =
    //        CreateUserRequest.builder()
    //            .email(generateNonExistentName(userService) + "@xyz.com")
    //            .type(USER)
    //            .lastName("")
    //            .lastName("")
    //            .preferredLanguage(ENGLISH)
    //            .build();
    //    val r1 = ((ObjectNode) MAPPER.valueToTree(templateR1)).put(STATUS, invalidStatus);
    //    initStringRequest().endpoint("/users").body(r1).postAnd().assertBadRequest();

    // Assert updateUser
    val templateR2 =
        UpdateUserRequest.builder()
            .email(generateNonExistentName(userService) + "@xyz.com")
            .type(USER)
            .preferredLanguage(ENGLISH)
            .build();
    val r2 = ((ObjectNode) MAPPER.valueToTree(templateR2)).put(STATUS, invalidStatus);
    initStringRequest().endpoint("/users/%s", user.getId()).body(r2).putAnd().assertBadRequest();
  }

  // TODO: FIX do not use /users POST
  // with new createUser implementation, type is not handled until update
  @Test
  public void typeValidation_MalformedType_BadRequest() {
    val invalidType = "something123";
    val match = stream(UserType.values()).anyMatch(x -> x.toString().equals(invalidType));
    assertFalse(match);

    val data = generateUniqueTestUserData();
    val user = data.getUsers().get(0);

    // Assert createUsers
    //    val templateR1 =
    //        CreateUserRequest.builder()
    //            .email(generateNonExistentName(userService) + "@xyz.com")
    //            .status(APPROVED)
    //            .preferredLanguage(ENGLISH)
    //            .firstName("")
    //            .lastName("")
    //            .build();
    //    val r1 = ((ObjectNode) MAPPER.valueToTree(templateR1)).put(TYPE, invalidType);
    //    initStringRequest().endpoint("/users").body(r1).postAnd().assertBadRequest();

    // Assert updateUser
    val templateR2 =
        UpdateUserRequest.builder()
            .email(generateNonExistentName(userService) + "@xyz.com")
            .status(DISABLED)
            .preferredLanguage(ENGLISH)
            .build();
    val r2 = ((ObjectNode) MAPPER.valueToTree(templateR2)).put(TYPE, invalidType);
    initStringRequest().endpoint("/users/%s", user.getId()).body(r2).putAnd().assertBadRequest();
  }

  // TODO: FIX do not use /users POST
  // with new createUser implementation, preferredLanguage is not handled until update
  @Test
  public void preferredLanguageValidation_MalformedPreferredLanguage_BadRequest() {
    val invalidLanguage = "something123";
    val match = stream(LanguageType.values()).anyMatch(x -> x.toString().equals(invalidLanguage));
    assertFalse(match);

    val data = generateUniqueTestUserData();
    val user = data.getUsers().get(0);

    // Assert createUsers
    //    val templateR1 =
    //        CreateUserRequest.builder()
    //            .email(generateNonExistentName(userService) + "@xyz.com")
    //            .status(APPROVED)
    //            .type(USER)
    //            .firstName("")
    //            .lastName("")
    //            .build();
    //    val r1 = ((ObjectNode) MAPPER.valueToTree(templateR1)).put(PREFERREDLANGUAGE,
    // invalidLanguage);
    //    initStringRequest().endpoint("/users").body(r1).postAnd().assertBadRequest();

    // Assert updateUser
    val templateR2 =
        UpdateUserRequest.builder()
            .email(generateNonExistentName(userService) + "@xyz.com")
            .status(DISABLED)
            .type(USER)
            .build();
    val r2 = ((ObjectNode) MAPPER.valueToTree(templateR2)).put(PREFERREDLANGUAGE, invalidLanguage);
    initStringRequest().endpoint("/users/%s", user.getId()).body(r2).putAnd().assertBadRequest();
  }

  @Test
  public void getApplicationsFromUser_FindAllQuery_Success() {
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Associate applications with user
    addApplicationsToUserPostRequestAnd(user0, data.getApplications()).assertOk();

    // get Applications for user, and assert it has the expected applications
    getApplicationsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Application.class)
        .containsExactlyInAnyOrderElementsOf(data.getApplications());
  }

  @Test
  public void getApplicationsFromUser_NonExistentUser_NotFound() {
    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    // Assert that getting the applications for a non-existent user id results in a
    // NOT_FOUND error
    getApplicationsForUserGetRequestAnd(nonExistentId).assertNotFound();
  }

  @Test
  @Ignore("low priority, but should be implemented")
  public void getApplicationsFromUser_FindSomeQuery_Success() {
    throw new NotImplementedException("need to implement");
  }

  @Test
  public void addApplicationsToUser_NonExistentUser_NotFound() {
    // Generate data
    val data = generateUniqueTestUserData();
    val existingApplicationIds = convertToIds(data.getApplications());

    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    // Assert NOT_FOUND thrown when adding existing applications to a non-existing
    // user
    addApplicationsToUserPostRequestAnd(nonExistentId, existingApplicationIds).assertNotFound();
  }

  @Test
  public void addApplicationsToUser_AllExistingUnassociatedApplications_Success() {
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Assert the user has no applications
    getApplicationsForUserGetRequestAnd(user0).assertPageResultsOfType(Application.class).isEmpty();

    // Add applications to user and assert the response is equal to the user
    addApplicationsToUserPostRequestAnd(user0, data.getApplications())
        .assertEntityOfType(User.class)
        .isEqualToIgnoringGivenFields(user0, USERPERMISSIONS, TOKENS, USERGROUPS, USERAPPLICATIONS);

    // Assert the user has all the applications
    getApplicationsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Application.class)
        .containsExactlyInAnyOrderElementsOf(data.getApplications());
  }

  @Test
  public void addApplicationsToUser_SomeExistingApplicationsButAllUnassociated_NotFound() {
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Assert NOT_FOUND thrown when adding non-existing applications to an existing
    // user
    val someExistingApplicationIds = mapToSet(data.getApplications(), Identifiable::getId);
    val nonExistingApplicationIds =
        repeatedCallsOf(() -> generateNonExistentId(applicationService), 10).stream()
            .collect(toImmutableSet());
    someExistingApplicationIds.addAll(nonExistingApplicationIds);

    addApplicationsToUserPostRequestAnd(user0.getId(), someExistingApplicationIds).assertNotFound();
  }

  @Test
  public void addApplicationsToUser_AllExistingApplicationsButSomeAlreadyAssociated_Conflict() {
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);
    val app0 = data.getApplications().get(0);
    val app1 = data.getApplications().get(1);

    // Assert the user has no applications
    getApplicationsForUserGetRequestAnd(user0).assertPageResultsOfType(Application.class).isEmpty();

    // Add app00 to user and assert the response is equal to the user
    addApplicationsToUserPostRequestAnd(user0, newArrayList(app0))
        .assertEntityOfType(User.class)
        .isEqualToIgnoringGivenFields(user0, USERPERMISSIONS, TOKENS, USERGROUPS, USERAPPLICATIONS);

    // Assert the user has app0
    getApplicationsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Application.class)
        .containsExactlyInAnyOrder(app0);

    // Add app0 and app1 to user and assert a CONFLICT error is returned since app0
    // was already
    // associated
    addApplicationsToUserPostRequestAnd(user0, newArrayList(app0, app1)).assertConflict();
  }

  @Test
  public void removeApplicationsFromUser_AllExistingAssociatedApplications_Success() {
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Assert the user has no applications
    getApplicationsForUserGetRequestAnd(user0).assertPageResultsOfType(Application.class).isEmpty();

    // Add apps to user and assert user is returned
    addApplicationsToUserPostRequestAnd(user0, data.getApplications())
        .assertEntityOfType(User.class)
        .isEqualToIgnoringGivenFields(user0, USERPERMISSIONS, TOKENS, USERGROUPS, USERAPPLICATIONS);

    // Assert the user has all the applications
    getApplicationsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Application.class)
        .containsExactlyInAnyOrderElementsOf(data.getApplications());

    // Delete applications from user
    deleteApplicationsFromUserDeleteRequestAnd(user0, data.getApplications()).assertOk();

    // Assert the user has no applications
    getApplicationsForUserGetRequestAnd(user0).assertPageResultsOfType(Application.class).isEmpty();
  }

  @Test
  public void removeApplicationsFromUser_AllExistingApplicationsButSomeNotAssociated_NotFound() {
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);
    val app0 = data.getApplications().get(0);
    val app1 = data.getApplications().get(1);

    // Assert the user has no applications
    getApplicationsForUserGetRequestAnd(user0).assertPageResultsOfType(Application.class).isEmpty();

    // Add apps to user and assert user is returned
    addApplicationsToUserPostRequestAnd(user0, newArrayList(app0))
        .assertEntityOfType(User.class)
        .isEqualToIgnoringGivenFields(user0, USERPERMISSIONS, TOKENS, USERGROUPS, USERAPPLICATIONS);

    // Assert the user is associated with app0
    getApplicationsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Application.class)
        .containsExactlyInAnyOrder(app0);

    // Delete applications from user
    deleteApplicationsFromUserDeleteRequestAnd(user0, newArrayList(app0, app1)).assertNotFound();
  }

  @Test
  public void removeApplicationsFromUser_SomeNonExistingApplicationsButAllAssociated_NotFound() {
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Assert the user has no applications
    getApplicationsForUserGetRequestAnd(user0).assertPageResultsOfType(Application.class).isEmpty();

    // Add all apps to user
    addApplicationsToUserPostRequestAnd(user0, data.getApplications())
        .assertEntityOfType(User.class)
        .isEqualToIgnoringGivenFields(user0, USERPERMISSIONS, TOKENS, USERGROUPS, USERAPPLICATIONS);

    // Assert the apps were added
    getApplicationsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Application.class)
        .containsExactlyInAnyOrderElementsOf(data.getApplications());

    // Create non existing application id
    val nonExistingApplicationId = generateNonExistentId(applicationService);
    getApplicationEntityGetRequestAnd(nonExistingApplicationId).assertNotFound();

    // Create a list of application ids with some not existing
    val someExistingApplicationsIds = Sets.<UUID>newHashSet();
    someExistingApplicationsIds.addAll(convertToIds(data.getApplications()));
    someExistingApplicationsIds.add(nonExistingApplicationId);

    // Delete applications from user and assert a NOT_FOUND error was returned due
    // to the
    // non-existing application id
    deleteApplicationsFromUserDeleteRequestAnd(user0.getId(), someExistingApplicationsIds)
        .assertNotFound();
  }

  @Test
  public void removeApplicationsFromUser_NonExistentUser_NotFound() {
    // Generate data
    val data = generateUniqueTestUserData();
    val existingApplicationIds = convertToIds(data.getApplications());

    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    // Assert NOT_FOUND thrown when deleting applications to a non-existing user
    deleteApplicationsFromUserDeleteRequestAnd(nonExistentId, existingApplicationIds)
        .assertNotFound();
  }

  @Test
  public void getGroupsFromUser_FindAllQuery_Success() {
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Assert no groups are associated with the user
    getGroupsForUserGetRequestAnd(user0).assertPageResultsOfType(Group.class).isEmpty();

    // Add groups to the user
    addGroupsToUserPostRequestAnd(user0, data.getGroups()).assertOk();

    // Assert all the groups are associated with the user
    getGroupsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Group.class)
        .containsExactlyInAnyOrderElementsOf(data.getGroups());
  }

  @Test
  public void getGroupsFromUser_NonExistentUser_NotFound() {
    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    // Assert that a NOT_FOUND error is thrown when attempting to all groups for a
    // non-existent user
    getGroupsForUserGetRequestAnd(nonExistentId).assertNotFound();
  }

  @Test
  @Ignore("low priority, but should be implemented")
  public void getGroupsFromUser_FindSomeQuery_Success() {
    throw new NotImplementedException(
        "need to implement the test 'getGroupsFromUser_FindSomeQuery_Success'");
  }

  @Test
  public void addGroupsToUser_NonExistentUser_NotFound() {
    val data = generateUniqueTestUserData();
    val existingGroupIds = convertToIds(data.getGroups());

    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    // Assert that a NOT_FOUND error is thrown when attempting to add existing
    // groups to a
    // non-existing user
    addGroupsToUserPostRequestAnd(nonExistentId, existingGroupIds).assertNotFound();
  }

  @Test
  public void addGroupsToUser_AllExistingUnassociatedGroups_Success() {
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Assert user has no groups
    getGroupsForUserGetRequestAnd(user0).assertPageResultsOfType(Group.class).isEmpty();

    // Add groups to user and asser response is a user
    addGroupsToUserPostRequestAnd(user0, data.getGroups())
        .assertEntityOfType(User.class)
        .isEqualToIgnoringGivenFields(user0, USERPERMISSIONS, TOKENS, USERGROUPS, USERAPPLICATIONS);

    // Get groups for user and assert they are associated
    getGroupsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Group.class)
        .containsExactlyInAnyOrderElementsOf(data.getGroups());
  }

  @Test
  public void addGroupsToUser_SomeExistingGroupsButAllUnassociated_NotFound() {
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Assert NOT_FOUND thrown when adding a mix of existing and non-existing
    // applications to an
    // existing user
    val someExistingGroupIds = mapToSet(data.getGroups(), Identifiable::getId);
    val nonExistingGroupIds =
        repeatedCallsOf(() -> generateNonExistentId(groupService), 10).stream()
            .collect(toImmutableSet());
    someExistingGroupIds.addAll(nonExistingGroupIds);

    addGroupsToUserPostRequestAnd(user0.getId(), someExistingGroupIds).assertNotFound();
  }

  @Test
  public void addGroupsToUser_AllExistingGroupsButSomeAlreadyAssociated_Conflict() {
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);
    val group0 = data.getGroups().get(0);
    val group1 = data.getGroups().get(1);

    // Assert user has no groups
    getGroupsForUserGetRequestAnd(user0).assertPageResultsOfType(Group.class).isEmpty();

    // Add group0 to user and assert response is a user
    addGroupsToUserPostRequestAnd(user0, newArrayList(group0))
        .assertEntityOfType(User.class)
        .isEqualToIgnoringGivenFields(user0, USERPERMISSIONS, TOKENS, USERGROUPS, USERAPPLICATIONS);

    // Get groups for user and assert they are associated only to group0
    getGroupsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Group.class)
        .containsExactlyInAnyOrderElementsOf(newArrayList(group0));

    // Add group0 and group1 to user and assert CONFLICT
    addGroupsToUserPostRequestAnd(user0, newArrayList(group0, group1)).assertConflict();
  }

  @Test
  public void removeGroupsFromUser_AllExistingAssociatedGroups_Success() {
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Add groups to user and assert response is a user
    addGroupsToUserPostRequestAnd(user0, data.getGroups())
        .assertEntityOfType(User.class)
        .isEqualToIgnoringGivenFields(user0, USERPERMISSIONS, TOKENS, USERGROUPS, USERAPPLICATIONS);

    // Assert groups were added
    getGroupsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Group.class)
        .containsExactlyInAnyOrderElementsOf(data.getGroups());

    // Delete groups from user
    deleteGroupsFromUserDeleteRequestAnd(user0, data.getGroups()).assertOk();

    // Assert user does not have any groups associated
    getGroupsForUserGetRequestAnd(user0).assertPageResultsOfType(Group.class).isEmpty();
  }

  @Test
  public void removeGroupsFromUser_AllExistingGroupsButSomeNotAssociated_NotFound() {
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);
    val group0 = data.getGroups().get(0);
    val group1 = data.getGroups().get(1);

    // Assert user has no groups
    getGroupsForUserGetRequestAnd(user0).assertPageResultsOfType(Group.class).isEmpty();

    // Add group0 only to user
    addGroupsToUserPostRequestAnd(user0, newArrayList(group0)).assertOk();

    // Assert group0 was added to user
    getGroupsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Group.class)
        .containsExactlyInAnyOrder(group0);

    // Attempt to delete group0 and group1 from user, and assert NOT_FOUND error
    deleteGroupsFromUserDeleteRequestAnd(user0, newArrayList(group0, group1)).assertNotFound();
  }

  @Test
  public void removeGroupsFromUser_SomeNonExistingGroupsButAllAssociated_NotFound() {
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Assert no groups for user
    getGroupsForUserGetRequestAnd(user0).assertPageResultsOfType(Group.class).isEmpty();

    // Add all groups to user
    addGroupsToUserPostRequestAnd(user0, data.getGroups()).assertOk();

    // Assert groups were added to user
    getGroupsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Group.class)
        .containsExactlyInAnyOrderElementsOf(data.getGroups());

    // delete all groups plus some that dont exist and assert NOT_FOUND error
    val groupIdsToDelete = newHashSet(convertToIds(data.getGroups()));
    groupIdsToDelete.add(generateNonExistentId(groupService));
    deleteGroupsFromUserDeleteRequestAnd(user0.getId(), groupIdsToDelete).assertNotFound();
  }

  @Test
  public void removeGroupsFromUser_NonExistentUser_NotFound() {
    // Setup data
    val data = generateUniqueTestUserData();
    val groupIds = convertToIds(data.getGroups());

    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    // Assert that a NOT_FOUND error is returned when trying to delete groups from a
    // non-existent
    // user
    deleteGroupsFromUserDeleteRequestAnd(nonExistentId, groupIds).assertNotFound();
  }

  @SneakyThrows
  private TestUserData generateUniqueTestUserData() {
    val groups = repeatedCallsOf(() -> entityGenerator.generateRandomGroup(), 2);
    val applications = repeatedCallsOf(() -> entityGenerator.generateRandomApplication(), 2);
    val policies = repeatedCallsOf(() -> entityGenerator.generateRandomPolicy(), 2);
    val users = repeatedCallsOf(() -> entityGenerator.generateRandomUser(), 2);

    return TestUserData.builder()
        .users(users)
        .groups(groups)
        .applications(applications)
        .policies(policies)
        .build();
  }

  @lombok.Value
  @Builder
  public static class TestUserData {
    @NonNull private final List<User> users;
    @NonNull private final List<Group> groups;
    @NonNull private final List<Application> applications;
    @NonNull private final List<Policy> policies;
  }
}
