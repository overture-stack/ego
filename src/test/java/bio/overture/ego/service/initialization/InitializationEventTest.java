package bio.overture.ego.service.initialization;

import static org.junit.Assert.assertTrue;

import bio.overture.ego.repository.InitTripWireRepository;
import bio.overture.ego.service.InitializationService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "initialization.enabled=true")
public class InitializationEventTest {

  @Autowired private InitializationService service;
  @Autowired private InitTripWireRepository repository;

  @Test
  public void testInitializationUsingSpringEvents() {
    assertTrue(service.isInitialized());
    // Note: this is necessary since this will persist the initialization flag for other tests.
    // This is out of context of a test transaction because the initialization happens at when
    // spring boots,
    // which is before the execution of this test.
    // Because of this, we need to delete the initialization value from the database for other
    // tests.
    repository.deleteAll();
  }
}
