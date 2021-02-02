package bio.overture.ego.controller;

import static bio.overture.ego.utils.EntityGenerator.generateNonExistentName;
import static bio.overture.ego.utils.Joiners.COMMA;
import static java.lang.String.format;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.GroupPermission;
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
public class GroupPermissionControllerTest
    extends AbstractNameablePermissionControllerTest<Group, GroupPermission> {

  /** Dependencies */
  @Autowired private EntityGenerator entityGenerator;

  @Autowired private PolicyService policyService;
  @Autowired private GroupService groupService;
  @Autowired private GroupPermissionService groupPermissionService;

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
  protected Class<Group> getOwnerType() {
    return Group.class;
  }

  @Override
  protected Class<GroupPermission> getPermissionType() {
    return GroupPermission.class;
  }

  @Override
  protected Group generateOwner(String name) {
    return entityGenerator.setupGroup(name);
  }

  @Override
  protected String generateNonExistentOwnerName() {
    return generateNonExistentName(groupService);
  }

  @Override
  protected NamedService<Group, UUID> getOwnerService() {
    return groupService;
  }

  @Override
  protected AbstractNameablePermissionService<Group, GroupPermission> getPermissionService() {
    return groupPermissionService;
  }

  @Override
  protected String getAddPermissionsEndpoint(String ownerId) {
    return format("groups/%s/permissions", ownerId);
  }

  @Override
  protected String getAddPermissionEndpoint(String policyId, String ownerId) {
    return format("policies/%s/permission/group/%s", policyId, ownerId);
  }

  @Override
  protected String getReadPermissionsEndpoint(String ownerId) {
    return format("groups/%s/permissions", ownerId);
  }

  @Override
  protected String getDeleteOwnerEndpoint(String ownerId) {
    return format("groups/%s", ownerId);
  }

  @Override
  protected String getDeletePermissionsEndpoint(String ownerId, Collection<String> permissionIds) {
    return format("groups/%s/permissions/%s", ownerId, COMMA.join(permissionIds));
  }

  @Override
  protected String getDeletePermissionEndpoint(String policyId, String ownerId) {
    return format("policies/%s/permission/group/%s", policyId, ownerId);
  }

  @Override
  protected String getReadOwnersForPolicyEndpoint(String policyId) {
    return format("policies/%s/groups", policyId);
  }

  @Override
  protected String getOwnerAndGroupPermissionsForOwnerEndpoint(String ownerId) {
    return format("groups/%s/permissions", ownerId);
  }

  @Override
  protected String getAddOwnerToGroupEndpoint(String groupId) {
    return "groups";
  }
}
