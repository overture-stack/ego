package bio.overture.ego.controller;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.PolicyService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.Streams;
import bio.overture.ego.utils.WebResource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static bio.overture.ego.model.enums.AccessLevel.DENY;
import static bio.overture.ego.utils.Collectors.toImmutableList;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentId;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentName;
import static bio.overture.ego.utils.Joiners.COMMA;
import static bio.overture.ego.utils.WebResource.createWebResource;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@TestExecutionListeners(listeners = DependencyInjectionTestExecutionListener.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GroupPermissionControllerTest {

  /** Constants */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String ACCESS_TOKEN = "TestToken";

  /** State */
  @LocalServerPort private int port;

  private TestRestTemplate restTemplate = new TestRestTemplate();
  private HttpHeaders headers = new HttpHeaders();

  /** Dependencies */
  @Autowired private EntityGenerator entityGenerator;
  @Autowired private GroupService groupService;
  @Autowired private PolicyService policyService;
  @Autowired private UserService userService;

  /** State */
  private User user1;

  private Group group1;
  private Group group2;
  private List<Policy> policies;
  private List<PermissionRequest> permissionRequests;

  @Before
  public void setup() {
    // Initial setup of entities (run once)
    this.group1 = entityGenerator.setupGroup(generateNonExistentName(groupService));
    this.group2 = entityGenerator.setupGroup(generateNonExistentName(groupService));
    this.user1 = entityGenerator.setupUser(entityGenerator.generateNonExistentUserName());
    this.policies =
        IntStream.range(0, 2)
            .boxed()
            .map(x -> generateNonExistentName(policyService))
            .map(entityGenerator::setupSinglePolicy)
            .collect(toImmutableList());

    this.permissionRequests =
        ImmutableList.<PermissionRequest>builder()
            .add(
                PermissionRequest.builder()
                    .policyId(policies.get(0).getId().toString())
                    .mask(AccessLevel.WRITE)
                    .build())
            .add(
                PermissionRequest.builder()
                    .policyId(policies.get(1).getId().toString())
                    .mask(DENY)
                    .build())
            .build();

    // Sanity check
    assertThat(groupService.isExist(group1.getId())).isTrue();
    assertThat(userService.isExist(user1.getId())).isTrue();
    policies.forEach(p -> assertThat(policyService.isExist(p.getId())).isTrue());

    headers.add(AUTHORIZATION, "Bearer " + ACCESS_TOKEN);
    headers.setContentType(APPLICATION_JSON);
  }

  /**
   * Add permissions to a non-existent group
   */
  @Test
  public void addGroupPermissionsToGroup_NonExistentGroup_NotFound() {
    val nonExistentGroupId = generateNonExistentId(groupService);
    val r1 =
        initStringRequest()
            .endpoint("groups/%s/permissions", nonExistentGroupId.toString())
            .body(permissionRequests)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(r1.getBody()).contains(nonExistentGroupId.toString());
  }

  /**
   * Attempt to add an empty list of permission request to a group
   */
  @Test
  @SneakyThrows
  public void addGroupPermissionsToGroup_EmptyPermissionRequests_Conflict() {
    // Add some of the permissions
    val r1 =
        initRequest(Group.class)
            .endpoint("groups/%s/permissions", group1.getId().toString())
            .body(newArrayList())
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  /**
   * Add permissions to a group that has SOME those permissions
   */
  @Test
  @SneakyThrows
  public void addGroupPermissionsToGroup_SomeAlreadyExists_Conflict() {
    val somePermissionRequests = ImmutableList.of(permissionRequests.get(0));

    // Add some of the permissions
    val r1 =
        initRequest(Group.class)
            .endpoint("groups/%s/permissions", group1.getId().toString())
            .body(somePermissionRequests)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);
    assertThat(r1.getBody()).isNotNull();
    val r1body = r1.getBody();
    assertThat(r1body.getId()).isEqualTo(group1.getId());

    // Add all the permissions, including the one before
    val r2 =
        initRequest(Group.class)
            .endpoint("groups/%s/permissions", group1.getId().toString())
            .body(permissionRequests)
            .post();
    assertThat(r2.getStatusCode()).isEqualTo(CONFLICT);
    assertThat(r2.getBody()).isNotNull();
  }

  /**
   * Add permissions to a group that has all those permissions
   */
  @Test
  @SneakyThrows
  public void addGroupPermissionsToGroup_AllAlreadyExists_Conflict() {
    log.info("Initially adding permissions to the group");
    val r1 =
        initRequest(Group.class)
            .endpoint("groups/%s/permissions", group1.getId().toString())
            .body(permissionRequests)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);
    assertThat(r1.getBody()).isNotNull();
    val r1body = r1.getBody();
    assertThat(r1body.getId()).isEqualTo(group1.getId());

    log.info("Add the same permissions to the group. This means duplicates are being added");
    val r2 =
        initRequest(Group.class)
            .endpoint("groups/%s/permissions", group1.getId().toString())
            .body(permissionRequests)
            .post();
    assertThat(r2.getStatusCode()).isEqualTo(CONFLICT);
    assertThat(r2.getBody()).isNotNull();
  }

  /**
   * Create permissions for the group, using one addPermissionRequest with multiple masks for a policyId
   */
  @Test
  public void addGroupPermissionsToGroup_MultipleMasks_Conflict() {
    val result =
        stream(AccessLevel.values())
            .filter(x -> !x.toString().equals(permissionRequests.get(0).getMask()))
            .findAny();
    assertThat(result).isNotEmpty();
    val differentMask = result.get();

    val newPermRequest =
        PermissionRequest.builder()
            .mask(differentMask)
            .policyId(permissionRequests.get(0).getPolicyId())
            .build();

    val newPolicyIdStringWithAccessLevel =
        ImmutableList.<PermissionRequest>builder()
            .addAll(permissionRequests)
            .add(newPermRequest)
            .build();

    val r1 =
        initStringRequest()
            .endpoint("groups/%s/permissions", group1.getId().toString())
            .body(newPolicyIdStringWithAccessLevel)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(CONFLICT);
    assertThat(r1.getBody()).isNotNull();
  }

  /**
   * Add permissions containing a non-existing policyId to a group
   */
  @Test
  public void addGroupPermissionsToGroup_NonExistentPolicy_NotFound() {
    val nonExistentPolicyId = generateNonExistentId(policyService).toString();

    // inject a non existent id
    permissionRequests.get(1).setPolicyId(nonExistentPolicyId);

    val r1 =
        initStringRequest()
            .endpoint("groups/%s/permissions", group1.getId().toString())
            .body(permissionRequests)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(r1.getBody()).contains(nonExistentPolicyId);
    assertThat(r1.getBody()).doesNotContain(permissionRequests.get(0).getPolicyId());
  }

  /**
   * Happy path
   * Add non-existent permissions to a group
   */
  @Test
  @SneakyThrows
  public void addGroupPermissionsToGroup_Unique_Success() {
    // Add Permissions to group
    val r1 =
        initRequest(Group.class)
            .endpoint("groups/%s/permissions", group1.getId().toString())
            .body(permissionRequests)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);
    assertThat(r1.getBody()).isNotNull();
    val r1body = r1.getBody();
    assertThat(r1body.getId()).isEqualTo(group1.getId());

    // Get the policies for this group
    val r3 = initStringRequest()
        .endpoint("groups/%s/permissions", group1.getId().toString())
        .get();
    assertThat(r3.getStatusCode()).isEqualTo(OK);

    // Analyze results
    val page = MAPPER.readTree(r3.getBody());
    assertThat(page).isNotNull();
    assertThat(page.get("count").asInt()).isEqualTo(2);
    val outputMap =
        Streams.stream(page.path("resultSet").iterator())
            .collect(
                toMap(
                    x -> x.path("policy").path("id").asText(),
                    x -> x.path("accessLevel").asText()));
    assertThat(outputMap)
        .containsKeys(policies.get(0).getId().toString(), policies.get(1).getId().toString());
    assertThat(outputMap.get(policies.get(0).getId().toString()))
        .isEqualTo(AccessLevel.WRITE.toString());
    assertThat(outputMap.get(policies.get(1).getId().toString()))
        .isEqualTo(DENY.toString());
  }


  /** GroupController */





  @Test
  @Ignore
  public void deleteGroupPermissionsForGroup_NonExistent_ThrowsNotFoundException() {
    val r1 =
        initRequest(Group.class)
            .endpoint("groups/%s/permissions", group1.getId().toString())
            .body(permissionRequests)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);
    assertThat(r1.getBody()).isNotNull();
    val r1body = r1.getBody();
    assertThat(r1body.getId()).isEqualTo(group1.getId());
  }


  @Ignore
  @Test
  @SneakyThrows
  public void deleteGroupPermissionsForGroup_AlreadyExists_Success() {
    // Add group permissions
    val r1 =
        initRequest(Group.class)
            .endpoint("groups/%s/permissions", group1.getId().toString())
            .body(permissionRequests)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);
    assertThat(r1.getBody()).isNotNull();
    val r1body = r1.getBody();
    assertThat(r1body.getId()).isEqualTo(group1.getId());

    // Get permissions for the group
    val r2 = initStringRequest().endpoint("groups/%s/permissions", group1.getId().toString()).get();
    assertThat(r2.getStatusCode()).isEqualTo(OK);
    assertThat(r2.getBody()).isNotNull();
    val page = MAPPER.readTree(r2.getBody());
    val existingPermissionIds =
        Streams.stream(page.path("resultSet").iterator())
            .map(x -> x.get("id"))
            .map(JsonNode::asText)
            .collect(toImmutableSet());
    assertThat(existingPermissionIds).hasSize(permissionRequests.size());

    // Delete the permissions for the group
    val r3 =
        initStringRequest()
            .endpoint(
                "groups/%s/permissions/%s",
                group1.getId().toString(), COMMA.join(existingPermissionIds))
            .delete();
    assertThat(r3.getStatusCode()).isEqualTo(OK);

    // Ensure permissions were deleted
    val r4 = initStringRequest().endpoint("groups/%s/permissions", group1.getId().toString()).get();
    assertThat(r4.getStatusCode()).isEqualTo(OK);
    assertThat(r4.getBody()).isNotNull();
    val page4 = MAPPER.readTree(r2.getBody());
    val existingPermissionIds4 =
        Streams.stream(page4.path("resultSet").iterator())
            .map(x -> x.get("id"))
            .map(JsonNode::asText)
            .collect(toImmutableSet());
    assertThat(existingPermissionIds4).isEmpty();
    // Ensure policies still exists
    for (val p : policies) {
      val r5 = initStringRequest().endpoint("policies/%s", p.getId().toString()).get();
      assertThat(r5.getStatusCode()).isEqualTo(OK);
      assertThat(r5.getBody()).isNotNull();
    }

    // Ensure group still exists
    val r6 = initStringRequest().endpoint("groups/%s", group1.getId().toString()).get();
    assertThat(r6.getStatusCode()).isEqualTo(OK);
    assertThat(r6.getBody()).isNotNull();
  }

  @Test
  @Ignore
  public void deleteGroupPermissionsForGroup_EmptyPermissionIds_ThrowsNotFoundException() {}

  @Test
  @Ignore
  public void readGroupPermissionsForGroup_AlreadyExists_Success() {}

  @Test
  @Ignore
  public void readGroupPermissionsForGroup_NonExistent_ThrowsNotFoundException() {}

  /** PolicyController */

  /**
   * Using the policy controller, add a single permission for a non-existent group
   */
  @Test
  public void addGroupPermissionToPolicy_NonExistentGroupId_NotFound() {
    val nonExistentGroupId = generateNonExistentId(groupService);

    val r1 =
        initStringRequest()
            .endpoint(
                "policies/%s/permission/group/%s",
                policies.get(0).getId().toString(), nonExistentGroupId.toString())
            .body(createMaskJson(DENY.toString()))
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(r1.getBody()).contains(nonExistentGroupId.toString());
  }

  /**
   * Using the policy controller, add a single permission for a non-existent policy
   */
  @Test
  public void addGroupPermissionToPolicy_NonExistentPolicyId_NotFound() {
    val nonExistentPolicyId = generateNonExistentId(policyService);

    val r1 =
        initStringRequest()
            .endpoint(
                "policies/%s/permission/group/%s", nonExistentPolicyId, group1.getId().toString())
            .body(createMaskJson(DENY.toString()))
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(r1.getBody()).contains(nonExistentPolicyId.toString());
  }

  /**
   * Add a single permission using the policy controller
   */
  @Test
  @SneakyThrows
  public void addGroupPermissionToPolicy_Unique_Success() {
    val permRequest = permissionRequests.get(0);
    // Create 2 requests with same policy but different groups
    val r1 = initRequest(Group.class)
            .endpoint(
                "policies/%s/permission/group/%s",
                permRequest.getPolicyId(), group1.getId().toString())
            .body(createMaskJson(permRequest.getMask().toString()))
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);
    assertThat(r1.getBody()).isNotNull();
    val r1body = r1.getBody();
    assertThat(r1body.getId()).isEqualTo(group1.getId());

    val r2 = initRequest(Group.class)
            .endpoint(
                "policies/%s/permission/group/%s",
                permRequest.getPolicyId(), group2.getId().toString())
            .body(createMaskJson(permRequest.getMask().toString()))
            .post();
    assertThat(r2.getStatusCode()).isEqualTo(OK);
    assertThat(r2.getBody()).isNotNull();
    val r2body = r2.getBody();
    assertThat(r2body.getId()).isEqualTo(group2.getId());

    // Get the groups for the policy previously used
    val r3 = initStringRequest().endpoint("policies/%s/groups", permRequest.getPolicyId()).get();
    assertThat(r3.getStatusCode()).isEqualTo(OK);

    // Assert that response contains both groupIds, groupNames and policyId
    val body = MAPPER.readTree(r3.getBody());
    assertThat(body).isNotNull();
    val expectedMap =
        Stream.of(group1, group2).collect(Collectors.toMap(x -> x.getId().toString(), x -> x));
    Streams.stream(body.iterator())
        .forEach(
            n -> {
              val actualGroupId = n.path("id").asText();
              val actualGroupName = n.path("name").asText();
              val actualMask = AccessLevel.fromValue(n.path("mask").asText());
              assertThat(expectedMap).containsKey(actualGroupId);
              val expectedGroup = expectedMap.get(actualGroupId);
              assertThat(actualGroupName).isEqualTo(expectedGroup.getName());
              assertThat(actualMask).isEqualTo(permRequest.getMask());
            });
  }

  /**
   * Using the group controller, add a group permission with an undefined mask
   */
  @Test
  public void addGroupPermissionsToGroup_IncorrectMask_BadRequest(){
    // Corrupt the request
    val incorrectMask = "anIncorrectMask";
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> AccessLevel.fromValue(incorrectMask) );

    val body = MAPPER.valueToTree(permissionRequests);
    val firstElement = (ObjectNode)body.get(0);
    firstElement.put("mask", incorrectMask);

    val r1 = initStringRequest()
            .endpoint("groups/%s/permissions", group1.getId().toString())
            .body(body)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  /**
   * Using the policy controller, add a group permission with an undefined mask
   */
  @Test
  public void addGroupPermissionsToPolicy_IncorrectMask_BadRequest(){
    // Corrupt the request
    val incorrectMask = "anIncorrectMask";
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> AccessLevel.fromValue(incorrectMask) );

    // Using the policy controller
    val policyId = permissionRequests.get(0).getPolicyId();
    val r2 = initStringRequest()
        .endpoint( "policies/%s/permission/group/%s", policyId, group1.getId().toString())
        .body(createMaskJson(incorrectMask))
        .post();
    assertThat(r2.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  // Group
  // TODO [rtisma]:  Test 1 - Get permissions when groupId DNE

  // Policy controller
  // TODO [rtisma]: Test 1 - add permissions with policy id DNE
  // TODO [rtisma]: Test 2 - add permissions with group id DNE

  @Test
  @SneakyThrows
  @Ignore
  public void addGroupPermissionsToPolicy_DuplicateRequests_Success() {}

  @Test
  @Ignore
  public void readGroupPermissionsForPolicy_AlreadyExists_Success() {}

  @Test
  @Ignore
  public void readGroupPermissionsForPolicy_NonExistent_ThrowsNotFoundException() {}

  private WebResource<String> initStringRequest() {
    return initRequest(String.class);
  }

  private <T> WebResource<T> initRequest(@NonNull Class<T> responseType) {
    return createWebResource(restTemplate, getServerUrl(), responseType).headers(this.headers);
  }

  private static ObjectNode createMaskJson(String maskStringValue){
    return MAPPER.createObjectNode().put("mask", maskStringValue);
  }

  private String getServerUrl() {
    return "http://localhost:" + port;
  }
}
