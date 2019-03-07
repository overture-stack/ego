package bio.overture.ego.service;

import bio.overture.ego.controller.resolver.PageableResolver;
import bio.overture.ego.model.dto.CreateApplicationRequest;
import bio.overture.ego.model.dto.UpdateApplicationRequest;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.enums.ApplicationStatus;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.model.exceptions.UniqueViolationException;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.ApplicationRepository;
import bio.overture.ego.token.app.AppTokenClaims;
import bio.overture.ego.utils.EntityGenerator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.IntStream;

import static bio.overture.ego.service.ApplicationService.APPLICATION_CONVERTER;
import static bio.overture.ego.utils.CollectionUtils.setOf;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentId;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
@Ignore("replace with controller tests.")
public class ApplicationServiceTest {

  @Autowired private ApplicationService applicationService;
  @Autowired private ApplicationRepository applicationRepository;

  @Autowired private UserService userService;

  @Autowired private GroupService groupService;

  @Autowired private EntityGenerator entityGenerator;

  @Test
  public void applicationConversion_UpdateApplicationRequest_Application() {
    val id = randomUUID();
    val clientId = randomUUID().toString();
    val clientSecret = randomUUID().toString();
    val name = randomUUID().toString();
    val status = ApplicationStatus.PENDING.toString();

    val groups =
        IntStream.range(0, 3)
            .boxed()
            .map(x -> Group.builder().id(randomUUID()).build())
            .collect(toImmutableSet());

    val app =
        Application.builder()
            .id(id)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .name(name)
            .groups(groups)
            .status(status)
            .redirectUri(null)
            .users(null)
            .build();

    val newName = randomUUID().toString();
    assertThat(newName).isNotEqualTo(name);
    val partialAppUpdateRequest =
        UpdateApplicationRequest.builder()
            .name(newName)
            .status(ApplicationStatus.APPROVED.toString())
            .redirectUri(randomUUID().toString())
            .build();
    APPLICATION_CONVERTER.updateApplication(partialAppUpdateRequest, app);

    assertThat(app.getDescription()).isNull();
    assertThat(app.getGroups()).containsExactlyInAnyOrderElementsOf(groups);
    assertThat(app.getClientSecret()).isEqualTo(clientSecret);
    assertThat(app.getClientId()).isEqualTo(clientId);
    assertThat(app.getRedirectUri()).isNotNull();
    assertThat(app.getStatus()).isEqualTo(ApplicationStatus.APPROVED.toString());
    assertThat(app.getId()).isEqualTo(id);
    assertThat(app.getName()).isEqualTo(newName);
    assertThat(app.getUsers()).isNull();
  }

  @Test
  public void applicationConversion_CreateApplicationRequest_Application() {
    val req =
        CreateApplicationRequest.builder()
            .status(ApplicationStatus.PENDING.toString())
            .clientSecret(randomUUID().toString())
            .clientId(randomUUID().toString())
            .name(randomUUID().toString())
            .redirectUri("")
            .build();
    val app = APPLICATION_CONVERTER.convertToApplication(req);
    assertThat(app.getId()).isNull();
    assertThat(app.getGroups()).isEmpty();
    assertThat(app.getClientId()).isEqualTo(req.getClientId());
    assertThat(app.getName()).isEqualTo(req.getName());
    assertThat(app.getUsers()).isEmpty();
    assertThat(app.getClientSecret()).isEqualTo(req.getClientSecret());
    assertThat(app.getStatus()).isEqualTo(req.getStatus());
    assertThat(app.getDescription()).isNull();
    assertThat(app.getRedirectUri()).isEqualTo("");
    assertThat(app.getGroups()).isEmpty();
  }

  // Create
  @Test
  public void testCreate() {
    val application = entityGenerator.setupApplication("123456");
    assertThat(application.getClientId()).isEqualTo("123456");
  }

  // Get
  @Test
  public void testGet() {
    val application = entityGenerator.setupApplication("123456");
    val savedApplication = applicationService.getById(application.getId());
    assertThat(savedApplication.getClientId()).isEqualTo("123456");
  }

  @Test
  public void testGetEntityNotFoundException() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> applicationService.getById(randomUUID()));
  }

  @Test
  public void testGetByName() {
    entityGenerator.setupApplication("123456");
    val savedApplication = applicationService.getByName("Application 123456");
    assertThat(savedApplication.getClientId()).isEqualTo("123456");
  }

  @Test
  public void testGetByNameAllCaps() {
    entityGenerator.setupApplication("123456");
    val savedApplication = applicationService.getByName("APPLICATION 123456");
    assertThat(savedApplication.getClientId()).isEqualTo("123456");
  }

  @Test
  @Ignore
  public void testGetByNameNotFound() {
    // TODO Currently returning null, should throw exception (EntityNotFoundException?)
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> applicationService.getByName("Application 123456"));
  }

  @Test
  public void testGetByClientId() {
    entityGenerator.setupApplication("123456");
    val savedApplication = applicationService.getByClientId("123456");
    assertThat(savedApplication.getClientId()).isEqualTo("123456");
  }

  @Test
  @Ignore
  public void testGetByClientIdNotFound() {
    // TODO Currently returning null, should throw exception (EntityNotFoundException?)
    assertThatExceptionOfType(EntityNotFoundException.class)
        .isThrownBy(() -> applicationService.getByClientId("123456"));
  }

  // List
  @Test
  public void testListAppsNoFilters() {
    val expectedApplications = newArrayList(applicationRepository.findAll());
    val actualApplicationsPage =
        applicationService.listApps(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(actualApplicationsPage.getTotalElements()).isEqualTo(expectedApplications.size());
    assertThat(actualApplicationsPage.getContent())
        .containsExactlyInAnyOrderElementsOf(expectedApplications);
  }

  @Test
  public void testListAppsFiltered() {
    entityGenerator.setupTestApplications();
    val clientIdFilter = new SearchFilter("clientId", "333333");
    val applications =
        applicationService.listApps(
            singletonList(clientIdFilter), new PageableResolver().getPageable());
    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("333333");
  }

  @Test
  public void testListAppsFilteredEmptyResult() {
    entityGenerator.setupTestApplications();
    val clientIdFilter = new SearchFilter("clientId", "666666");
    val applications =
        applicationService.listApps(
            singletonList(clientIdFilter), new PageableResolver().getPageable());
    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  // Find
  @Test
  public void testFindAppsNoFilters() {
    entityGenerator.setupTestApplications();
    val applications =
        applicationService.findApps(
            "222222", Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("222222");
  }

  @Test
  public void testFindAppsFiltered() {
    entityGenerator.setupTestApplications();
    val clientIdFilter = new SearchFilter("clientId", "333333");
    val applications =
        applicationService.findApps(
            "222222", singletonList(clientIdFilter), new PageableResolver().getPageable());
    // Expect empty list
    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindUsersAppsNoQueryNoFilters() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestUsers();

    val user = userService.getByName("FirstUser@domain.com");
    val userTwo = userService.getByName("SecondUser@domain.com");

    val application = applicationService.getByClientId("444444");

    userService.addUserToApps(
        user.getId(),newArrayList(application.getId()));
    userService.addUserToApps(
        userTwo.getId(), newArrayList(application.getId()));

    val applications =
        applicationService.findUserApps(
            user.getId(), Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("444444");
  }

  @Test
  public void testFindUsersAppsNoQueryNoFiltersNoUser() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestUsers();

    val user = userService.getByName("FirstUser@domain.com");
    val applications =
        applicationService.findUserApps(
            user.getId(), Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindUsersAppsNoQueryFilters() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestUsers();

    val user = userService.getByName("FirstUser@domain.com");
    val applicationOne = applicationService.getByClientId("111111");
    val applicationTwo = applicationService.getByClientId("555555");

    userService.addUserToApps(
        user.getId(),
        newArrayList(applicationOne.getId(), applicationTwo.getId()));

    val clientIdFilter = new SearchFilter("clientId", "111111");

    val applications =
        applicationService.findUserApps(
            user.getId(),
            singletonList(clientIdFilter),
            new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("111111");
  }

  @Test
  public void testFindUsersAppsQueryAndFilters() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestUsers();

    val user = userService.getByName("FirstUser@domain.com");
    val applicationOne = applicationService.getByClientId("333333");
    val applicationTwo = applicationService.getByClientId("444444");

    userService.addUserToApps(
        user.getId(),
        newArrayList(applicationOne.getId(), applicationTwo.getId()));

    val clientIdFilter = new SearchFilter("clientId", "333333");

    val applications =
        applicationService.findUserApps(
            user.getId(),
            "444444",
            singletonList(clientIdFilter),
            new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindUsersAppsQueryNoFilters() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestUsers();

    val user = userService.getByName("FirstUser@domain.com");
    val applicationOne = applicationService.getByClientId("222222");
    val applicationTwo = applicationService.getByClientId("444444");

    userService.addUserToApps(
        user.getId(),
        newArrayList(applicationOne.getId(), applicationTwo.getId()));

    val applications =
        applicationService.findUserApps(
            user.getId(),
            "222222",
            Collections.emptyList(),
            new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("222222");
  }

  @Test
  public void testFindGroupsAppsNoQueryNoFilters() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group One");
    val groupTwo = groupService.getByName("Group Two");

    val application = applicationService.getByClientId("111111");

    group.getApplications().add(application);
    groupTwo.getApplications().add(application);

    val applications =
        applicationService.findGroupApplications(
            group.getId(),
            Collections.emptyList(),
            new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("111111");
  }

  @Test
  public void testFindGroupsAppsNoQueryNoFiltersNoGroup() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group One");
    val applications =
        applicationService.findGroupApplications(
            group.getId(),
            Collections.emptyList(),
            new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindGroupsAppsNoQueryFilters() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group One");
    val applicationOne = applicationService.getByClientId("222222");
    val applicationTwo = applicationService.getByClientId("333333");

    group.getApplications().add(applicationOne);
    group.getApplications().add(applicationTwo);

    val clientIdFilter = new SearchFilter("clientId", "333333");

    val applications =
        applicationService.findGroupApplications(
            group.getId(),
            singletonList(clientIdFilter),
            new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("333333");
  }

  @Test
  public void testFindGroupsAppsQueryAndFilters() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group Three");
    val applicationOne = applicationService.getByClientId("333333");
    val applicationTwo = applicationService.getByClientId("444444");

    group.getApplications().add(applicationOne);
    group.getApplications().add(applicationTwo);

    val clientIdFilter = new SearchFilter("clientId", "333333");

    val applications =
        applicationService.findGroupApplications(
            group.getId(),
            "444444",
            singletonList(clientIdFilter),
            new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindGroupsAppsQueryNoFilters() {
    entityGenerator.setupTestApplications();
    entityGenerator.setupTestGroups();

    val group = groupService.getByName("Group One");
    val applicationOne = applicationService.getByClientId("444444");
    val applicationTwo = applicationService.getByClientId("555555");

    group.getApplications().add(applicationOne);
    group.getApplications().add(applicationTwo);

    val applications =
        applicationService.findGroupApplications(
            group.getId(),
            "555555",
            Collections.emptyList(),
            new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("555555");
  }

  // Update
  @Test
  public void testUpdate() {
    val application = entityGenerator.setupApplication("123456");
    val updateRequest = UpdateApplicationRequest.builder().name("New Name").build();
    val updated = applicationService.partialUpdate(application.getId(), updateRequest);
    assertThat(updated.getName()).isEqualTo("New Name");
  }

  @Test
  public void testUpdateNonexistentEntity() {
    val nonExistentId = generateNonExistentId(applicationService);
    val updateRequest =
        UpdateApplicationRequest.builder()
            .clientId("123456")
            .name("DoesNotExist")
            .clientSecret("654321")
            .build();
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> applicationService.partialUpdate(nonExistentId, updateRequest));
  }

  @Test
  public void uniqueClientIdCheck_CreateApplication_ThrowsUniqueConstraintException() {
    val r1 =
        CreateApplicationRequest.builder()
            .clientId(UUID.randomUUID().toString())
            .clientSecret(UUID.randomUUID().toString())
            .name(UUID.randomUUID().toString())
            .status("Pending")
            .build();

    val a1 = applicationService.create(r1);
    assertThat(applicationService.isExist(a1.getId())).isTrue();

    assertThat(a1.getClientId()).isEqualTo(r1.getClientId());
    assertThatExceptionOfType(UniqueViolationException.class)
        .isThrownBy(() -> applicationService.create(r1));
  }

  @Test
  public void uniqueClientIdCheck_UpdateApplication_ThrowsUniqueConstraintException() {
    val clientId1 = UUID.randomUUID().toString();
    val clientId2 = UUID.randomUUID().toString();
    val cr1 =
        CreateApplicationRequest.builder()
            .clientId(clientId1)
            .clientSecret(UUID.randomUUID().toString())
            .name(UUID.randomUUID().toString())
            .status("Pending")
            .build();

    val cr2 =
        CreateApplicationRequest.builder()
            .clientId(clientId2)
            .clientSecret(UUID.randomUUID().toString())
            .name(UUID.randomUUID().toString())
            .status("Approved")
            .build();

    val a1 = applicationService.create(cr1);
    assertThat(applicationService.isExist(a1.getId())).isTrue();
    val a2 = applicationService.create(cr2);
    assertThat(applicationService.isExist(a2.getId())).isTrue();

    val ur3 = UpdateApplicationRequest.builder().clientId(clientId1).build();

    assertThat(a1.getClientId()).isEqualTo(ur3.getClientId());
    assertThat(a2.getClientId()).isNotEqualTo(ur3.getClientId());
    assertThatExceptionOfType(UniqueViolationException.class)
        .isThrownBy(() -> applicationService.partialUpdate(a2.getId(), ur3));
  }

  @Test
  @Ignore
  public void testUpdateClientIdNotAllowed() {
    //    entityGenerator.setupTestApplications();
    //    val application = applicationService.getByClientId("111111");
    //    application.setClientId("222222");
    //    val updated = applicationService.update(application);
    assertThat(1).isEqualTo(2);
    // TODO Check for uniqueness in application, currently only SQL
  }

  @Test
  @Ignore
  public void testUpdateStatusNotInAllowedEnum() {
    //    entityGenerator.setupTestApplications();
    //    val application = applicationService.getByClientId("111111");
    //    application.setStatus("Junk");
    //    val updated = applicationService.update(application);
    assertThat(1).isEqualTo(2);
    // TODO Check for uniqueness in application, currently only SQL
  }

  // Delete
  @Test
  public void testDelete() {
    entityGenerator.setupTestApplications();

    val application = applicationService.getByClientId("222222");
    applicationService.delete(application.getId());

    val applications =
        applicationService.listApps(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(applications.getTotalElements()).isEqualTo(4L);
    assertThat(applications.getContent()).doesNotContain(application);
  }

  @Test
  public void testDeleteNonExisting() {
    entityGenerator.setupTestApplications();
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> applicationService.delete(randomUUID()));
  }

  // Special (LoadClient)
  @Test
  public void testLoadClientByClientId() {
    val application = entityGenerator.setupApplication("123456");
    val updateRequest =
        UpdateApplicationRequest.builder().status(ApplicationStatus.APPROVED.toString()).build();
    applicationService.partialUpdate(application.getId(), updateRequest);

    val client = applicationService.loadClientByClientId("123456");

    assertThat(client.getClientId()).isEqualToIgnoringCase("123456");
    assertThat(
        client
            .getAuthorizedGrantTypes()
            .containsAll(Arrays.asList(AppTokenClaims.AUTHORIZED_GRANTS)));
    assertThat(client.getScope().containsAll(Arrays.asList(AppTokenClaims.SCOPES)));
    assertThat(client.getRegisteredRedirectUri()).isEqualTo(setOf(application.getRedirectUri()));
    assertThat(client.getAuthorities())
        .containsExactly(new SimpleGrantedAuthority(AppTokenClaims.ROLE));
  }

  @Test
  public void testLoadClientByClientIdNotFound() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> applicationService.loadClientByClientId("123456"))
        .withMessage("The 'Application' entity with clientId '123456' was not found");
  }

  @Test
  public void testLoadClientByClientIdEmptyString() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> applicationService.loadClientByClientId(""))
        .withMessage("The 'Application' entity with clientId '' was not found");
  }

  @Test
  public void testLoadClientByClientIdNotApproved() {
    val application = entityGenerator.setupApplication("123456");
    val updateRequest = UpdateApplicationRequest.builder().status("Pending").build();
    applicationService.partialUpdate(application.getId(), updateRequest);
    assertThatExceptionOfType(ClientRegistrationException.class)
        .isThrownBy(() -> applicationService.loadClientByClientId("123456"))
        .withMessage("Client Access is not approved.");

    updateRequest.setStatus("Rejected");
    applicationService.partialUpdate(application.getId(), updateRequest);
    assertThatExceptionOfType(ClientRegistrationException.class)
        .isThrownBy(() -> applicationService.loadClientByClientId("123456"))
        .withMessage("Client Access is not approved.");

    updateRequest.setStatus("Disabled");
    applicationService.partialUpdate(application.getId(), updateRequest);
    assertThatExceptionOfType(ClientRegistrationException.class)
        .isThrownBy(() -> applicationService.loadClientByClientId("123456"))
        .withMessage("Client Access is not approved.");
  }
}
