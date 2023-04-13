package bio.overture.ego.service.initialization;

import static bio.overture.ego.model.enums.ApplicationType.ADMIN;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentClientId;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentName;
import static java.util.Arrays.stream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import bio.overture.ego.config.InitializationConfig;
import bio.overture.ego.config.InitializationConfig.InitialApplication;
import bio.overture.ego.model.exceptions.UniqueViolationException;
import bio.overture.ego.repository.ApplicationRepository;
import bio.overture.ego.repository.InitTripWireRepository;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.InitializationService;
import bio.overture.ego.utils.EntityGenerator;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@Transactional
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "initialization.enabled=false")
public class InitializationServiceTest {

  /** Dependencies */
  @Autowired private InitTripWireRepository initTripWireRepository;

  @Autowired private InitializationService initializationService;
  @Autowired private ApplicationRepository applicationRepository;
  @Autowired private ApplicationService applicationService;
  @Autowired private EntityGenerator entityGenerator;

  @Rule public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void initialize_emptyConfig_trippedWithNoApplications() {
    // Create an empty config, meaning there are no applications to initialize
    val config = createConfig();

    // Assert the trip wire was not already set
    assertEquals(0, initTripWireRepository.count());
    assertFalse(initializationService.isInitialized());

    val referenceAppCount = applicationRepository.count();

    // Run the initialization using the empty config
    val service = new InitializationService(initTripWireRepository, applicationService, config);
    service.initialize();

    // Assert init trip wire was set
    assertEquals(1, initTripWireRepository.count());
    assertEquals(1, initTripWireRepository.findAll().stream().findFirst().get().getInitialized());
    assertTrue(initializationService.isInitialized());

    // Assert no applications were created
    assertEquals(referenceAppCount, applicationRepository.count());
  }

  @Test
  public void initialize_multipleApps_success() {
    // Create an empty config, meaning there are no applications to intialize
    val appName1 = generateNonExistentName(applicationService);
    val appClientId1 = generateNonExistentClientId(applicationService);
    val appName2 = generateNonExistentName(applicationService);
    val appClientId2 = generateNonExistentClientId(applicationService);

    val config =
        createConfig(
            InitialApplication.builder()
                .name(appName1)
                .clientId(appClientId1)
                .clientSecret("clientSecret")
                .type(ADMIN)
                .build(),
            InitialApplication.builder()
                .name(appName2)
                .clientId(appClientId2)
                .clientSecret("clientSecret")
                .type(ADMIN)
                .build());

    // Assert the trip wire was not already set, and that there are no applications yet
    assertEquals(0, initTripWireRepository.count());
    assertFalse(initializationService.isInitialized());

    // Assert the applications do not exist
    assertFalse(applicationService.getClientApplication(appClientId1).isPresent());
    assertFalse(applicationService.getClientApplication(appClientId2).isPresent());

    // Run the initialization using the empty config
    val service = new InitializationService(initTripWireRepository, applicationService, config);
    service.initialize();

    // Assert init trip wire was set
    assertEquals(1, initTripWireRepository.count());
    assertEquals(1, initTripWireRepository.findAll().stream().findFirst().get().getInitialized());
    assertTrue(initializationService.isInitialized());

    // Assert the applications exist
    assertTrue(applicationService.getClientApplication(appClientId1).isPresent());
    assertTrue(applicationService.getClientApplication(appClientId2).isPresent());
  }

  @Test
  public void initialize_conflictingApplication_conflictAndRollback() {
    val appName1 = generateNonExistentName(applicationService);
    val appClientId1 = generateNonExistentClientId(applicationService);
    val appName2 = generateNonExistentName(applicationService);

    // Create an empty config, meaning there are no applications to initialize
    val config =
        createConfig(
            InitialApplication.builder()
                .name(appName1)
                .clientId(appClientId1)
                .clientSecret("clientSecret")
                .type(ADMIN)
                .build(),
            InitialApplication.builder()
                .name(appName2)
                .clientId(appClientId1) // same as previous clientId
                .clientSecret("clientSecret")
                .type(ADMIN)
                .build());

    // Assert the trip wire was not already set, and that there are no applications yet
    assertEquals(0, initTripWireRepository.count());
    assertFalse(initializationService.isInitialized());

    // Assert the applications do not exist
    assertFalse(applicationService.getClientApplication(appClientId1).isPresent());
    assertFalse(applicationService.findByName(appName1).isPresent());
    assertFalse(applicationService.findByName(appName2).isPresent());

    // Run the initialization using the empty config
    val service = new InitializationService(initTripWireRepository, applicationService, config);
    exceptionRule.expect(UniqueViolationException.class);
    service.initialize();

    // Assert init trip wire was NOT set
    assertEquals(0, initTripWireRepository.count());
    assertEquals(0, initTripWireRepository.findAll().stream().findFirst().get().getInitialized());
    assertFalse(initializationService.isInitialized());

    // Assert the applications were not created
    assertFalse(applicationService.getClientApplication(appClientId1).isPresent());
    assertFalse(applicationService.findByName(appName1).isPresent());
    assertFalse(applicationService.findByName(appName2).isPresent());
  }

  private static InitializationConfig createConfig(InitialApplication... applications) {
    val config = new InitializationConfig();
    stream(applications).forEach(x -> config.getApplications().add(x));
    return config;
  }
}
