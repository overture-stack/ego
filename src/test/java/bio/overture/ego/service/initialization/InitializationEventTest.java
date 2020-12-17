package bio.overture.ego.service.initialization;

import bio.overture.ego.service.InitializationService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertTrue;

@Slf4j
@Transactional
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "initialization.enabled=true")
public class InitializationEventTest {

  @Autowired private InitializationService service;

  @Test
  public void testInitializationUsingSpringEvents(){
    assertTrue(service.isInitialized());
  }

}
