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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
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

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static bio.overture.ego.controller.resolver.PageableResolver.LIMIT;
import static bio.overture.ego.controller.resolver.PageableResolver.OFFSET;
import static bio.overture.ego.model.enums.JavaFields.APPLICATIONS;
import static bio.overture.ego.model.enums.JavaFields.CREATEDAT;
import static bio.overture.ego.model.enums.JavaFields.ID;
import static bio.overture.ego.model.enums.JavaFields.LASTNAME;
import static bio.overture.ego.model.enums.JavaFields.NAME;
import static bio.overture.ego.model.enums.JavaFields.PREFERREDLANGUAGE;
import static bio.overture.ego.model.enums.JavaFields.STATUS;
import static bio.overture.ego.model.enums.JavaFields.TOKENS;
import static bio.overture.ego.model.enums.JavaFields.TYPE;
import static bio.overture.ego.model.enums.JavaFields.USERGROUPS;
import static bio.overture.ego.model.enums.JavaFields.USERPERMISSIONS;
import static bio.overture.ego.model.enums.LanguageType.ENGLISH;
import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static bio.overture.ego.model.enums.StatusType.DISABLED;
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
import static bio.overture.ego.utils.EntityGenerator.randomEnumExcluding;
import static bio.overture.ego.utils.EntityGenerator.randomStringNoSpaces;
import static bio.overture.ego.utils.EntityTools.extractUserIds;
import static bio.overture.ego.utils.Joiners.COMMA;
import static bio.overture.ego.utils.Streams.stream;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.time.temporal.ChronoUnit.DAYS;
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
        stream(resultSetIterable)
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
    assertThat(user).isEqualToIgnoringGivenFields(r, ID, APPLICATIONS, USERPERMISSIONS, USERGROUPS, TOKENS, NAME, CREATEDAT);


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
  @Ignore("test not possible as CreateUSerRequest does not have defineable date field")
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
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Add applications to user
    addApplicationsToUserPostRequestAnd(user0, data.getApplications()).assertOk();

    // Check applications were added
    getApplicationsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Application.class)
        .containsExactlyInAnyOrderElementsOf(data.getApplications());

    // Delete user
    deleteUserDeleteRequestAnd(user0).assertOk();

    // Check user was deleted
    getUserEntityGetRequestAnd(user0).assertNotFound();

    // Check applications exist
    data.getApplications().forEach(application -> getApplicationEntityGetRequestAnd(application).assertOk());

    // Check no users associated with applications
    data.getApplications().forEach(a -> getUsersForApplicationGetRequestAnd(a).assertPageResultsOfType(User.class).isEmpty());
	}

	@Test
	public void getUser_ExistingUser_Success(){
    // Generate user
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Assert actual and expected user are the same
    getUserEntityGetRequestAnd(user0.getId())
        .assertEntityOfType(User.class)
        .isEqualTo(user0);
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
    initStringRequest().endpoint("/users/%s/applications/%s", badUUID, COMMA.join(applicationIds)).deleteAnd().assertBadRequest();

    initStringRequest().endpoint("/users/%s/groups", badUUID).getAnd().assertBadRequest();
    initStringRequest().endpoint("/users/%s/groups", badUUID).postAnd().assertBadRequest();
    initStringRequest().endpoint("/users/%s/groups/%s", badUUID, COMMA.join(groupIds)).deleteAnd().assertBadRequest();

    initStringRequest().endpoint("/users/%s/permissions", badUUID).getAnd().assertBadRequest();
    initStringRequest().endpoint("/users/%s/permissions", badUUID).postAnd().assertBadRequest();
    initStringRequest().endpoint("/users/%s/permissions/%s", badUUID, COMMA.join(randomPermIds)).deleteAnd().assertBadRequest();
	}

	@Test
	public void updateUser_ExistingUser_Success(){
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // create update request 1
    val uniqueName = generateNonExistentName(userService);
    val email = uniqueName+"@xyz.com";
    val r1 = UpdateUserRequest.builder()
        .firstName("aNewFirstName")
        .email(email)
        .build();

    // Update user
    partialUpdateUserPutRequestAnd(user0.getId(), r1).assertOk();

    // Assert update was correct
    val actualUser1 = getUserEntityGetRequestAnd(user0)
        .extractOneEntity(User.class);
    assertThat(actualUser1).isEqualToIgnoringGivenFields(r1, TYPE, STATUS, NAME, LASTNAME, PREFERREDLANGUAGE,ID, CREATEDAT, USERPERMISSIONS, APPLICATIONS, USERGROUPS, TOKENS);
    assertThat(actualUser1.getFirstName()).isEqualTo(r1.getFirstName());
    assertThat(actualUser1.getEmail()).isEqualTo(r1.getEmail());
    assertThat(actualUser1.getName()).isEqualTo(r1.getEmail());

    // create update request 2
    val r2 = UpdateUserRequest.builder()
        .status(randomEnumExcluding(StatusType.class, user0.getStatus()))
        .type(randomEnumExcluding(UserType.class, user0.getType()))
        .preferredLanguage(randomEnumExcluding(LanguageType.class, user0.getPreferredLanguage()))
        .build();

    // Update user
    partialUpdateUserPutRequestAnd(user0.getId(), r2).assertOk();

    // Assert update was correct
    val actualUser2 = getUserEntityGetRequestAnd(user0)
        .extractOneEntity(User.class);
    assertThat(actualUser2.getStatus()).isEqualTo(r2.getStatus());
    assertThat(actualUser2.getType()).isEqualTo(r2.getType());
    assertThat(actualUser2.getPreferredLanguage()).isEqualTo(r2.getPreferredLanguage());
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
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);
    val user1 = data.getUsers().get(1);

    // Assumptions
    assertThat(user0.getName()).isEqualTo(user0.getEmail());
    assertThat(user1.getName()).isEqualTo(user1.getEmail());

    // Create update request with same email
    val r1 = UpdateUserRequest.builder()
        .email(user1.getName())
        .status(randomEnumExcluding(StatusType.class, user0.getStatus()))
        .build();

    // Assert that a CONFLICT error occurs when trying to update a user with a name that already exists
    partialUpdateUserPutRequestAnd(user0.getId(), r1).assertConflict();
	}

	@Test
	public void updateUser_FutureLastLoginDate_BadRequest(){
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Create update request with same email
    val futureDate = Date.from(Instant.now().plus(1, DAYS));
    val r1 = UpdateUserRequest.builder().lastLogin(futureDate).build();

    // Assert that updating with a future last login results in a BAD_REQUEST error
    partialUpdateUserPutRequestAnd(user0.getId(), r1).assertBadRequest();
	}

	@Test
	public void updateUser_LastLoginBeforeCreatedAtDate_BadRequest(){
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Create update request with same email
    val timeTravelDate = Date.from(user0.getCreatedAt().toInstant().minus(1, DAYS));
    assertThat(user0.getCreatedAt().after(timeTravelDate)).isTrue();
    val r1 = UpdateUserRequest.builder().lastLogin(timeTravelDate).build();

    // Assert that updating with a last login before the created at data results in a BAD_REQUEST error
    partialUpdateUserPutRequestAnd(user0.getId(), r1).assertBadRequest();
	}

	@Test
	public void statusValidation_MalformedStatus_BadRequest(){
    val invalidStatus = "something123";
    val match = stream(StatusType.values()).anyMatch(x -> x.toString().equals(invalidStatus));
    assertThat(match).isFalse();

    val data = generateUniqueTestUserData();
    val user = data.getUsers().get(0);

    // Assert createUsers
    val templateR1 = CreateUserRequest.builder()
        .email(generateNonExistentName(userService)+"@xyz.com")
        .type(USER)
        .preferredLanguage(ENGLISH)
        .build();
    val r1 = ((ObjectNode)MAPPER.valueToTree(templateR1)).put(STATUS, invalidStatus);
    initStringRequest()
        .endpoint("/users")
        .body(r1)
        .postAnd()
        .assertBadRequest();

    // Assert updateUser
    val templateR2 = UpdateUserRequest.builder()
        .email(generateNonExistentName(userService)+"@xyz.com")
        .type(USER)
        .preferredLanguage(ENGLISH)
        .build();
    val r2 = ((ObjectNode)MAPPER.valueToTree(templateR2)).put(STATUS, invalidStatus);
    initStringRequest()
        .endpoint("/users/%s", user.getId())
        .body(r2)
        .putAnd()
        .assertBadRequest();
	}

	@Test
	public void typeValidation_MalformedType_BadRequest(){
    val invalidType = "something123";
    val match = stream(UserType.values()).anyMatch(x -> x.toString().equals(invalidType));
    assertThat(match).isFalse();

    val data = generateUniqueTestUserData();
    val user = data.getUsers().get(0);

    // Assert createUsers
    val templateR1 = CreateUserRequest.builder()
        .email(generateNonExistentName(userService)+"@xyz.com")
        .status(APPROVED)
        .preferredLanguage(ENGLISH)
        .build();
    val r1 = ((ObjectNode)MAPPER.valueToTree(templateR1)).put(TYPE, invalidType);
    initStringRequest()
        .endpoint("/users")
        .body(r1)
        .postAnd()
        .assertBadRequest();

    // Assert updateUser
    val templateR2 = UpdateUserRequest.builder()
        .email(generateNonExistentName(userService)+"@xyz.com")
        .status(DISABLED)
        .preferredLanguage(ENGLISH)
        .build();
    val r2 = ((ObjectNode)MAPPER.valueToTree(templateR2)).put(TYPE, invalidType);
    initStringRequest()
        .endpoint("/users/%s", user.getId())
        .body(r2)
        .putAnd()
        .assertBadRequest();
	}

	@Test
	public void preferredLanguageValidation_MalformedPreferredLanguage_BadRequest(){
    val invalidLanguage = "something123";
    val match = stream(LanguageType.values()).anyMatch(x -> x.toString().equals(invalidLanguage));
    assertThat(match).isFalse();

    val data = generateUniqueTestUserData();
    val user = data.getUsers().get(0);

    // Assert createUsers
    val templateR1 = CreateUserRequest.builder()
        .email(generateNonExistentName(userService)+"@xyz.com")
        .status(APPROVED)
        .type(USER)
        .build();
    val r1 = ((ObjectNode)MAPPER.valueToTree(templateR1)).put(PREFERREDLANGUAGE, invalidLanguage);
    initStringRequest()
        .endpoint("/users")
        .body(r1)
        .postAnd()
        .assertBadRequest();

    // Assert updateUser
    val templateR2 = UpdateUserRequest.builder()
        .email(generateNonExistentName(userService)+"@xyz.com")
        .status(DISABLED)
        .type(USER)
        .build();
    val r2 = ((ObjectNode)MAPPER.valueToTree(templateR2)).put(PREFERREDLANGUAGE, invalidLanguage);
    initStringRequest()
        .endpoint("/users/%s", user.getId())
        .body(r2)
        .putAnd()
        .assertBadRequest();
	}

	@Test
	public void getApplicationsFromUser_FindAllQuery_Success(){
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
	public void getApplicationsFromUser_NonExistentUser_NotFound(){
    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    // Assert that getting the applicaitons for a non-existent user id results in a NOT_FOUND error
    getApplicationsForUserGetRequestAnd(nonExistentId).assertNotFound();
	}

	@Test
  @Ignore
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
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Assert the user has no applications
    getApplicationsForUserGetRequestAnd(user0).assertPageResultsOfType(Application.class).isEmpty();

    // Add applications to user and assert the response is equal to the user
    addApplicationsToUserPostRequestAnd(user0, data.getApplications()).assertEntityOfType(User.class).isEqualToIgnoringGivenFields(user0, USERPERMISSIONS, TOKENS, USERGROUPS, APPLICATIONS);

    // Assert the user has all the applications
    getApplicationsForUserGetRequestAnd(user0).assertPageResultsOfType(Application.class).containsExactlyInAnyOrderElementsOf(data.getApplications());
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
	public void addApplicationsToUser_AllExistingApplicationsButSomeAlreadyAssociated_Conflict(){
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);
    val app0 = data.getApplications().get(0);
    val app1 = data.getApplications().get(1);

    // Assert the user has no applications
    getApplicationsForUserGetRequestAnd(user0).assertPageResultsOfType(Application.class).isEmpty();

    // Add app00 to user and assert the response is equal to the user
    addApplicationsToUserPostRequestAnd(user0, newArrayList(app0)).assertEntityOfType(User.class).isEqualToIgnoringGivenFields(user0, USERPERMISSIONS, TOKENS, USERGROUPS, APPLICATIONS);

    // Assert the user has app0
    getApplicationsForUserGetRequestAnd(user0).assertPageResultsOfType(Application.class).containsExactlyInAnyOrder(app0);

    // Add app0 and app1 to user and assert a CONFLICT error is returned since app0 was already associated
    addApplicationsToUserPostRequestAnd(user0, newArrayList(app0, app1)).assertConflict();
	}

	@Test
	public void removeApplicationsFromUser_AllExistingAssociatedApplications_Success(){
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Assert the user has no applications
    getApplicationsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Application.class)
        .isEmpty();

    // Add apps to user and assert user is returned
    addApplicationsToUserPostRequestAnd(user0, data.getApplications())
        .assertEntityOfType(User.class)
        .isEqualToIgnoringGivenFields(user0, USERPERMISSIONS, TOKENS, USERGROUPS, APPLICATIONS);

    // Assert the user has all the applications
    getApplicationsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Application.class)
        .containsExactlyInAnyOrderElementsOf(data.getApplications());

    // Delete applications from user
    deleteApplicationsFromUserDeleteRequestAnd(user0,data.getApplications()).assertOk();

    // Assert the user has no applications
    getApplicationsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Application.class)
        .isEmpty();
	}

	@Test
	public void removeApplicationsFromUser_AllExistingApplicationsButSomeNotAssociated_NotFound(){
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);
    val app0 = data.getApplications().get(0);
    val app1 = data.getApplications().get(1);

    // Assert the user has no applications
    getApplicationsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Application.class)
        .isEmpty();

    // Add apps to user and assert user is returned
    addApplicationsToUserPostRequestAnd(user0, newArrayList(app0))
        .assertEntityOfType(User.class)
        .isEqualToIgnoringGivenFields(user0, USERPERMISSIONS, TOKENS, USERGROUPS, APPLICATIONS);

    // Assert the user is associated with app0
    getApplicationsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Application.class)
        .containsExactlyInAnyOrder(app0);

    // Delete applications from user
    deleteApplicationsFromUserDeleteRequestAnd(user0, newArrayList(app0, app1)).assertNotFound();
	}

	@Test
	public void removeApplicationsFromUser_SomeNonExistingApplicationsButAllAssociated_NotFound(){
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Assert the user has no applications
    getApplicationsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Application.class)
        .isEmpty();

    // Add all apps to user
    addApplicationsToUserPostRequestAnd(user0, data.getApplications())
        .assertEntityOfType(User.class)
        .isEqualToIgnoringGivenFields(user0, USERPERMISSIONS, TOKENS, USERGROUPS, APPLICATIONS);

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

    // Delete applications from user and assert a NOT_FOUND error was returned due to the non-existing application id
    deleteApplicationsFromUserDeleteRequestAnd(user0.getId(), someExistingApplicationsIds).assertNotFound();
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
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Assert no groups are associated with the user
    getGroupsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Group.class)
        .isEmpty();

    // Add groups to the user
    addGroupsToUserPostRequestAnd(user0, data.getGroups()).assertOk();

    // Assert all the groups are associated with the user
    getGroupsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Group.class)
        .containsExactlyInAnyOrderElementsOf(data.getGroups());
  }

	@Test
	public void getGroupsFromUser_NonExistentUser_NotFound(){
    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    // Assert that a NOT_FOUND error is thrown when attempting to all groups for a non-existent user
    getGroupsForUserGetRequestAnd(nonExistentId).assertNotFound();
	}

	@Test
  @Ignore("should test this")
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
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Assert user has no groups
    getGroupsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Group.class)
        .isEmpty();

    // Add groups to user and asser response is a user
    addGroupsToUserPostRequestAnd(user0, data.getGroups())
        .assertEntityOfType(User.class)
        .isEqualToIgnoringGivenFields(user0, USERPERMISSIONS, TOKENS, USERGROUPS, APPLICATIONS);

    // Get groups for user and assert they are associated
    getGroupsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Group.class)
        .containsExactlyInAnyOrderElementsOf(data.getGroups());
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
	public void addGroupsToUser_AllExistingGroupsButSomeAlreadyAssociated_Conflict(){
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);
    val group0 = data.getGroups().get(0);
    val group1 = data.getGroups().get(1);

    // Assert user has no groups
    getGroupsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Group.class)
        .isEmpty();

    // Add group0 to user and assert response is a user
    addGroupsToUserPostRequestAnd(user0, newArrayList(group0))
        .assertEntityOfType(User.class)
        .isEqualToIgnoringGivenFields(user0, USERPERMISSIONS, TOKENS, USERGROUPS, APPLICATIONS);

    // Get groups for user and assert they are associated only to group0
    getGroupsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Group.class)
        .containsExactlyInAnyOrderElementsOf(newArrayList(group0));

    // Add group0 and group1 to user and assert CONFLICT
    addGroupsToUserPostRequestAnd(user0, newArrayList(group0, group1)).assertConflict();
	}

	@Test
	public void removeGroupsFromUser_AllExistingAssociatedGroups_Success(){
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Add groups to user and assert response is a user
    addGroupsToUserPostRequestAnd(user0, data.getGroups())
        .assertEntityOfType(User.class)
        .isEqualToIgnoringGivenFields(user0, USERPERMISSIONS, TOKENS, USERGROUPS, APPLICATIONS);

    // Assert groups were added
    getGroupsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Group.class)
        .containsExactlyInAnyOrderElementsOf(data.getGroups());

    // Delete groups from user
    deleteGroupsFromUserDeleteRequestAnd(user0, data.getGroups()).assertOk();

    // Assert user does not have any groups associated
    getGroupsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Group.class)
        .isEmpty();
	}

	@Test
	public void removeGroupsFromUser_AllExistingGroupsButSomeNotAssociated_NotFound(){
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);
    val group0 = data.getGroups().get(0);
    val group1 = data.getGroups().get(1);

    // Assert user has no groups
    getGroupsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Group.class)
        .isEmpty();

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
	public void removeGroupsFromUser_SomeNonExistingGroupsButAllAssociated_NotFound(){
    // Generate data
    val data = generateUniqueTestUserData();
    val user0 = data.getUsers().get(0);

    // Assert no groups for user
    getGroupsForUserGetRequestAnd(user0)
        .assertPageResultsOfType(Group.class)
        .isEmpty();

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
