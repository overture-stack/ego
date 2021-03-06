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
import static bio.overture.ego.model.enums.AccessLevel.READ;
import static bio.overture.ego.model.enums.AccessLevel.WRITE;
import static bio.overture.ego.model.enums.JavaFields.*;
import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static bio.overture.ego.utils.CollectionUtils.repeatedCallsOf;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentClientId;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentId;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentName;
import static bio.overture.ego.utils.EntityGenerator.randomApplicationType;
import static bio.overture.ego.utils.EntityGenerator.randomEnum;
import static bio.overture.ego.utils.EntityGenerator.randomEnumExcluding;
import static bio.overture.ego.utils.EntityGenerator.randomStatusType;
import static bio.overture.ego.utils.EntityGenerator.randomStringNoSpaces;
import static bio.overture.ego.utils.EntityGenerator.randomStringWithSpaces;
import static bio.overture.ego.utils.Streams.stream;
import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.*;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.CreateApplicationRequest;
import bio.overture.ego.model.dto.UpdateApplicationRequest;
import bio.overture.ego.model.entity.*;
import bio.overture.ego.model.enums.ApplicationType;
import bio.overture.ego.model.enums.StatusType;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.utils.EntityGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
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

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApplicationControllerTest extends AbstractControllerTest {

  private static boolean hasRunEntitySetup = false;

  /** Dependencies */
  @Autowired private EntityGenerator entityGenerator;

  @Autowired private ApplicationService applicationService;

  @Value("${logging.test.controller.enable}")
  private boolean enableLogging;

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

  @Override
  protected boolean enableLogging() {
    return enableLogging;
  }

  @Test
  @SneakyThrows
  public void addApplication_Success() {
    val app =
        Application.builder()
            .name("addApplication_Success")
            .clientId("addApplication_Success")
            .clientSecret("addApplication_Success")
            .redirectUri("http://example.com")
            .status(APPROVED)
            .type(ApplicationType.CLIENT)
            .build();

    val response = initStringRequest().endpoint("/applications").body(app).post();

    val responseStatus = response.getStatusCode();
    assertEquals(responseStatus, HttpStatus.OK);
    val responseJson = MAPPER.readTree(response.getBody());
    assertEquals(responseJson.get("name").asText(), "addApplication_Success");
  }

  @Test
  @SneakyThrows
  public void addDuplicateApplication_Conflict() {
    val app1 =
        Application.builder()
            .name("addDuplicateApplication")
            .clientId("addDuplicateApplication")
            .clientSecret("addDuplicateApplication")
            .redirectUri("http://example.com")
            .status(APPROVED)
            .type(ApplicationType.CLIENT)
            .build();

    val app2 =
        Application.builder()
            .name("addDuplicateApplication")
            .clientId("addDuplicateApplication")
            .clientSecret("addDuplicateApplication")
            .redirectUri("http://example.com")
            .status(APPROVED)
            .type(ApplicationType.CLIENT)
            .build();

    val response1 = initStringRequest().endpoint("/applications").body(app1).post();

    val responseStatus1 = response1.getStatusCode();
    assertEquals(responseStatus1, HttpStatus.OK);

    val response2 = initStringRequest().endpoint("/applications").body(app2).post();
    val responseStatus2 = response2.getStatusCode();
    assertEquals(responseStatus2, HttpStatus.CONFLICT);
  }

  @Test
  @SneakyThrows
  public void getApplication_Success() {
    val application = applicationService.getByClientId("111111");
    getApplicationEntityGetRequestAnd(application)
        .assertEntityOfType(Application.class)
        .isEqualToIgnoringGivenFields(application, GROUPAPPLICATIONS, USERAPPLICATIONS);
  }

  @Test
  public void findApplications_FindAllQuery_Success() {
    // Generate data
    val data = generateUniqueTestApplicationData();

    // Get total count of applications
    val totalApplications = (int) applicationService.getRepository().count();

    // List all applications
    val actualApps =
        listApplicationsEndpointAnd()
            .queryParam("offset", 0)
            .queryParam("limit", totalApplications)
            .getAnd()
            .extractPageResults(Application.class);

    // Assert the generated applications are included in the list
    assertEquals(actualApps.size(), totalApplications);
    assertTrue(actualApps.containsAll(data.getApplications()));
  }

  @Test
  @Ignore("Should be tested")
  public void findApplications_FindSomeQuery_Success() {
    throw new NotImplementedException(
        "need to implement the test 'getApplications_FindSomeQuery_Success'");
  }

  @Test
  @Ignore("Should be tested")
  public void getGroupsFromApplication_FindSomeQuery_Success() {
    throw new NotImplementedException(
        "need to implement the test 'getGroupsFromApplication_FindSomeQuery_Success'");
  }

  @Test
  @Ignore("Should be tested")
  public void getUsersFromApplication_FindSomeQuery_Success() {
    throw new NotImplementedException(
        "need to implement the test 'getUsersFromApplication_FindSomeQuery_Success'");
  }

  @Test
  public void createApplication_NullValuesForRequiredFields_BadRequest() {
    // Create with null values
    val r1 = CreateApplicationRequest.builder().build();

    // Assert that a bad request is returned
    createApplicationPostRequestAnd(r1).assertBadRequest();
  }

  @Test
  public void createApplication_NonExisting_Success() {
    // Create application request
    val createRequest =
        CreateApplicationRequest.builder()
            .clientId(randomStringNoSpaces(6))
            .clientSecret(randomStringNoSpaces(6))
            .name(randomStringNoSpaces(6))
            .status(randomStatusType())
            .type(randomApplicationType())
            .build();

    // Create the application using the request
    val app = createApplicationPostRequestAnd(createRequest).extractOneEntity(Application.class);
    assertEquals(app.getClientId(), createRequest.getClientId());
    assertEquals(app.getClientSecret(), createRequest.getClientSecret());
    assertEquals(app.getName(), createRequest.getName());
    assertEquals(app.getType(), createRequest.getType());
    assertEquals(app.getDescription(), createRequest.getDescription());

    // Get the application
    getApplicationEntityGetRequestAnd(app)
        .assertEntityOfType(Application.class)
        .isEqualToIgnoringGivenFields(
            createRequest,
            ID,
            GROUPAPPLICATIONS,
            USERAPPLICATIONS,
            Application.Fields.applicationPermissions);
  }

  @Test
  public void createApplication_NameAlreadyExists_Conflict() {
    val name = generateNonExistentName(applicationService);
    // Create application request
    val createRequest =
        CreateApplicationRequest.builder()
            .clientId(randomStringNoSpaces(6))
            .clientSecret(randomStringNoSpaces(6))
            .name(name)
            .status(randomStatusType())
            .type(randomApplicationType())
            .build();

    // Create the application using the request
    val expectedApp =
        createApplicationPostRequestAnd(createRequest).extractOneEntity(Application.class);

    // Assert app exists
    getApplicationEntityGetRequestAnd(expectedApp).assertOk();

    // Create another create request with the same name
    val createRequest2 =
        CreateApplicationRequest.builder()
            .clientId(randomStringNoSpaces(2))
            .clientSecret(randomStringNoSpaces(6))
            .name(name)
            .status(randomStatusType())
            .type(randomApplicationType())
            .build();

    // Assert that creating an application with an existing name, results in a CONFLICT
    createApplicationPostRequestAnd(createRequest2).assertConflict();
  }

  @Test
  public void createApplication_ClientIdAlreadyExists_Conflict() {
    val clientId = generateNonExistentClientId(applicationService);
    val name1 = generateNonExistentName(applicationService);
    // Create application request
    val createRequest =
        CreateApplicationRequest.builder()
            .clientId(clientId)
            .clientSecret(randomStringNoSpaces(6))
            .name(name1)
            .status(randomStatusType())
            .type(randomApplicationType())
            .build();

    // Create the application using the request
    val expectedApp =
        createApplicationPostRequestAnd(createRequest).extractOneEntity(Application.class);

    // Assert app exists
    getApplicationEntityGetRequestAnd(expectedApp).assertOk();

    val name2 = generateNonExistentName(applicationService);
    // Create another create request with the same name
    val createRequest2 =
        CreateApplicationRequest.builder()
            .clientId(clientId)
            .clientSecret(randomStringNoSpaces(6))
            .name(name2)
            .status(randomStatusType())
            .type(randomApplicationType())
            .build();

    // Assert that creating an application with an existing clientId, results in a CONFLICT
    createApplicationPostRequestAnd(createRequest2).assertConflict();
  }

  @Test
  public void deleteApplication_NonExisting_NotFound() {
    // Create an non-existing application Id
    val nonExistentId = generateNonExistentId(applicationService);

    // Assert that deleting a non-existing applicationId results in NOT_FOUND error
    deleteApplicationDeleteRequestAnd(nonExistentId).assertNotFound();
  }

  @Test
  public void deleteApplicationAndRelationshipsOnly_AlreadyExisting_Success() {
    // Generate data
    val data = generateUniqueTestApplicationData();
    val group0 = data.getGroups().get(0);
    val app0 = data.getApplications().get(0);
    val user0 = data.getUsers().get(0);

    // Add Applications to Group0
    addApplicationsToGroupPostRequestAnd(group0, newArrayList(app0)).assertOk();

    // Assert group0 was added to app0
    getGroupsForApplicationGetRequestAnd(app0)
        .assertPageResultsOfType(Group.class)
        .containsExactly(group0);

    // Add user0 to app0
    addApplicationsToUserPostRequestAnd(user0, newArrayList(app0)).assertOk();

    // Assert user0 was added to app0
    getUsersForApplicationGetRequestAnd(app0)
        .assertPageResultsOfType(User.class)
        .containsExactly(user0);

    addApplicationPermissionToApplicationPostRequestAnd(app0, data.getPolicies().get(0), READ)
        .assertOk();

    addApplicationPermissionToApplicationPostRequestAnd(app0, data.getPolicies().get(1), WRITE)
        .assertOk();

    getApplicationPermissionsForApplicationGetRequestAnd(app0)
        .assertPageResultsOfType(ApplicationPermission.class)
        .hasSize(2);

    // Delete App0
    deleteApplicationDeleteRequestAnd(app0).assertOk();

    // Assert app0 was deleted
    getApplicationEntityGetRequestAnd(app0).assertNotFound();

    // Assert user0 still exists
    getUserEntityGetRequestAnd(user0).assertOk();

    // Assert group0 still exists
    getGroupEntityGetRequestAnd(group0).assertOk();

    // Assert app0 permissions were deleted
    getApplicationPermissionsForApplicationGetRequestAnd(app0).assertNotFound();

    // Assert all policies still exist
    data.getPolicies().forEach(p -> getPolicyGetRequestAnd(p).assertOk());

    // Assert user0 is associated with 0 applications
    getApplicationsForUserGetRequestAnd(user0).assertPageResultsOfType(Application.class).isEmpty();

    // Assert group0 is associated with 0 applications
    getApplicationsForGroupGetRequestAnd(group0).assertPageResultsOfType(Group.class).isEmpty();
  }

  @Test
  public void getApplication_ExistingApplication_Success() {
    val data = generateUniqueTestApplicationData();
    val app0 = data.getApplications().get(0);

    // Assert app0 can be read
    getApplicationEntityGetRequestAnd(app0)
        .assertEntityOfType(Application.class)
        .isEqualToIgnoringGivenFields(app0, GROUPAPPLICATIONS, USERAPPLICATIONS);
  }

  @Test
  public void getApplication_NonExistentApplication_NotFound() {
    // Create non-existing application id
    val nonExistingId = generateNonExistentId(applicationService);

    // Assert that the id cannot be read and throws a NOT_FOUND error
    getApplicationEntityGetRequestAnd(nonExistingId).assertNotFound();
  }

  @Test
  public void UUIDValidation_MalformedUUID_BadRequest() {
    val badUUID = "123sksk";

    initStringRequest().endpoint("/applications/%s", badUUID).deleteAnd().assertBadRequest();

    initStringRequest().endpoint("/applications/%s", badUUID).getAnd().assertBadRequest();

    val dummyUpdateRequest = UpdateApplicationRequest.builder().build();
    initStringRequest()
        .endpoint("/applications/%s", badUUID)
        .body(dummyUpdateRequest)
        .putAnd()
        .assertBadRequest();

    initStringRequest().endpoint("/applications/%s/groups", badUUID).getAnd().assertBadRequest();

    initStringRequest().endpoint("/applications/%s/users", badUUID).getAnd().assertBadRequest();
  }

  private UpdateApplicationRequest randomUpdateApplicationRequest() {
    val name = generateNonExistentName(applicationService);
    return UpdateApplicationRequest.builder()
        .name(name)
        .status(randomEnum(StatusType.class))
        .clientId(randomStringNoSpaces(7))
        .clientSecret(randomStringNoSpaces(7))
        .redirectUri(randomStringNoSpaces(7))
        .description(randomStringWithSpaces(100))
        .type(randomEnum(ApplicationType.class))
        .build();
  }

  @Test
  public void updateApplication_ExistingApplication_Success() {
    // Generate data
    val data = generateUniqueTestApplicationData();
    val app0 = data.getApplications().get(0);

    // Create updateRequest1
    val updateRequest1 =
        UpdateApplicationRequest.builder()
            .name(generateNonExistentName(applicationService))
            .build();
    assertNotEquals(app0.getName(), updateRequest1.getName());

    // Update app0 with updateRequest1, and assert the name changed
    val app0_before0 = getApplicationEntityGetRequestAnd(app0).extractOneEntity(Application.class);
    partialUpdateApplicationPutRequestAnd(app0.getId(), updateRequest1).assertOk();
    val app0_after0 = getApplicationEntityGetRequestAnd(app0).extractOneEntity(Application.class);
    assertEquals(app0_before0.getClientId(), app0_after0.getClientId());
    assertEquals(app0_before0.getClientSecret(), app0_after0.getClientSecret());
    assertNotEquals(app0_before0.getName(), app0_after0.getName());
    assertEquals(app0_before0.getType(), app0_after0.getType());
    assertEquals(app0_before0.getDescription(), app0_after0.getDescription());

    // Update app0 with empty update request, and assert nothing changed
    val app0_before1 = getApplicationEntityGetRequestAnd(app0).extractOneEntity(Application.class);
    partialUpdateApplicationPutRequestAnd(app0.getId(), UpdateApplicationRequest.builder().build())
        .assertOk();
    val app0_after1 = getApplicationEntityGetRequestAnd(app0).extractOneEntity(Application.class);
    assertEquals(app0_before1.getClientId(), app0_after1.getClientId());
    assertEquals(app0_before1.getClientSecret(), app0_after1.getClientSecret());
    assertEquals(app0_before1.getName(), app0_after1.getName());
    assertEquals(app0_before1.getType(), app0_after1.getType());
    assertEquals(app0_before1.getDescription(), app0_after1.getDescription());

    // Update the status field, and assert only that was updated
    val app0_before2 = getApplicationEntityGetRequestAnd(app0).extractOneEntity(Application.class);
    val updateRequest2 =
        UpdateApplicationRequest.builder()
            .status(randomEnumExcluding(StatusType.class, app0_before2.getStatus()))
            .build();
    partialUpdateApplicationPutRequestAnd(app0.getId(), updateRequest2).assertOk();
    val app0_after2 = getApplicationEntityGetRequestAnd(app0).extractOneEntity(Application.class);
    assertEquals(app0_before2.getClientId(), app0_after2.getClientId());
    assertEquals(app0_before2.getClientSecret(), app0_after2.getClientSecret());
    assertEquals(app0_before2.getName(), app0_after2.getName());
    assertEquals(app0_before2.getType(), app0_after2.getType());
    assertEquals(app0_before2.getDescription(), app0_after2.getDescription());
    assertNotEquals(app0_before2.getStatus(), app0_after2.getStatus());
  }

  @Test
  public void updateApplication_NonExistentApplication_NotFound() {
    // Generate a non-existing application Id
    val nonExistentId = generateNonExistentId(applicationService);

    // Assert that updating a non-existing application results in NOT_FOUND error
    partialUpdateApplicationPutRequestAnd(nonExistentId, UpdateApplicationRequest.builder().build())
        .assertNotFound();
  }

  @Test
  public void updateApplication_NameAlreadyExists_Conflict() {
    // Generate data
    val data = generateUniqueTestApplicationData();
    val app0 = data.getApplications().get(0);
    val app1 = data.getApplications().get(1);

    // Create update request with the same name as app1
    val updateRequest = UpdateApplicationRequest.builder().name(app1.getName()).build();

    // Update app0 with the same name as app1, and assert a CONFLICT
    partialUpdateApplicationPutRequestAnd(app0.getId(), updateRequest).assertConflict();
  }

  @Test
  public void updateApplication_ClientIdAlreadyExists_Conflict() {
    // Generate data
    val data = generateUniqueTestApplicationData();
    val app0 = data.getApplications().get(0);
    val app1 = data.getApplications().get(1);

    // Create update request with the same name as app1
    val updateRequest = UpdateApplicationRequest.builder().clientId(app1.getClientId()).build();

    // Update app0 with the same clientId as app1, and assert a CONFLICT
    partialUpdateApplicationPutRequestAnd(app0.getId(), updateRequest).assertConflict();
  }

  @Test
  public void statusValidation_MalformedStatus_BadRequest() {
    // Assert the invalid status is actually invalid
    val invalidStatus = "something123";
    val match = stream(StatusType.values()).anyMatch(x -> x.toString().equals(invalidStatus));
    assertFalse(match);

    // Generate data
    val data = generateUniqueTestApplicationData();
    val app0 = data.getApplications().get(0);

    // Build application create request with invalid status
    val createRequest =
        CreateApplicationRequest.builder()
            .name(randomStringNoSpaces(7))
            .clientId(randomStringNoSpaces(7))
            .clientSecret(randomStringNoSpaces(7))
            .type(randomEnum(ApplicationType.class))
            .build();
    val createRequestJson = (ObjectNode) MAPPER.valueToTree(createRequest);
    createRequestJson.put("status", invalidStatus);

    // Create application with invalid request, and assert BAD_REQUEST
    initStringRequest()
        .endpoint("/applications")
        .body(createRequestJson)
        .postAnd()
        .assertBadRequest();

    // Build application update request with invalid status
    val updateRequestJson = MAPPER.createObjectNode().put("status", invalidStatus);
    initStringRequest()
        .endpoint("/applications/%s", app0.getId())
        .body(updateRequestJson)
        .putAnd()
        .assertBadRequest();
  }

  @Test
  public void applicationTypeValidation_MalformedApplicationType_BadRequest() {
    // Assert the invalid status is actually invalid
    val invalidApplicationType = "something123";
    val match =
        stream(ApplicationType.values()).anyMatch(x -> x.toString().equals(invalidApplicationType));
    assertFalse(match);

    // Generate data
    val data = generateUniqueTestApplicationData();
    val app0 = data.getApplications().get(0);

    // Build application create request with invalid application Type
    val createRequest =
        CreateApplicationRequest.builder()
            .name(randomStringNoSpaces(7))
            .clientId(randomStringNoSpaces(7))
            .clientSecret(randomStringNoSpaces(7))
            .status(randomEnum(StatusType.class))
            .build();
    val createRequestJson = (ObjectNode) MAPPER.valueToTree(createRequest);
    createRequestJson.put("type", invalidApplicationType);

    // Create application with invalid request, and assert BAD_REQUEST
    initStringRequest()
        .endpoint("/applications")
        .body(createRequestJson)
        .postAnd()
        .assertBadRequest();

    // Build application update request with invalid status
    val updateRequestJson = MAPPER.createObjectNode().put("type", invalidApplicationType);
    initStringRequest()
        .endpoint("/applications/%s", app0.getId())
        .body(updateRequestJson)
        .putAnd()
        .assertBadRequest();
  }

  @Test
  public void getGroupsFromApplication_FindAllQuery_Success() {
    // Generate data
    val data = generateUniqueTestApplicationData();
    val app0 = data.getApplications().get(0);
    val groups = data.getGroups();

    // Add groups to app0
    groups.forEach(g -> addApplicationsToGroupPostRequestAnd(g, newArrayList(app0)).assertOk());

    // Assert all associated groups with the application can be read
    getGroupsForApplicationEndpoint(app0.getId())
        .queryParam(OFFSET, 0)
        .queryParam(LIMIT, groups.size() + 100)
        .getAnd()
        .assertPageResultsOfType(Group.class)
        .containsExactlyInAnyOrderElementsOf(groups);
  }

  @Test
  public void getGroupsFromApplication_NonExistentApplication_NotFound() {
    // Generate non existing application id
    val nonExistentId = generateNonExistentId(applicationService);

    // Read non existing application id and assert its NOT_FOUND
    getGroupsForApplicationGetRequestAnd(nonExistentId).assertNotFound();
  }

  @Test
  public void getUsersFromApplication_FindAllQuery_Success() {
    // Generate data
    val data = generateUniqueTestApplicationData();
    val app0 = data.getApplications().get(0);
    val users = data.getUsers();

    // Add users to app0
    users.forEach(u -> addApplicationsToUserPostRequestAnd(u, newArrayList(app0)).assertOk());

    // Assert all associated users with the application can be read
    getUsersForApplicationEndpoint(app0.getId())
        .queryParam(OFFSET, 0)
        .queryParam(LIMIT, users.size() + 100)
        .getAnd()
        .assertPageResultsOfType(User.class)
        .containsExactlyInAnyOrderElementsOf(users);
  }

  @Test
  public void getUsersFromApplication_NonExistentGroup_NotFound() {
    // Generate non existing application id
    val nonExistentId = generateNonExistentId(applicationService);

    // Read non existing application id and assert its NOT_FOUND
    getUsersForApplicationGetRequestAnd(nonExistentId).assertNotFound();
  }

  @SneakyThrows
  private TestApplicationData generateUniqueTestApplicationData() {
    val applications = repeatedCallsOf(() -> entityGenerator.generateRandomApplication(), 2);
    val groups = repeatedCallsOf(() -> entityGenerator.generateRandomGroup(), 3);
    val users = repeatedCallsOf(() -> entityGenerator.generateRandomUser(), 3);
    val policies = repeatedCallsOf(() -> entityGenerator.generateRandomPolicy(), 2);

    return TestApplicationData.builder()
        .applications(applications)
        .users(users)
        .groups(groups)
        .policies(policies)
        .build();
  }

  @lombok.Value
  @Builder
  public static class TestApplicationData {
    @NonNull private final List<Group> groups;
    @NonNull private final List<Application> applications;
    @NonNull private final List<User> users;
    @NonNull private final List<Policy> policies;
  }
}
