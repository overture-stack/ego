package bio.overture.ego.controller;

import static java.lang.String.format;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.ApplicationPermission;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.utils.EntityGenerator;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@TestExecutionListeners(listeners = DependencyInjectionTestExecutionListener.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ListApplicationPermissionsForPolicyControllerTest
    extends AbstractListOwnerPermissionsForPolicyControllerTest<
        Application, ApplicationPermission> {
  @Autowired private EntityGenerator entityGenerator;

  @Value("${logging.test.controller.enable}")
  private boolean enableLogging;

  @Override
  protected List<Application> setupOwners(String... names) {
    return entityGenerator.setupApplications(names);
  }

  @Override
  protected String getAddPermissionsEndpoint(UUID ownerId) {
    return format("applications/%s/permissions", ownerId);
  }

  @Override
  protected String getOwnersForPolicyEndpoint(UUID policyId) {
    return format("policies/%s/applications", policyId);
  }

  @Override
  protected boolean enableLogging() {
    return enableLogging;
  }

  @Override
  protected Policy createPolicy(String name) {
    return entityGenerator.setupSinglePolicy(name);
  }
}
