package org.overture.ego.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.overture.ego.controller.resolver.PageableResolver;
import org.overture.ego.model.entity.Application;
import org.overture.ego.model.search.SearchFilter;
import org.overture.ego.token.app.AppTokenClaims;
import org.overture.ego.utils.EntityGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class ApplicationServiceTest {

  @Autowired
  private ApplicationService applicationService;

  @Autowired
  private UserService userService;

  @Autowired
  private GroupService groupService;

  @Autowired
  private EntityGenerator entityGenerator;

  @Test
  public void testCreate() {
    val application = applicationService.create(entityGenerator.createOneApplication("123456"));
    assertThat(application.getClientId()).isEqualTo("123456");
  }

  @Test
  public void testCreateUniqueClientId() {
    applicationService.create(entityGenerator.createOneApplication("111111"));
    applicationService.create(entityGenerator.createOneApplication("222222"));
    assertThatExceptionOfType(DataIntegrityViolationException.class)
        .isThrownBy(() -> applicationService.create(entityGenerator.createOneApplication("111111")));
  }

  @Test
  public void testGet() {
    val application = applicationService.create(entityGenerator.createOneApplication("123456"));
    val savedApplication = applicationService.get(Integer.toString(application.getId()));
    assertThat(savedApplication.getClientId()).isEqualTo("123456");
  }

  @Test
  public void testGetEntityNotFoundException() {
    assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> applicationService.get("1"));
  }

  @Test
  public void testGetByName() {
    applicationService.create(entityGenerator.createOneApplication("123456"));
    val savedApplication = applicationService.getByName("Application 123456");
    assertThat(savedApplication.getClientId()).isEqualTo("123456");
  }

  @Test
  public void testGetByNameAllCaps() {
    applicationService.create(entityGenerator.createOneApplication("123456"));
    val savedApplication = applicationService.getByName("APPLICATION 123456");
    assertThat(savedApplication.getClientId()).isEqualTo("123456");
  }

  @Test
  public void testGetByNameNotFound() {
    // TODO Currently returning null, should throw exception (EntityNotFoundException?)
    assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> applicationService.getByName("Application 123456"));
  }

  @Test
  public void testGetByClientId() {
    applicationService.create(entityGenerator.createOneApplication("123456"));
    val savedApplication = applicationService.getByClientId("123456");
    assertThat(savedApplication.getClientId()).isEqualTo("123456");
  }

  @Test
  public void testGetByClientIdNotFound() {
    // TODO Currently returning null, should throw exception (EntityNotFoundException?)
    assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> applicationService.getByClientId("123456"));
  }

  @Test
  public void testUpdate() {
    val application = applicationService.create(entityGenerator.createOneApplication("123456"));
    application.setName("New Name");
    val updated = applicationService.update(application);
    assertThat(updated.getName()).isEqualTo("New Name");
  }

  @Test
  public void testNonexistentEntityUpdate() {
    applicationService.create(entityGenerator.createOneApplication("123456"));
    val nonExistentEntity = entityGenerator.createOneApplication("654321");
    assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> applicationService.update(nonExistentEntity));
  }

  @Test
  public void testUpdateIdNotAllowed() {
    val application = applicationService.create(entityGenerator.createOneApplication("123456"));
    application.setId(777);
    // New id means new non-existent entity or one that exists and is being overwritten
    assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> applicationService.update(application));
  }

  @Test
  public void testUpdateClientIdNotAllowed() {
    entityGenerator.setupSimpleApplications();
    val application = applicationService.getByClientId("111111");
    application.setClientId("222222");
    val newApplication = new Application();
    newApplication.setStatus(application.getStatus());
    newApplication.setClientId(application.getClientId());
    val updated = applicationService.update(application);
    assertThat(updated.getClientId()).isEqualTo("222222");
    // TODO Check for uniqueness in application, currently only SQL
  }

  @Test
  public void testUpdateStatusNotInEnumNotAllowed() {
    entityGenerator.setupSimpleApplications();
    val application = applicationService.getByClientId("111111");
    application.setStatus("Junk");
    val updated = applicationService.update(application);
    assertThat(updated.getName()).isEqualTo("Application 111111");
    // TODO Check for uniqueness in application, currently only SQL
  }

  @Test
  public void testListAppsNoFilters() {
    entityGenerator.setupSimpleApplications();
    val applications = applicationService.listApps(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(applications.getTotalElements()).isEqualTo(5L);
  }

  @Test
  public void testListAppsNoFiltersEmptyResult() {
    val applications = applicationService.listApps(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testListAppsFiltered() {
    entityGenerator.setupSimpleApplications();
    val clientIdFilter = new SearchFilter("clientId", "333333");
    val applications = applicationService.listApps(Arrays.asList(clientIdFilter), new PageableResolver().getPageable());
    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("333333");
  }

  @Test
  public void testListAppsFilteredEmptyResult() {
    entityGenerator.setupSimpleApplications();
    val clientIdFilter = new SearchFilter("clientId", "666666");
    val applications = applicationService.listApps(Arrays.asList(clientIdFilter), new PageableResolver().getPageable());
    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindAppsNoFilters() {
    entityGenerator.setupSimpleApplications();
    val applications = applicationService.findApps("222222", Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("222222");
  }

  @Test
  public void testFindAppsFiltered() {
    entityGenerator.setupSimpleApplications();
    val clientIdFilter = new SearchFilter("clientId", "333333");
    val applications = applicationService.findApps("222222", Arrays.asList(clientIdFilter), new PageableResolver().getPageable());
    // Expect empty list
    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindUsersAppsNoQueryNoFilters() {
    entityGenerator.setupSimpleApplications();
    entityGenerator.setupSimpleUsers();

    val user = userService.getByName("FirstUser@domain.com");
    val userTwo = userService.getByName("SecondUser@domain.com");

    val application = applicationService.getByClientId("444444");

    user.addNewApplication(application);
    userTwo.addNewApplication(application);

    val applications = applicationService.findUsersApps(Integer.toString(user.getId()), Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("444444");
  }

  @Test
  public void testFindUsersAppsNoQueryNoFiltersNoUser() {
    entityGenerator.setupSimpleApplications();
    entityGenerator.setupSimpleUsers();

    val user = userService.getByName("FirstUser@domain.com");
    val applications = applicationService.findUsersApps(Integer.toString(user.getId()), Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindUsersAppsNoQueryNoFiltersEmptyUserString() {
    entityGenerator.setupSimpleApplications();
    entityGenerator.setupSimpleUsers();
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> applicationService.findUsersApps("", Collections.emptyList(), new PageableResolver().getPageable()));
  }

  @Test
  public void testFindUsersAppsNoQueryFilters() {
    entityGenerator.setupSimpleApplications();
    entityGenerator.setupSimpleUsers();

    val user = userService.getByName("FirstUser@domain.com");
    val applicationOne = applicationService.getByClientId("111111");
    val applicationTwo = applicationService.getByClientId("555555");

    user.addNewApplication(applicationOne);
    user.addNewApplication(applicationTwo);

    val clientIdFilter = new SearchFilter("clientId", "111111");

    val applications = applicationService.findUsersApps(Integer.toString(user.getId()), Arrays.asList(clientIdFilter), new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("111111");
  }

  @Test
  public void testFindUsersAppsQueryAndFilters() {
    entityGenerator.setupSimpleApplications();
    entityGenerator.setupSimpleUsers();

    val user = userService.getByName("FirstUser@domain.com");
    val applicationOne = applicationService.getByClientId("333333");
    val applicationTwo = applicationService.getByClientId("444444");

    user.addNewApplication(applicationOne);
    user.addNewApplication(applicationTwo);

    val clientIdFilter = new SearchFilter("clientId", "333333");

    val applications = applicationService.findUsersApps(Integer.toString(user.getId()), "444444", Arrays.asList(clientIdFilter), new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindUsersAppsQueryNoFilters() {
    entityGenerator.setupSimpleApplications();
    entityGenerator.setupSimpleUsers();

    val user = userService.getByName("FirstUser@domain.com");
    val applicationOne = applicationService.getByClientId("222222");
    val applicationTwo = applicationService.getByClientId("444444");

    user.addNewApplication(applicationOne);
    user.addNewApplication(applicationTwo);

    val applications = applicationService.findUsersApps(Integer.toString(user.getId()), "222222", Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("222222");
  }

  @Test
  public void testFindGroupsAppsNoQueryNoFilters() {
    entityGenerator.setupSimpleApplications();
    entityGenerator.setupSimpleGroups();

    val group = groupService.getByName("Group One");
    val groupTwo = groupService.getByName("Group Two");

    val application = applicationService.getByClientId("111111");

    group.addApplication(application);
    groupTwo.addApplication(application);

    val applications = applicationService.findGroupsApplications(Integer.toString(group.getId()), Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("111111");
  }

  @Test
  public void testFindGroupsAppsNoQueryNoFiltersNoGroup() {
    entityGenerator.setupSimpleApplications();
    entityGenerator.setupSimpleGroups();

    val group = groupService.getByName("Group One");
    val applications = applicationService.findGroupsApplications(Integer.toString(group.getId()), Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindGroupsAppsNoQueryNoFiltersEmptyGroupString() {
    entityGenerator.setupSimpleApplications();
    entityGenerator.setupSimpleGroups();
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> applicationService.findGroupsApplications("", Collections.emptyList(), new PageableResolver().getPageable()));
  }

  @Test
  public void testFindGroupsAppsNoQueryFilters() {
    entityGenerator.setupSimpleApplications();
    entityGenerator.setupSimpleGroups();

    val group = groupService.getByName("Group One");
    val applicationOne = applicationService.getByClientId("222222");
    val applicationTwo = applicationService.getByClientId("333333");

    group.addApplication(applicationOne);
    group.addApplication(applicationTwo);

    val clientIdFilter = new SearchFilter("clientId", "333333");

    val applications = applicationService.findGroupsApplications(Integer.toString(group.getId()), Arrays.asList(clientIdFilter), new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("333333");
  }

  @Test
  public void testFindGroupsAppsQueryAndFilters() {
    entityGenerator.setupSimpleApplications();
    entityGenerator.setupSimpleGroups();

    val group = groupService.getByName("Group Three");
    val applicationOne = applicationService.getByClientId("333333");
    val applicationTwo = applicationService.getByClientId("444444");

    group.addApplication(applicationOne);
    group.addApplication(applicationTwo);

    val clientIdFilter = new SearchFilter("clientId", "333333");

    val applications = applicationService.findGroupsApplications(Integer.toString(group.getId()), "444444", Arrays.asList(clientIdFilter), new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testFindGroupsAppsQueryNoFilters() {
    entityGenerator.setupSimpleApplications();
    entityGenerator.setupSimpleGroups();

    val group = groupService.getByName("Group One");
    val applicationOne = applicationService.getByClientId("444444");
    val applicationTwo = applicationService.getByClientId("555555");

    group.addApplication(applicationOne);
    group.addApplication(applicationTwo);

    val applications = applicationService.findGroupsApplications(Integer.toString(group.getId()), "555555", Collections.emptyList(), new PageableResolver().getPageable());

    assertThat(applications.getTotalElements()).isEqualTo(1L);
    assertThat(applications.getContent().get(0).getClientId()).isEqualTo("555555");
  }

  @Test
  public void testDelete() {
    entityGenerator.setupSimpleApplications();

    val application = applicationService.getByClientId("222222");
    applicationService.delete(Integer.toString(application.getId()));

    val applications = applicationService.listApps(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(applications.getTotalElements()).isEqualTo(4L);
    assertThat(applications.getContent()).doesNotContain(application);
  }

  @Test
  public void testDeleteNonExisting() {
    entityGenerator.setupSimpleApplications();
    assertThatExceptionOfType(EmptyResultDataAccessException.class).isThrownBy(() -> applicationService.delete("666666"));
  }

  @Test
  public void testDeleteEmptyIdString() {
    entityGenerator.setupSimpleApplications();
    assertThatExceptionOfType(NumberFormatException.class).isThrownBy(() -> applicationService.delete(""));
  }

  @Test
  public void testLoadClientByClientId() {
    val application = applicationService.create(entityGenerator.createOneApplication("123456"));
    application.setStatus("Approved");
    applicationService.update(application);

    val client = applicationService.loadClientByClientId("123456");

    assertThat(client.getClientId()).isEqualToIgnoringCase("123456");
    assertThat(client.getAuthorizedGrantTypes().containsAll(Arrays.asList(AppTokenClaims.AUTHORIZED_GRANTS)));
    assertThat(client.getScope().containsAll(Arrays.asList(AppTokenClaims.SCOPES)));
    assertThat(client.getRegisteredRedirectUri()).isEqualTo(application.getURISet());
    assertThat(client.getAuthorities()).containsExactly(new SimpleGrantedAuthority(AppTokenClaims.ROLE));
  }

  @Test
  public void testLoadClientByClientIdNotFound() {
    assertThatExceptionOfType(ClientRegistrationException.class).isThrownBy(
        () -> applicationService.loadClientByClientId("123456")).withMessage("Client ID not found.");
  }

  @Test
  public void testLoadClientByClientIdEmptyString() {
    assertThatExceptionOfType(ClientRegistrationException.class).isThrownBy(
        () -> applicationService.loadClientByClientId("")).withMessage("Client ID not found.");
  }

  @Test
  public void testLoadClientByClientIdNotApproved() {
    val application = applicationService.create(entityGenerator.createOneApplication("123456"));
    application.setStatus("Pending");
    applicationService.update(application);
    assertThatExceptionOfType(ClientRegistrationException.class).isThrownBy(() -> applicationService.loadClientByClientId("123456")).withMessage("Client Access is not approved.");
    application.setStatus("Rejected");
    applicationService.update(application);
    assertThatExceptionOfType(ClientRegistrationException.class).isThrownBy(() -> applicationService.loadClientByClientId("123456")).withMessage("Client Access is not approved.");
    application.setStatus("Disabled");
    applicationService.update(application);
    assertThatExceptionOfType(ClientRegistrationException.class).isThrownBy(() -> applicationService.loadClientByClientId("123456")).withMessage("Client Access is not approved.");
  }

}
