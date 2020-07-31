package bio.overture.ego.controller;

import static bio.overture.ego.utils.EntityGenerator.generateNonExistentName;
import static bio.overture.ego.utils.Joiners.COMMA;
import static java.lang.String.format;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.ApplicationPermission;
import bio.overture.ego.service.*;
import bio.overture.ego.utils.EntityGenerator;
import java.util.Collection;
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
public class ApplicationPermissionControllerTest
    extends AbstractPermissionControllerTest<Application, ApplicationPermission> {

  /** Dependencies */
  @Autowired private EntityGenerator entityGenerator;

  @Autowired private PolicyService policyService;
  @Autowired private ApplicationService applicationService;
  @Autowired private ApplicationPermissionService applicationPermissionService;

  @Value("${logging.test.controller.enable}")
  private boolean enableLogging;

  @Override
  protected boolean enableLogging() {
    return enableLogging;
  }

  @Override
  protected EntityGenerator getEntityGenerator() {
    return entityGenerator;
  }

  @Override
  protected PolicyService getPolicyService() {
    return policyService;
  }

  @Override
  protected Class<Application> getOwnerType() {
    return Application.class;
  }

  @Override
  protected Class<ApplicationPermission> getPermissionType() {
    return ApplicationPermission.class;
  }

  @Override
  protected Application generateOwner(String name) {
    return entityGenerator.setupApplication(name);
  }

  @Override
  protected String generateNonExistentOwnerName() {
    return generateNonExistentName(applicationService);
  }

  @Override
  protected NamedService<Application, UUID> getOwnerService() {
    return applicationService;
  }

  @Override
  protected AbstractPermissionService<Application, ApplicationPermission> getPermissionService() {
    return applicationPermissionService;
  }

  @Override
  protected String getAddPermissionsEndpoint(String ownerId) {
    return format("applications/%s/permissions", ownerId);
  }

  @Override
  protected String getAddPermissionEndpoint(String policyId, String ownerId) {
    return format("policies/%s/permission/application/%s", policyId, ownerId);
  }

  @Override
  protected String getReadPermissionsEndpoint(String ownerId) {
    return format("applications/%s/permissions", ownerId);
  }

  @Override
  protected String getDeleteOwnerEndpoint(String ownerId) {
    return format("applications/%s", ownerId);
  }

  @Override
  protected String getDeletePermissionsEndpoint(String ownerId, Collection<String> permissionIds) {
    return format("applications/%s/permissions/%s", ownerId, COMMA.join(permissionIds));
  }

  @Override
  protected String getDeletePermissionEndpoint(String policyId, String ownerId) {
    return format("policies/%s/permission/application/%s", policyId, ownerId);
  }

  @Override
  protected String getReadOwnersForPolicyEndpoint(String policyId) {
    return format("policies/%s/applications", policyId);
  }

  @Override
  protected String getOwnerAndGroupPermissionsForOwnerEndpoint(String ownerId) {
    return format("applications/%s/groups/permissions", ownerId);
  }

  @Override
  protected String getAddOwnerToGroupEndpoint(String groupId) {
    return format("groups/%s/applications", groupId);
  }
}
