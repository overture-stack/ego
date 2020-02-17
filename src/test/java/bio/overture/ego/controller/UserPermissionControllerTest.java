package bio.overture.ego.controller;

import static bio.overture.ego.model.enums.AccessLevel.DENY;
import static bio.overture.ego.model.enums.AccessLevel.READ;
import static bio.overture.ego.model.enums.AccessLevel.WRITE;
import static bio.overture.ego.utils.Joiners.COMMA;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.OK;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.service.AbstractPermissionService;
import bio.overture.ego.service.NamedService;
import bio.overture.ego.service.PolicyService;
import bio.overture.ego.service.UserPermissionService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import bio.overture.ego.utils.Streams;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hamcrest.Matchers;
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

  private List<User> usersWithWrite;
  private List<User> usersWithRead;
  private List<User> usersWithDeny;
  private Policy policyUT;

  private void createWritePermissionsForUsers(Policy p,
      Collection<User> users){
    createPermissionsForUsers(p.getId(), WRITE, users);
  }

  private void createReadPermissionsForUsers(Policy p,
      Collection<User> users){
    createPermissionsForUsers(p.getId(), READ, users);
  }

  private void createDenyPermissionsForUsers(Policy p,
      Collection<User> users){
    createPermissionsForUsers(p.getId(), DENY, users);
  }

  private void createPermissionsForUsers(UUID policyId, AccessLevel mask, Collection<User> users){
    users.forEach(u ->
      initStringRequest()
          .endpoint(getAddPermissionsEndpoint(u.getId().toString()))
          .body(PermissionRequest.builder()
              .mask(mask)
              .policyId(policyId)
              .build() )
          .postAnd()
          .assertOk());
  }

  @Override
  protected void beforeTest() {
    super.beforeTest();
    policyUT = this.policies.get(0);
    usersWithWrite = entityGenerator.setupUsers("AUser Write", "BUser Write", "CUser Write");
    usersWithRead = entityGenerator.setupUsers("AUser Read", "BUser Read", "CUser Read");
    usersWithDeny = entityGenerator.setupUsers("AUser Deny", "BUser Deny", "CUser Deny");
    createDenyPermissionsForUsers(policyUT, usersWithDeny);
    createReadPermissionsForUsers(policyUT, usersWithRead);
    createWritePermissionsForUsers(policyUT, usersWithWrite);
  }

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

    val r2 =
        initStringRequest()
            .endpoint(getAddPermissionsEndpoint(owner2.getId().toString()))
            .body(permissionRequests)
            .post();
    assertEquals(r2.getStatusCode(), OK);

    // Get the policies for this owner
    val r3 =
        initStringRequest().endpoint(getReadPermissionsEndpoint(owner1.getId().toString())).get();
    assertEquals(r3.getStatusCode(), OK);

    val r4 =
        initStringRequest().endpoint(getReadPermissionsEndpoint(owner2.getId().toString())).get();
    assertEquals(r4.getStatusCode(), OK);

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
    // TESTSSSSSS
    ////////////////////
    val p = this.policies.get(0);
    initStringRequest()
        .endpoint("policies/%s/users?limit=1", p.getId())
        .getAnd()
        .assertPageResultsOfType(PolicyResponse.class)
        .hasSize(1);

    initStringRequest()
        .endpoint("policies/%s/users", p.getId())
        .getAnd()
        .assertPageResultsOfType(PolicyResponse.class)
        .hasSize(2);

    initStringRequest()
        .endpoint("policies/%s/users?query=%s", p.getId(), owner1.getName())
        .getAnd()
        .assertPageResultsOfType(PolicyResponse.class)
        .hasSize(1);

    initStringRequest()
        .endpoint("policies/%s/users?query=%s&limit=1", p.getId(), owner1.getName())
        .getAnd()
        .assertPageResultsOfType(PolicyResponse.class)
        .hasSize(1);
    initStringRequest()
        .endpoint("policies/%s/users?name=%s&limit=1", p.getId(), owner1.getName())
        .getAnd()
        .assertPageResultsOfType(PolicyResponse.class)
        .hasSize(1);

    val nameDescResp = initStringRequest()
        .endpoint("policies/%s/users?sort=name&sortOrder=desc", p.getId())
        .getAnd()
        .extractPageResults(PolicyResponse.class);
    assertEquals(2, nameDescResp.size());
    for (int i = 1; i<nameDescResp.size(); i++){
      val above = nameDescResp.get(i-1).getName().toLowerCase();
      val below = nameDescResp.get(i).getName().toLowerCase();
      assertTrue(above.compareToIgnoreCase(below) > 0);
    }

    val nameAscResp = initStringRequest()
        .endpoint("policies/%s/users?sort=name&sortOrder=asc", p.getId())
        .getAnd()
        .extractPageResults(PolicyResponse.class);
    assertEquals(2, nameAscResp.size());
    for (int i = 1; i<nameAscResp.size(); i++){
      val above = nameAscResp.get(i-1).getName().toLowerCase();
      val below = nameAscResp.get(i).getName().toLowerCase();
      assertTrue(above.compareToIgnoreCase(below) < 0);
    }

    val idDescResp = initStringRequest()
        .endpoint("policies/%s/users?sort=id&sortOrder=desc", p.getId())
        .getAnd()
        .extractPageResults(PolicyResponse.class);
    assertEquals(2, idDescResp.size());
    for (int i = 1; i<idDescResp.size(); i++){
      val above = idDescResp.get(i-1).getId().toLowerCase();
      val below = idDescResp.get(i).getId().toLowerCase();
      assertTrue(above.compareToIgnoreCase(below) > 0);
    }

    val idAscResp = initStringRequest()
        .endpoint("policies/%s/users?sort=id&sortOrder=asc", p.getId())
        .getAnd()
        .extractPageResults(PolicyResponse.class);
    assertEquals(2, idAscResp.size());
    for (int i = 1; i<idAscResp.size(); i++){
      val above = idAscResp.get(i-1).getId().toLowerCase();
      val below = idAscResp.get(i).getId().toLowerCase();
      assertTrue(above.compareToIgnoreCase(below) < 0);
    }

    val maskDescResp = initStringRequest()
        .endpoint("policies/%s/users?sort=mask&sortOrder=desc", p.getId())
        .getAnd()
        .extractPageResults(PolicyResponse.class);
    assertEquals(2, maskDescResp.size());
    for (int i = 1; i<maskDescResp.size(); i++){
      val above = maskDescResp.get(i-1).getMask().toString().toLowerCase();
      val below = maskDescResp.get(i).getMask().toString().toLowerCase();
      // Need to improve test setup to have different masks to show sorting
      assertTrue(above.compareToIgnoreCase(below) == 0);
    }

    val maskAscResp = initStringRequest()
        .endpoint("policies/%s/users?sort=mask&sortOrder=asc", p.getId())
        .getAnd()
        .extractPageResults(PolicyResponse.class);
    assertEquals(2, maskAscResp.size());
    for (int i = 1; i<maskAscResp.size(); i++){
      val above = maskDescResp.get(i-1).getMask().toString().toLowerCase();
      val below = maskDescResp.get(i).getMask().toString().toLowerCase();
      // Need to improve test setup to have different masks to show sorting
      assertTrue(above.compareToIgnoreCase(below) == 0);
    }


//    log.info("response: {}", resp);
  }

  public void test

}
