package bio.overture.ego.controller;

import static bio.overture.ego.model.enums.AccessLevel.DENY;
import static bio.overture.ego.model.enums.AccessLevel.WRITE;
import static bio.overture.ego.utils.Joiners.COMMA;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.OK;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.service.AbstractPermissionService;
import bio.overture.ego.service.NamedService;
import bio.overture.ego.service.PolicyService;
import bio.overture.ego.service.UserPermissionService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.Streams;
import java.util.Collection;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
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
public class UserPermissionControllerTest
    extends AbstractPermissionControllerTest<User, UserPermission> {

  /** Dependencies */
  @Autowired private EntityGenerator entityGenerator;

  @Autowired private PolicyService policyService;
  @Autowired private UserService userService;
  @Autowired private UserPermissionService userPermissionService;

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
  protected Class<User> getOwnerType() {
    return User.class;
  }

  @Override
  protected Class<UserPermission> getPermissionType() {
    return UserPermission.class;
  }

  @Override
  protected User generateOwner(String name) {
    return entityGenerator.setupUser(name);
  }

  @Override
  protected String generateNonExistentOwnerName() {
    return entityGenerator.generateNonExistentUserName();
  }

  @Override
  protected NamedService<User, UUID> getOwnerService() {
    return userService;
  }

  @Override
  protected AbstractPermissionService<User, UserPermission> getPermissionService() {
    return userPermissionService;
  }

  @Override
  protected String getAddPermissionsEndpoint(String ownerId) {
    return format("users/%s/permissions", ownerId);
  }

  @Override
  protected String getAddPermissionEndpoint(String policyId, String ownerId) {
    return format("policies/%s/permission/user/%s", policyId, ownerId);
  }

  @Override
  protected String getReadPermissionsEndpoint(String ownerId) {
    return format("users/%s/permissions", ownerId);
  }

  @Override
  protected String getDeleteOwnerEndpoint(String ownerId) {
    return format("users/%s", ownerId);
  }

  @Override
  protected String getDeletePermissionsEndpoint(String ownerId, Collection<String> permissionIds) {
    return format("users/%s/permissions/%s", ownerId, COMMA.join(permissionIds));
  }

  @Override
  protected String getDeletePermissionEndpoint(String policyId, String ownerId) {
    return format("policies/%s/permission/user/%s", policyId, ownerId);
  }

  @Override
  protected String getReadOwnersForPolicyEndpoint(String policyId) {
    return format("policies/%s/users", policyId);
  }

  //TODO: delete this test
  @Test
  @SneakyThrows
  public void addPermissionsToOwner_Unique_Success__ROB() {
    // Add Permissions to owner
    val r1 =
        initStringRequest()
            .endpoint(getAddPermissionsEndpoint(owner1.getId().toString()))
            .body(permissionRequests)
            .post();
    assertEquals(r1.getStatusCode(), OK);

    // Get the policies for this owner
    val r3 =
        initStringRequest().endpoint(getReadPermissionsEndpoint(owner1.getId().toString())).get();
    assertEquals(r3.getStatusCode(), OK);

    // Analyze results
    val page = MAPPER.readTree(r3.getBody());
    assertNotNull(page);
    assertEquals(page.get("count").asInt(), 2);
    val outputMap =
        Streams.stream(page.path("resultSet").iterator())
            .collect(
                toMap(
                    x -> x.path("policy").path("id").asText(),
                    x -> x.path("accessLevel").asText()));
    assertTrue(outputMap.containsKey(policies.get(0).getId().toString()));
    assertTrue(outputMap.containsKey(policies.get(1).getId().toString()));
    assertEquals(outputMap.get(policies.get(0).getId().toString()), WRITE.toString());
    assertEquals(outputMap.get(policies.get(1).getId().toString()), DENY.toString());

    ////////////////////
    val p = this.policies.get(0);
    val resp =
        initStringRequest()
            .endpoint("policies/%s/users?query=%s", p.getId(), owner1.getName())
            .getAnd()
            .getResponse();
    log.info("response: {}", resp);
  }
}
