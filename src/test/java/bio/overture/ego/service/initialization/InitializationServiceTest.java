package bio.overture.ego.service.initialization;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.config.InitializationConfig;
import bio.overture.ego.config.InitializationConfig.InitialApplication;
import bio.overture.ego.model.enums.ApplicationType;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.model.exceptions.UniqueViolationException;
import bio.overture.ego.repository.ApplicationRepository;
import bio.overture.ego.repository.InitTripWireRepository;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.InitializationService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;

import static java.util.Arrays.stream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Slf4j
@Transactional
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "initialization.enabled=false")
public class InitializationServiceTest {

  /**
   * Dependencies
   */
  @Autowired private InitTripWireRepository initTripWireRepository;
  @Autowired private InitializationService initializationService;
  @Autowired private ApplicationRepository applicationRepository;
  @Autowired private ApplicationService applicationService;

  @Rule public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void initialize_emptyConfig_trippedWithNoApplications(){
    // Create an empty config, meaning there are no applications to intialize
    val config = createConfig();

    // Assert the trip wire was not already set, and that there are no applications yet
    assertEquals(0,initTripWireRepository.count());
    assertFalse(initializationService.isInitialized());
    assertEquals(0,applicationRepository.count());

    // Run the initialization using the empty config
    val service = new InitializationService(initTripWireRepository, applicationService, config);
    service.initialize();

    // Assert init trip wire was set
    assertEquals(1,initTripWireRepository.count());
    assertEquals(1, initTripWireRepository.findAll().stream().findFirst().get().getInitialized());
    assertTrue(initializationService.isInitialized());
    // Assert no applications were created
    assertEquals(0,applicationRepository.count());
  }

  @Test
  public void initialize_multipleApps_success(){
    // Create an empty config, meaning there are no applications to intialize
    val config = createConfig(
        InitialApplication.builder()
            .name("app1")
            .clientId("clientId1")
            .clientSecret("clientSecret")
            .type(ApplicationType.ADMIN)
            .build(),
        InitialApplication.builder()
            .name("app2")
            .clientId("clientId2")
            .clientSecret("clientSecret")
            .type(ApplicationType.ADMIN)
            .build()
    );

    // Assert the trip wire was not already set, and that there are no applications yet
    assertEquals(0,initTripWireRepository.count());
    assertFalse(initializationService.isInitialized());
    assertEquals(0,applicationRepository.count());

    // Run the initialization using the empty config
    val service = new InitializationService(initTripWireRepository, applicationService, config);
    service.initialize();

    // Assert init trip wire was set
    assertEquals(1,initTripWireRepository.count());
    assertEquals(1, initTripWireRepository.findAll().stream().findFirst().get().getInitialized());
    assertTrue(initializationService.isInitialized());

    // Assert 2 applications exist
    assertEquals(2,applicationRepository.count());
    applicationService.getByClientId("clientId1");
    applicationService.getByClientId("clientId2");
  }

  @Test
  public void initialize_conflictingApplication_conflictAndRollback(){
    // Create an empty config, meaning there are no applications to intialize
    val config = createConfig(
        InitialApplication.builder()
            .name("app1")
            .clientId("clientId1")
            .clientSecret("clientSecret")
            .type(ApplicationType.ADMIN)
            .build(),
        InitialApplication.builder()
            .name("app2")
            .clientId("clientId1")
            .clientSecret("clientSecret")
            .type(ApplicationType.ADMIN)
            .build()
    );

    // Assert the trip wire was not already set, and that there are no applications yet
    assertEquals(0,initTripWireRepository.count());
    assertFalse(initializationService.isInitialized());
    assertEquals(0,applicationRepository.count());

    // Run the initialization using the empty config
    val service = new InitializationService(initTripWireRepository, applicationService, config);
    exceptionRule.expect(UniqueViolationException.class);
    service.initialize();

    // Assert init trip wire was NOT set
    assertEquals(0,initTripWireRepository.count());
    assertEquals(0, initTripWireRepository.findAll().stream().findFirst().get().getInitialized());
    assertFalse(initializationService.isInitialized());

    // Assert 0 applications exist
    assertEquals(0,applicationRepository.count());
  }

  private static InitializationConfig createConfig(InitialApplication ... applications){
    val config = new InitializationConfig();
    stream(applications)
        .forEach(x -> config.getApplications().add(x));
    return config;
  }

}
