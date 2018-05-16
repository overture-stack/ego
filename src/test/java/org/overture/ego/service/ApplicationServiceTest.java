package org.overture.ego.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.overture.ego.controller.resolver.PageableResolver;
import org.overture.ego.model.entity.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

  @Test
  public void testCreate() {
    val application = applicationService.create(createOneApplication("123456"));
    assertThat(application.getClientId()).isEqualTo("123456");
  }

  @Test
  public void testCreateUniqueClientId() {
    applicationService.create(createOneApplication("111111"));
    applicationService.create(createOneApplication("222222"));
    assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() -> applicationService.create(createOneApplication("111111")));
  }

  @Test
  public void testGet() {
    val application = applicationService.create(createOneApplication("123456"));
    val savedApplication = applicationService.get(Integer.toString(application.getId()));
    assertThat(savedApplication.getClientId()).isEqualTo("123456");
  }

  @Test
  public void testGetEntityNotFoundException() {
    assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> applicationService.get("1"));
  }

  @Test
  public void testUpdate() {
    val application = applicationService.create(createOneApplication("123456"));
    application.setName("New Name");
    val updated = applicationService.update(application);
    assertThat(updated.getName()).isEqualTo("New Name");
  }

  @Test
  public void testNonexistentEntityUpdate() {
    applicationService.create(createOneApplication("123456"));
    val nonExistentEntity = createOneApplication("654321");
    assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> applicationService.update(nonExistentEntity));
  }

  @Test
  public void testListAppsNoFilter() {
    for (Application application : createApplicationsFromList(Arrays.asList("111111", "222222", "333333", "444444", "555555"))) {
      applicationService.create(application);
    }
    val applications = applicationService.listApps(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(applications.getTotalElements()).isEqualTo(5L);
  }


  private Application createOneApplication(String clientId) {
    return new Application(String.format("Application %s", clientId), clientId, new StringBuilder(clientId).reverse().toString());
  }

  private List<Application> createApplicationsFromList(List<String> clientIds) {
    return clientIds.stream().map(this::createOneApplication).collect(Collectors.toList());
  }
}
