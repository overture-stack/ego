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
import bio.overture.ego.model.dto.CreateApplicationRequest;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.ApplicationType;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.utils.EntityGenerator;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.NotImplementedException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static bio.overture.ego.model.enums.JavaFields.GROUPS;
import static bio.overture.ego.model.enums.JavaFields.ID;
import static bio.overture.ego.model.enums.JavaFields.USERS;
import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static bio.overture.ego.utils.CollectionUtils.repeatedCallsOf;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentId;
import static bio.overture.ego.utils.EntityGenerator.randomApplicationType;
import static bio.overture.ego.utils.EntityGenerator.randomStatusType;
import static bio.overture.ego.utils.EntityGenerator.randomStringNoSpaces;
import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

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
    // Initial setup of entities (run once
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
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);
    val responseJson = MAPPER.readTree(response.getBody());
    assertThat(responseJson.get("name").asText()).isEqualTo("addApplication_Success");
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
    assertThat(responseStatus1).isEqualTo(HttpStatus.OK);

    val response2 = initStringRequest().endpoint("/applications").body(app2).post();
    val responseStatus2 = response2.getStatusCode();
    assertThat(responseStatus2).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  @SneakyThrows
  public void getApplication_Success() {
		val application = applicationService.getByClientId("111111");
    getApplicationEntityGetRequestAnd(application)
				.assertEntityOfType(Application.class)
				.isEqualToComparingFieldByField(application);
  }

	@Test
	public void getApplications_FindAllQuery_Success(){
  	// Generate data
  	val data = generateUniqueTestApplicationData();

  	// Get total count of applications
  	val totalApplications = (int)applicationService.getRepository().count();

  	// List all applications
  	val actualApps = listApplicationsEndpointAnd()
				.queryParam("offset", 0)
				.queryParam("limit", totalApplications)
				.getAnd()
				.extractPageResults(Application.class);

  	// Assert the generated applications are included in the list
  	assertThat(actualApps).hasSize(totalApplications);
  	assertThat(actualApps).containsAll(data.getApplications());
	}

	@Test
	public void getApplications_FindSomeQuery_Success(){
		throw new NotImplementedException("need to implement the test 'getApplications_FindSomeQuery_Success'");
	}

	@Test
	public void createApplication_NonExisting_Success(){
  	// Create application request
  	val createRequest = CreateApplicationRequest.builder()
				.clientId(randomStringNoSpaces(6))
				.clientSecret(randomStringNoSpaces(6))
        .name(randomStringNoSpaces(6))
				.status(randomStatusType())
        .type(randomApplicationType())
        .build();

  	// Create the application using the request
    val app = createApplicationPostRequestAnd(createRequest)
        .extractOneEntity(Application.class);
		assertThat(app).isEqualToIgnoringGivenFields(createRequest, ID, GROUPS, USERS );

    // Get the application
		getApplicationEntityGetRequestAnd(app)
				.assertEntityOfType(Application.class)
				.isEqualToIgnoringGivenFields(createRequest, ID, GROUPS, USERS);
	}

	@Test
	public void createApplication_NameAlreadyExists_Conflict(){
		// Create application request
		val createRequest = CreateApplicationRequest.builder()
				.clientId(randomStringNoSpaces(6))
				.clientSecret(randomStringNoSpaces(6))
				.name(randomStringNoSpaces(6))
				.status(randomStatusType())
				.type(randomApplicationType())
				.build();

		// Create the application using the request
		val expectedApp = createApplicationPostRequestAnd(createRequest)
				.extractOneEntity(Application.class);

		// Assert app exists
		getApplicationEntityGetRequestAnd(expectedApp).assertOk();

		// Create another create request with the same name
		val createRequest2 = CreateApplicationRequest.builder()
				.clientId(randomStringNoSpaces(6))
				.clientSecret(randomStringNoSpaces(6))
				.name(createRequest.getName())
				.status(randomStatusType())
				.type(randomApplicationType())
				.build();

		// Assert that creating an application with an existing name, results in a CONFLICT
		createApplicationPostRequestAnd(createRequest2).assertConflict();
	}

	@Test
	public void deleteApplication_NonExisting_NotFound(){
  	// Create an non-existing application Id
    val nonExistentId = generateNonExistentId(applicationService);

    // Assert that deleting a non-existing applicationId results in NOT_FOUND error
    deleteApplicationDeleteRequestAnd(nonExistentId).assertNotFound();
	}

	@Test
	public void deleteApplicationAndRelationshipsOnly_AlreadyExisting_Success(){
  	// Generate data
    val data = generateUniqueTestApplicationData();
    val group0 = data.getGroups().get(0);
    val app0 =  data.getApplications().get(0);
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

		// Delete App0
		deleteApplicationDeleteRequestAnd(app0).assertOk();

		// Assert app0 was deleted
		getApplicationEntityGetRequestAnd(app0).assertNotFound();

		// Assert user0 still exists
		getUserEntityGetRequestAnd(user0).assertOk();

		// Assert group0 still exists
		getGroupEntityGetRequestAnd(group0).assertOk();

		// Assert user0 is associated with 0 applications
		getApplicationsForUserGetRequestAnd(user0)
				.assertPageResultsOfType(Application.class)
				.isEmpty();

		// Assert group0 is associated with 0 applications
		getApplicationsForGroupGetRequestAnd(group0)
				.assertPageResultsOfType(Group.class)
				.isEmpty();
	}

	@Test
	public void getApplication_ExistingApplication_Success(){
		throw new NotImplementedException("need to implement the test 'getApplication_ExistingApplication_Success'");
	}

	@Test
	public void getApplication_NonExistentApplication_NotFound(){
		throw new NotImplementedException("need to implement the test 'getApplication_NonExistentApplication_NotFound'");
	}

	@Test
	public void UUIDValidation_MalformedUUID_Conflict(){
		throw new NotImplementedException("need to implement the test 'UUIDValidation_MalformedUUID_Conflict'");
	}

	@Test
	public void updateApplication_ExistingApplication_Success(){
		throw new NotImplementedException("need to implement the test 'updateApplication_ExistingApplication_Success'");
	}

	@Test
	public void updateApplication_NonExistentApplication_NotFound(){
		throw new NotImplementedException("need to implement the test 'updateApplication_NonExistentApplication_NotFound'");
	}

	@Test
	public void updateApplication_NameAlreadyExists_Conflict(){
		throw new NotImplementedException("need to implement the test 'updateApplication_NameAlreadyExists_Conflict'");
	}

	@Test
	public void statusValidation_MalformedStatus_Conflict(){
		throw new NotImplementedException("need to implement the test 'statusValidation_MalformedStatus_Conflict'");
	}

	@Test
	public void getGroupsFromApplication_FindAllQuery_Success(){
		throw new NotImplementedException("need to implement the test 'getGroupsFromApplication_FindAllQuery_Success'");
	}

	@Test
	public void getGroupsFromApplication_NonExistentGroup_NotFound(){
		throw new NotImplementedException("need to implement the test 'getGroupsFromApplication_NonExistentGroup_NotFound'");
	}

	@Test
	public void getUsersFromApplication_FindAllQuery_Success(){
		throw new NotImplementedException("need to implement the test 'getUsersFromApplication_FindAllQuery_Success'");
	}

	@Test
	public void getUsersFromApplication_NonExistentGroup_NotFound(){
		throw new NotImplementedException("need to implement the test 'getUsersFromApplication_NonExistentGroup_NotFound'");
	}

	@SneakyThrows
	private TestApplicationData generateUniqueTestApplicationData() {
		val applications = repeatedCallsOf(() -> entityGenerator.generateRandomApplication(), 2);
		val groups = repeatedCallsOf(() -> entityGenerator.generateRandomGroup(), 3);
		val users = repeatedCallsOf(() -> entityGenerator.generateRandomUser(), 3);

		return TestApplicationData.builder()
				.applications(applications)
				.users(users)
        .groups(groups)
				.build();
	}

	@lombok.Value
	@Builder
	public static class TestApplicationData {
		@NonNull private final List<Group> groups;
		@NonNull private final List<Application> applications;
		@NonNull private final List<User> users;
	}

}
