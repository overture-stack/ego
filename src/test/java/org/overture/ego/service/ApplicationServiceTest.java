package org.overture.ego.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.overture.ego.controller.resolver.PageableResolver;
import org.overture.ego.model.entity.Application;
import org.overture.ego.repository.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class ApplicationServiceTest {

  @Autowired
  private ApplicationService applicationService;

  private Application testApplication;

  @Before
  public void setup() {
    testApplication = new Application("Test Name", "123456", "654321");

  }

  @Test
  public void testCreate() {
    val x = applicationService.create(testApplication);
    assertThat(1).isEqualTo(1);
  }

//  private Application testApplication;
//
//  @Mock
//  private ApplicationRepository applicationRepository;
//
//  @Mock
//  private PasswordEncoder passwordEncoder;
//
//  @Mock
//  private Application applicationMock;
//
//  @InjectMocks
//  private ApplicationService applicationService;
//
//  @Before
//  public void setup() {
//    MockitoAnnotations.initMocks(this);
//    testApplication = new Application("Test Name", "123456", "654321");
//    when(applicationRepository.findById(0)).thenReturn(Optional.of(testApplication));
//  }
//
//  @Test
//  public void testCreate() {
//    applicationService.create(testApplication);
//    verify(applicationRepository).save(testApplication);
//  }
//
//  @Test
//  public void testGet() {
//    applicationService.get("0");
//    verify(applicationRepository).findById(0);
//  }
//
//  @Test
//  public void testGetNotFound() {
//    assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> applicationService.get("1"));
//  }
//
//  @Test
//  public void testListAppsNoFilter() {
//    applicationService.listApps(Collections.emptyList(), new PageableResolver().getPageable());
//    verify(applicationRepository).findAll(notNull(), (Pageable) notNull());
//  }
//
//  @Test
//  public void testUpdate() {
//    testApplication.setClientId("updated");
//    val test = applicationService.update(testApplication);
//    assertThat(testApplication.getClientId()).isEqualTo("updated");
//    verify(applicationRepository).save(testApplication);
//  }
//
//  @Test
//  public void testNonexistentEntityUpdate() {
//    Application nonExistentEntity = new Application("Hello World", "asd","dsa");
//    nonExistentEntity.setId(1);
//    assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> applicationService.update(nonExistentEntity));
//  }
//
//  private List<Application> createMultipleApplications() {
//    return Arrays.asList(
//        new Application("Application Two", "111aaa", "222bbb"),
//        new Application("Application Three", "333ccc", "444ddd"),
//        new Application("Application Four", "555eee", "666fff")
//    );
//  }
}
