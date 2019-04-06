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
import bio.overture.ego.model.dto.CreateUserRequest;
import bio.overture.ego.model.dto.UpdateUserRequest;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.LanguageType;
import bio.overture.ego.model.enums.StatusType;
import bio.overture.ego.model.enums.UserType;
import bio.overture.ego.model.join.UserGroup;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.Streams;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.NotImplementedException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.UUID;

import static bio.overture.ego.controller.resolver.PageableResolver.LIMIT;
import static bio.overture.ego.controller.resolver.PageableResolver.OFFSET;
import static bio.overture.ego.model.enums.JavaFields.APPLICATIONS;
import static bio.overture.ego.model.enums.JavaFields.ID;
import static bio.overture.ego.model.enums.JavaFields.NAME;
import static bio.overture.ego.model.enums.JavaFields.TOKENS;
import static bio.overture.ego.model.enums.JavaFields.USERGROUPS;
import static bio.overture.ego.model.enums.JavaFields.USERPERMISSIONS;
import static bio.overture.ego.model.enums.LanguageType.ENGLISH;
import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static bio.overture.ego.model.enums.StatusType.REJECTED;
import static bio.overture.ego.model.enums.UserType.USER;
import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static bio.overture.ego.utils.CollectionUtils.repeatedCallsOf;
import static bio.overture.ego.utils.Collectors.toImmutableList;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Converters.convertToIds;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentId;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentName;
import static bio.overture.ego.utils.EntityGenerator.randomEnum;
import static bio.overture.ego.utils.EntityGenerator.randomStringNoSpaces;
import static bio.overture.ego.utils.EntityTools.extractUserIds;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

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
      entityGenerator.setupTestApplications();
      entityGenerator.setupTestGroups();
      hasRunEntitySetup = true;
    }
  }

  @Test
  public void addUser() {

    val user =
        CreateUserRequest.builder()
            .firstName("foo")
            .lastName("bar")
            .email("foobar@foo.bar")
            .preferredLanguage(ENGLISH)
            .type(USER)
            .status(APPROVED)
            .build();

    val response = initStringRequest().endpoint("/users").body(user).post();

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void addUniqueUser() {
    val user1 =
        CreateUserRequest.builder()
            .firstName("unique")
            .lastName("unique")
            .email("unique@unique.com")
            .preferredLanguage(ENGLISH)
            .type(USER)
            .status(APPROVED)
            .build();
    val user2 =
        CreateUserRequest.builder()
            .firstName("unique")
            .lastName("unique")
            .email("unique@unique.com")
            .preferredLanguage(ENGLISH)
            .type(USER)
            .status(APPROVED)
            .build();

    val response1 = initStringRequest().endpoint("/users").body(user1).post();
    val responseStatus1 = response1.getStatusCode();

    assertThat(responseStatus1).isEqualTo(HttpStatus.OK);

    // Return a 409 conflict because email already exists for a registered user.
    val response2 = initStringRequest().endpoint("/users").body(user2).post();
    val responseStatus2 = response2.getStatusCode();
    assertThat(responseStatus2).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  @SneakyThrows
  public void getUser() {

    // Users created in setup
    val userId = userService.getByName("FirstUser@domain.com").getId();
    val response = initStringRequest().endpoint("/users/%s", userId).get();

    val responseStatus = response.getStatusCode();
    val responseJson = MAPPER.readTree(response.getBody());

    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    assertThat(responseJson.get("firstName").asText()).isEqualTo("First");
    assertThat(responseJson.get("lastName").asText()).isEqualTo("User");
    assertThat(responseJson.get("name").asText()).isEqualTo("FirstUser@domain.com");
    assertThat(responseJson.get("preferredLanguage").asText()).isEqualTo(ENGLISH.toString());
    assertThat(responseJson.get("status").asText()).isEqualTo(APPROVED.toString());
    assertThat(responseJson.get("id").asText()).isEqualTo(userId.toString());
  }

  @Test
  public void getUser404() {
    val response = initStringRequest().endpoint("/users/%s", UUID.randomUUID().toString()).get();

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  @SneakyThrows
  public void listUsersNoFilter() {
    val numUsers = userService.getRepository().count();

    // Since previous test may introduce new users. If there are more users than the default page
    // size, only a subset will be returned and could cause a test failure.
    val response = initStringRequest().endpoint("/users?offset=0&limit=%s", numUsers).get();

    val responseStatus = response.getStatusCode();
    val responseJson = MAPPER.readTree(response.getBody());

    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    assertThat(responseJson.get("count").asInt()).isGreaterThanOrEqualTo(3);
    assertThat(responseJson.get("resultSet").isArray()).isTrue();

    // Verify that the returned Users are the ones from the setup.
    Iterable<JsonNode> resultSetIterable = () -> responseJson.get("resultSet").iterator();
    val actualUserNames =
        Streams.stream(resultSetIterable)
            .map(j -> j.get("name").asText())
            .collect(toImmutableList());
    assertThat(actualUserNames)
        .contains("FirstUser@domain.com", "SecondUser@domain.com", "ThirdUser@domain.com");
  }

  @Test
  @SneakyThrows
  public void listUsersWithQuery() {
    val response = initStringRequest().endpoint("/users?query=FirstUser").get();

    val responseStatus = response.getStatusCode();
    val responseJson = MAPPER.readTree(response.getBody());

    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    assertThat(responseJson.get("count").asInt()).isEqualTo(1);
    assertThat(responseJson.get("resultSet").isArray()).isTrue();
    assertThat(responseJson.get("resultSet").elements().next().get("name").asText())
        .isEqualTo("FirstUser@domain.com");
  }

  @Test
  public void updateUser() {
    val user = entityGenerator.setupUser("update test");
    val update = UpdateUserRequest.builder().status(REJECTED).build();

    val response = initStringRequest().endpoint("/users/%s", user.getId()).body(update).put();

    val responseBody = response.getBody();

    HttpStatus responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    assertThatJson(responseBody).node("id").isEqualTo(user.getId());
    assertThatJson(responseBody).node("status").isEqualTo(REJECTED.toString());
  }

  @Test
  @SneakyThrows
  public void addGroupToUser() {
    val userId = entityGenerator.setupUser("Group1 User").getId();
    val groupId = entityGenerator.setupGroup("Addone Group").getId().toString();

    val response =
        initStringRequest()
            .endpoint("/users/%s/groups", userId)
            .body(singletonList(groupId))
            .post();

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);

    val groupResponse = initStringRequest().endpoint("/users/%s/groups", userId).get();

    val groupResponseStatus = groupResponse.getStatusCode();
    assertThat(groupResponseStatus).isEqualTo(HttpStatus.OK);

    val groupResponseJson = MAPPER.readTree(groupResponse.getBody());
    assertThat(groupResponseJson.get("count").asInt()).isEqualTo(1);
    assertThat(groupResponseJson.get("resultSet").elements().next().get("id").asText())
        .isEqualTo(groupId);
  }

  @Test
  @SneakyThrows
  public void deleteGroupFromUser() {
    val userId = entityGenerator.setupUser("DeleteGroup User").getId();
    val deleteGroup = entityGenerator.setupGroup("Delete One Group").getId().toString();
    val remainGroup = entityGenerator.setupGroup("Don't Delete This One").getId().toString();

    initStringRequest()
        .endpoint("/users/%s/groups", userId)
        .body(asList(deleteGroup, remainGroup))
        .post();

    val groupResponse = initStringRequest().endpoint("/users/%s/groups", userId).get();

    val groupResponseStatus = groupResponse.getStatusCode();
    assertThat(groupResponseStatus).isEqualTo(HttpStatus.OK);
    val groupResponseJson = MAPPER.readTree(groupResponse.getBody());
    assertThat(groupResponseJson.get("count").asInt()).isEqualTo(2);

    val deleteResponse =
        initStringRequest().endpoint("/users/%s/groups/%s", userId, deleteGroup).delete();

    val deleteResponseStatus = deleteResponse.getStatusCode();
    assertThat(deleteResponseStatus).isEqualTo(HttpStatus.OK);

    val secondGetResponse = initStringRequest().endpoint("/users/%s/groups", userId).get();
    val secondGetResponseStatus = deleteResponse.getStatusCode();
    assertThat(secondGetResponseStatus).isEqualTo(HttpStatus.OK);
    val secondGetResponseJson = MAPPER.readTree(secondGetResponse.getBody());
    assertThat(secondGetResponseJson.get("count").asInt()).isEqualTo(1);
    assertThat(secondGetResponseJson.get("resultSet").elements().next().get("id").asText())
        .isEqualTo(remainGroup);
  }

  @Test
  @SneakyThrows
  public void addApplicationToUser() {
    val userId = entityGenerator.setupUser("AddApp1 User").getId();
    val appId = entityGenerator.setupApplication("app1").getId().toString();

    val response =
        initStringRequest()
            .endpoint("/users/%s/applications", userId)
            .body(singletonList(appId))
            .post();

    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);

    val appResponse = initStringRequest().endpoint("/users/%s/applications", userId).get();

    val appResponseStatus = appResponse.getStatusCode();
    assertThat(appResponseStatus).isEqualTo(HttpStatus.OK);

    val groupResponseJson = MAPPER.readTree(appResponse.getBody());
    assertThat(groupResponseJson.get("count").asInt()).isEqualTo(1);
    assertThat(groupResponseJson.get("resultSet").elements().next().get("id").asText())
        .isEqualTo(appId);
  }

  @Test
  @SneakyThrows
  public void deleteApplicationFromUser() {
    val userId = entityGenerator.setupUser("App2 User").getId();
    val deleteApp = entityGenerator.setupApplication("deleteApp").getId().toString();
    val remainApp = entityGenerator.setupApplication("remainApp").getId().toString();

    val appResponse =
        initStringRequest()
            .endpoint("/users/%s/applications", userId)
            .body(asList(deleteApp, remainApp))
            .post();

    log.info(appResponse.getBody());

    val appResponseStatus = appResponse.getStatusCode();
    assertThat(appResponseStatus).isEqualTo(HttpStatus.OK);

    val deleteResponse =
        initStringRequest().endpoint("/users/%s/applications/%s", userId, deleteApp).delete();

    val deleteResponseStatus = deleteResponse.getStatusCode();
    assertThat(deleteResponseStatus).isEqualTo(HttpStatus.OK);

    val secondGetResponse = initStringRequest().endpoint("/users/%s/applications", userId).get();

    val secondGetResponseStatus = deleteResponse.getStatusCode();
    assertThat(secondGetResponseStatus).isEqualTo(HttpStatus.OK);
    val secondGetResponseJson = MAPPER.readTree(secondGetResponse.getBody());
    assertThat(secondGetResponseJson.get("count").asInt()).isEqualTo(1);
    assertThat(secondGetResponseJson.get("resultSet").elements().next().get("id").asText())
        .isEqualTo(remainApp);
  }

  @Test
  @SneakyThrows
  public void deleteUser() {
    val userId = entityGenerator.setupUser("User ToDelete").getId();

    // Add application to user
    val appOne = entityGenerator.setupApplication("TempGroupApp");
    val appBody = singletonList(appOne.getId().toString());
    val addAppToUserResponse =
        initStringRequest().endpoint("/users/%s/applications", userId).body(appBody).post();
    val addAppToUserResponseStatus = addAppToUserResponse.getStatusCode();
    assertThat(addAppToUserResponseStatus).isEqualTo(HttpStatus.OK);

    // Make sure user-application relationship is there
    val appWithUser = applicationService.getByClientId("TempGroupApp");
    assertThat(extractUserIds(appWithUser.getUsers())).contains(userId);

    // Add group to user
    val groupOne = entityGenerator.setupGroup("GroupOne");
    val groupBody = singletonList(groupOne.getId().toString());
    val addGroupToUserResponse =
        initStringRequest().endpoint("/users/%s/groups", userId).body(groupBody).post();
    val addGroupToUserResponseStatus = addGroupToUserResponse.getStatusCode();
    assertThat(addGroupToUserResponseStatus).isEqualTo(HttpStatus.OK);
    // Make sure user-group relationship is there
    val expectedUserGroups = groupService.getByName("GroupOne").getUserGroups();
    val expectedUsers = mapToSet(expectedUserGroups, UserGroup::getUser);
    assertThat(extractUserIds(expectedUsers)).contains(userId);

    // delete user
    val deleteResponse = initStringRequest().endpoint("/users/%s", userId).delete();
    val deleteResponseStatus = deleteResponse.getStatusCode();
    assertThat(deleteResponseStatus).isEqualTo(HttpStatus.OK);

    // verify if user is deleted
    val getUserResponse = initStringRequest().endpoint("/users/%s", userId).get();
    val getUserResponseStatus = getUserResponse.getStatusCode();
    assertThat(getUserResponseStatus).isEqualTo(HttpStatus.NOT_FOUND);
    val jsonResponse = MAPPER.readTree(getUserResponse.getBody());
    assertThat(jsonResponse.get("error").asText())
        .isEqualTo(HttpStatus.NOT_FOUND.getReasonPhrase());

    // check if user - group is deleted
    val groupWithoutUser = groupService.getByName("GroupOne");
    assertThat(groupWithoutUser.getUserGroups()).isEmpty();

    // make sure user - application is deleted
    val appWithoutUser = applicationService.getByClientId("TempGroupApp");
    assertThat(appWithoutUser.getUsers()).isEmpty();
  }

	@Test
	public void findUsers_FindAllQuery_Success(){
    // Generate data
    val data = generateUniqueTestUserData();

    val numUsers = userService.getRepository().count();

    // Assert that you can page users that were created
    listUsersEndpointAnd()
        .queryParam(LIMIT, numUsers )
        .queryParam(OFFSET, 0)
        .getAnd()
        .assertPageResultsOfType(User.class)
        .containsAll(data.getUsers());
	}

	@Test
  @Ignore
	public void findUsers_FindSomeQuery_Success(){
		throw new NotImplementedException("need to implement the test 'findUsers_FindSomeQuery_Success'");
	}

	@Test
	public void createUser_NonExisting_Success(){
    // Create unique name
    val name = generateNonExistentName(userService);

    // Create request
    val r = CreateUserRequest.builder()
        .email(name+"@gmail.com")
        .status(randomEnum(StatusType.class))
        .type(randomEnum(UserType.class))
        .preferredLanguage(randomEnum(LanguageType.class))
        .firstName(randomStringNoSpaces(10))
        .lastName(randomStringNoSpaces(10))
        .build();

    // Create the user
    val user = createUserPostRequestAnd(r)
        .extractOneEntity(User.class);
    assertThat(user).isEqualToIgnoringGivenFields(r, ID, APPLICATIONS, USERPERMISSIONS, USERGROUPS, TOKENS, NAME);


    // Assert the user can be read and matches the request data
    val r1 = getUserEntityGetRequestAnd(user)
        .extractOneEntity(User.class);
    assertThat(r1).isEqualToIgnoringGivenFields(r, ID, APPLICATIONS, USERPERMISSIONS, USERGROUPS, TOKENS, NAME);
    assertThat(r1).isEqualToIgnoringGivenFields(user);
	}

	@Test
	public void createUser_EmailAlreadyExists_Conflict(){
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Get an existing name
    val existingName = getUserEntityGetRequestAnd(user0)
        .extractOneEntity(User.class)
        .getName();

    // Create a request with an existing name
    val r = CreateUserRequest.builder()
        .email(existingName)
        .status(randomEnum(StatusType.class))
        .type(randomEnum(UserType.class))
        .preferredLanguage(randomEnum(LanguageType.class))
        .firstName(randomStringNoSpaces(10))
        .lastName(randomStringNoSpaces(10))
        .build();


    // Create the user and assert a conflict
    createUserPostRequestAnd(r).assertConflict();
	}

	@Test
	public void createUser_FutureCreatedAtDate_BadRequest(){
		throw new NotImplementedException("need to implement the test 'createUser_FutureCreatedAtDate_BadRequest'");
	}

	@Test
	public void deleteUser_NonExisting_NotFound(){
    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    // Assert that you cannot delete a non-existent id
    deleteGroupDeleteRequestAnd(nonExistentId).assertNotFound();
	}

	@Test
	public void deleteUserAndRelationshipsOnly_AlreadyExisting_Success(){
		throw new NotImplementedException("need to implement the test 'deleteUserAndRelationshipsOnly_AlreadyExisting_Success'");
	}

	@Test
	public void getUser_ExistingUser_Success(){
		throw new NotImplementedException("need to implement the test 'getUser_ExistingUser_Success'");
	}

	@Test
	public void getUser_NonExistentUser_NotFound(){
    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    // Assert that you cannot get a non-existent id
    getUserEntityGetRequestAnd(nonExistentId).assertNotFound();
	}

	@Test
	public void UUIDValidation_MalformedUUID_BadRequest(){
		throw new NotImplementedException("need to implement the test 'UUIDValidation_MalformedUUID_BadRequest'");
	}

	@Test
	public void updateUser_ExistingUser_Success(){
		throw new NotImplementedException("need to implement the test 'updateUser_ExistingUser_Success'");
	}

	@Test
	public void updateUser_NonExistentUser_NotFound(){
    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    val dummyUpdateUserRequest = UpdateUserRequest.builder().build();

    // Assert that you cannot get a non-existent id
    partialUpdateUserPutRequestAnd(nonExistentId, dummyUpdateUserRequest).assertNotFound();
	}

	@Test
	public void updateUser_EmailAlreadyExists_Conflict(){
		throw new NotImplementedException("need to implement the test 'updateUser_EmailAlreadyExists_Conflict'");
	}

	@Test
	public void updateUser_FutureLastLoginDate_BadRequest(){
		throw new NotImplementedException("need to implement the test 'updateUser_FutureLastLoginDate_BadRequest'");
	}

	@Test
	public void updateUser_LastLoginBeforeCreatedAtDate_BadRequest(){
		throw new NotImplementedException("need to implement the test 'updateUser_LastLoginBeforeCreatedAtDate_BadRequest'");
	}

	@Test
	public void statusValidation_MalformedStatus_BadRequest(){
		throw new NotImplementedException("need to implement the test 'statusValidation_MalformedStatus_BadRequest'");
	}

	@Test
	public void typeValidation_MalformedType_BadRequest(){
		throw new NotImplementedException("need to implement the test 'typeValidation_MalformedType_BadRequest'");
	}

	@Test
	public void preferredLanguageValidation_MalformedPreferredLanguage_BadRequest(){
		throw new NotImplementedException("need to implement the test 'preferredLanguageValidation_MalformedPreferredLanguage_BadRequest'");
	}

	@Test
	public void getApplicationsFromUser_FindAllQuery_Success(){
		throw new NotImplementedException("need to implement the test 'getApplicationsFromUser_FindAllQuery_Success'");
	}

	@Test
	public void getApplicationsFromUser_NonExistentUser_NotFound(){
    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    // Assert that getting the applicaitons for a non-existent user id results in a NOT_FOUND error
    getApplicationsForUserGetRequestAnd(nonExistentId).assertNotFound();
	}

	@Test
	public void getApplicationsFromUser_FindSomeQuery_Success(){
		throw new NotImplementedException("need to implement the test 'getApplicationsFromUser_FindSomeQuery_Success'");
	}

	@Test
	public void addApplicationsToUser_NonExistentUser_NotFound(){
    // Generate data
    val data = generateUniqueTestUserData();
    val existingApplicationIds = convertToIds(data.getApplications());

    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    // Assert NOT_FOUND thrown when adding existing applications to a non-existing user
    addApplicationsToUserPostRequestAnd(nonExistentId, existingApplicationIds).assertNotFound();
	}

	@Test
	public void addApplicationsToUser_AllExistingUnassociatedApplications_Success(){
		throw new NotImplementedException("need to implement the test 'addApplicationsToUser_AllExistingUnassociatedApplications_Success'");
	}

	@Test
	public void addApplicationsToUser_SomeExistingApplicationsButAllUnassociated_NotFound(){
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Assert NOT_FOUND thrown when adding non-existing applications to an existing user
    val someExistingApplicationIds = mapToSet(data.getApplications(), Identifiable::getId);
    val nonExistingApplicationIds = repeatedCallsOf(() -> generateNonExistentId(applicationService), 10).stream().collect(toImmutableSet());
    someExistingApplicationIds.addAll(nonExistingApplicationIds);

    addApplicationsToUserPostRequestAnd(user0.getId(), someExistingApplicationIds).assertNotFound();
	}

	@Test
	public void addApplicationsToUser_AllExsitingApplicationsButSomeAlreadyAssociated_Conflict(){
		throw new NotImplementedException("need to implement the test 'addApplicationsToUser_AllExsitingApplicationsButSomeAlreadyAssociated_Conflict'");
	}

	@Test
	public void removeApplicationsFromUser_AllExistingAssociatedApplications_Success(){
		throw new NotImplementedException("need to implement the test 'removeApplicationsFromUser_AllExistingAssociatedApplications_Success'");
	}

	@Test
	public void removeApplicationsFromUser_AllExistingApplicationsButSomeNotAssociated_NotFound(){
    throw new NotImplementedException("sdf");
	}

	@Test
	public void removeApplicationsFromUser_SomeNonExistingApplicationsButAllAssociated_NotFound(){
    throw new NotImplementedException("sdf");
	}

	@Test
	public void removeApplicationsFromUser_NonExistentUser_NotFound(){
    // Generate data
    val data = generateUniqueTestUserData();
    val existingApplicationIds = convertToIds(data.getApplications());

    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    // Assert NOT_FOUND thrown when deleting applications to a non-existing user
    deleteApplicationsFromUserDeleteRequestAnd(nonExistentId, existingApplicationIds).assertNotFound();
	}

	@Test
	public void getGroupsFromUser_FindAllQuery_Success(){
		throw new NotImplementedException("need to implement the test 'getGroupsFromUser_FindAllQuery_Success'");
	}

	@Test
	public void getGroupsFromUser_NonExistentUser_NotFound(){
    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    // Assert that a NOT_FOUND error is thrown when attempting to all groups for a non-existent user
    getGroupsForUserGetRequestAnd(nonExistentId).assertNotFound();
	}

	@Test
	public void getGroupsFromUser_FindSomeQuery_Success(){
		throw new NotImplementedException("need to implement the test 'getGroupsFromUser_FindSomeQuery_Success'");
	}

	@Test
	public void addGroupsToUser_NonExistentUser_NotFound(){
    val data = generateUniqueTestUserData();
    val existingGroupIds = convertToIds(data.getGroups());

    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    // Assert that a NOT_FOUND error is thrown when attempting to add existing groups to a non-existing user
    addGroupsToUserPostRequestAnd(nonExistentId, existingGroupIds).assertNotFound();
	}

	@Test
	public void addGroupsToUser_AllExistingUnassociatedGroups_Success(){
		throw new NotImplementedException("need to implement the test 'addGroupsToUser_AllExistingUnassociatedGroups_Success'");
	}

	@Test
	public void addGroupsToUser_SomeExistingGroupsButAllUnassociated_NotFound(){
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Assert NOT_FOUND thrown when adding a mix of existing and non-existing applications to an existing user
    val someExistingGroupIds = mapToSet(data.getGroups(), Identifiable::getId);
    val nonExistingGroupIds = repeatedCallsOf(() -> generateNonExistentId(groupService), 10).stream().collect(toImmutableSet());
    someExistingGroupIds.addAll(nonExistingGroupIds);

    addGroupsToUserPostRequestAnd(user0.getId(), someExistingGroupIds).assertNotFound();
	}

	@Test
	public void addGroupsToUser_AllExsitingGroupsButSomeAlreadyAssociated_Conflict(){
		throw new NotImplementedException("need to implement the test 'addGroupsToUser_AllExsitingGroupsButSomeAlreadyAssociated_Conflict'");
	}

	@Test
	public void removeGroupsFromUser_AllExistingAssociatedGroups_Success(){
		throw new NotImplementedException("need to implement the test 'removeGroupsFromUser_AllExistingAssociatedGroups_Success'");
	}

	@Test
	public void removeGroupsFromUser_AllExistingGroupsButSomeNotAssociated_NotFound(){
		throw new NotImplementedException("need to implement the test 'removeGroupsFromUser_AllExistingGroupsButSomeNotAssociated_NotFound'");
	}

	@Test
	public void removeGroupsFromUser_SomeNonExistingGroupsButAllAssociated_NotFound(){
		throw new NotImplementedException("need to implement the test 'removeGroupsFromUser_SomeNonExistingGroupsButAllAssociated_NotFound'");
	}

	@Test
	public void removeGroupsFromUser_NonExistentUser_NotFound(){
    // Setup data
    val data = generateUniqueTestUserData();
    val groupIds = convertToIds(data.getGroups());

    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    // Assert that a NOT_FOUND error is returned when trying to delete groups from a non-existent user
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
