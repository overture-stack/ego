package bio.overture.ego.controller;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.service.GroupPermissionService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.PolicyService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.Streams;
import bio.overture.ego.utils.WebResource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
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
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static bio.overture.ego.model.enums.AccessLevel.DENY;
import static bio.overture.ego.model.enums.AccessLevel.WRITE;
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
import static org.springframework.http.HttpStatus.NOT_FOUND;
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
  private static final String INVALID_UUID = "invalidUUID000";
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
  @Autowired private GroupPermissionService groupPermissionService;

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
                    .policyId(policies.get(0).getId())
                    .mask(WRITE)
                    .build())
            .add(
                PermissionRequest.builder()
                    .policyId(policies.get(1).getId())
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
  public void addGroupPermissionsToGroup_DuplicateRequest_Conflict() {
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
    val nonExistentPolicyId = generateNonExistentId(policyService);

    // inject a non existent id
    permissionRequests.get(1).setPolicyId(nonExistentPolicyId);

    val r1 =
        initStringRequest()
            .endpoint("groups/%s/permissions", group1.getId().toString())
            .body(permissionRequests)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(r1.getBody()).contains(nonExistentPolicyId.toString());
    assertThat(r1.getBody()).doesNotContain(permissionRequests.get(0).getPolicyId().toString());
  }

  /**
   * Happy path
   * Add non-existent permissions to a group, and read it back
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
        .isEqualTo(WRITE.toString());
    assertThat(outputMap.get(policies.get(1).getId().toString()))
        .isEqualTo(DENY.toString());
  }


  /** GroupController */

  @Test
  @SneakyThrows
  public void deleteGroupPermissionsForGroup_NonExistent_NotFound() {
    // Add permissions to group1
    val r1 =
        initRequest(Group.class)
            .endpoint("groups/%s/permissions", group1.getId().toString())
            .body(permissionRequests)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);
    assertThat(r1.getBody()).isNotNull();
    val r1body = r1.getBody();
    assertThat(r1body.getId()).isEqualTo(group1.getId());

    // Get permissions for group1
    val r2 = initStringRequest()
        .endpoint("groups/%s/permissions", group1.getId().toString())
        .get();
    assertThat(r2.getStatusCode()).isEqualTo(OK);
    assertThat(r2.getBody()).isNotNull();

    // Assert the expected permission ids exist
    val page = MAPPER.readTree(r2.getBody());
    val existingPermissionIds =
        Streams.stream(page.path("resultSet").iterator())
            .map(x -> x.get("id"))
            .map(JsonNode::asText)
            .collect(toImmutableSet());
    assertThat(existingPermissionIds).hasSize(permissionRequests.size());

    // Attempt to delete permissions for a nonExistent group
    val nonExistentGroupId = generateNonExistentId(groupService);
    val r3 = initStringRequest()
        .endpoint(
            "groups/%s/permissions/%s",
            nonExistentGroupId, COMMA.join(existingPermissionIds))
        .delete();
    assertThat(r3.getStatusCode()).isEqualTo(NOT_FOUND);

    // Attempt to delete permissions for an existing group but a non-existent permission id
    val nonExistentPermissionId = generateNonExistentId(groupPermissionService).toString();
    val someExistingPermissionIds = Sets.<String>newHashSet();
    someExistingPermissionIds.addAll(existingPermissionIds);
    someExistingPermissionIds.add(nonExistentPermissionId);
    assertThat(groupService.isExist(group1.getId())).isTrue();
    val r4 = initStringRequest()
        .endpoint(
            "groups/%s/permissions/%s",
            group1.getId(), COMMA.join(someExistingPermissionIds))
        .delete();
    assertThat(r4.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  @SneakyThrows
  public void deleteGroupPermissionsForPolicy_NonExistentGroup_NotFound() {
    val permRequest = permissionRequests.get(0);
    val policyId = permRequest.getPolicyId();
    val nonExistingGroupId = generateNonExistentId(groupService);
    val r3 = initRequest(Group.class)
        .endpoint( "policies/%s/permission/group/%s", policyId, nonExistingGroupId)
        .delete();
    assertThat(r3.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  @SneakyThrows
  public void deleteGroupPermissionsForPolicy_NonExistentPolicy_NotFound() {
    val nonExistentPolicyId = generateNonExistentId(policyService);
    val r3 = initRequest(Group.class)
        .endpoint( "policies/%s/permission/group/%s", nonExistentPolicyId, group1.getId())
        .delete();
    assertThat(r3.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  @SneakyThrows
  public void deleteGroupPermissionsForPolicy_DuplicateRequest_NotFound() {
    val permRequest = permissionRequests.get(0);
    val policyId = permRequest.getPolicyId();
    val mask = permRequest.getMask();

    // Create a permission
    val r1 = initRequest(Group.class)
        .endpoint(
            "policies/%s/permission/group/%s", policyId, group1.getId())
        .body(createMaskJson(mask.toString()))
        .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);
    assertThat(r1.getBody()).isNotNull();

    // Assert the permission exists
    val r2 = initStringRequest()
        .endpoint("groups/%s/permissions", group1.getId())
        .get();
    assertThat(r2.getStatusCode()).isEqualTo(OK);
    assertThat(r2.getBody()).isNotNull();
    val page = MAPPER.readTree(r2.getBody());
    val existingPermissionIds =
        Streams.stream(page.path("resultSet").iterator())
            .map(x -> x.get("id"))
            .map(JsonNode::asText)
            .collect(toImmutableSet());
    assertThat(existingPermissionIds).hasSize(1);

    // Delete an existing permission
    val r3 = initRequest(Group.class)
        .endpoint( "policies/%s/permission/group/%s", policyId, group1.getId())
        .delete();
    assertThat(r3.getStatusCode()).isEqualTo(OK);
    assertThat(r3.getBody()).isNotNull();

    // Assert the permission no longer exists
    val r4 = initStringRequest()
        .endpoint("groups/%s/permissions", group1.getId())
        .get();
    assertThat(r4.getStatusCode()).isEqualTo(OK);
    assertThat(r4.getBody()).isNotNull();
    val page2 = MAPPER.readTree(r4.getBody());
    val actualPermissionIds  =
        Streams.stream(page2.path("resultSet").iterator())
            .map(x -> x.get("id"))
            .map(JsonNode::asText)
            .collect(toImmutableSet());
    assertThat(actualPermissionIds).isEmpty();

    // Delete an existing permission
    val r5 = initRequest(Group.class)
        .endpoint( "policies/%s/permission/group/%s", policyId, group1.getId())
        .delete();
    assertThat(r5.getStatusCode()).isEqualTo(NOT_FOUND);
    assertThat(r5.getBody()).isNotNull();
  }

  @Test
  @SneakyThrows
  public void deleteGroupPermissionsForPolicy_AlreadyExists_Success() {
    val permRequest = permissionRequests.get(0);
    val policyId = permRequest.getPolicyId();
    val mask = permRequest.getMask();

    // Create a permission
    val r1 = initRequest(Group.class)
        .endpoint(
            "policies/%s/permission/group/%s", policyId, group1.getId())
        .body(createMaskJson(mask.toString()))
        .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);
    assertThat(r1.getBody()).isNotNull();

    // Assert the permission exists
    val r2 = initStringRequest()
        .endpoint("groups/%s/permissions", group1.getId())
        .get();
    assertThat(r2.getStatusCode()).isEqualTo(OK);
    assertThat(r2.getBody()).isNotNull();
    val page = MAPPER.readTree(r2.getBody());
    val existingPermissionIds =
        Streams.stream(page.path("resultSet").iterator())
            .map(x -> x.get("id"))
            .map(JsonNode::asText)
            .collect(toImmutableSet());
    assertThat(existingPermissionIds).hasSize(1);

    // Delete an existing permission
    val r3 = initRequest(Group.class)
            .endpoint( "policies/%s/permission/group/%s", policyId, group1.getId())
        .delete();
    assertThat(r3.getStatusCode()).isEqualTo(OK);
    assertThat(r3.getBody()).isNotNull();

    // Assert the permission no longer exists
    val r4 = initStringRequest()
        .endpoint("groups/%s/permissions", group1.getId())
        .get();
    assertThat(r4.getStatusCode()).isEqualTo(OK);
    assertThat(r4.getBody()).isNotNull();
    val page2 = MAPPER.readTree(r4.getBody());
    val actualPermissionIds  =
        Streams.stream(page2.path("resultSet").iterator())
            .map(x -> x.get("id"))
            .map(JsonNode::asText)
            .collect(toImmutableSet());
    assertThat(actualPermissionIds).isEmpty();
  }

  @Test
  @SneakyThrows
  public void deleteGroupPermissionsForGroup_AlreadyExists_Success() {
    // Add group permissions
    val r1 = initRequest(Group.class)
            .endpoint("groups/%s/permissions", group1.getId().toString())
            .body(permissionRequests)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);
    assertThat(r1.getBody()).isNotNull();
    val r1body = r1.getBody();
    assertThat(r1body.getId()).isEqualTo(group1.getId());

    // Get permissions for the group
    val r2 = initStringRequest()
        .endpoint("groups/%s/permissions", group1.getId().toString())
        .get();
    assertThat(r2.getStatusCode()).isEqualTo(OK);
    assertThat(r2.getBody()).isNotNull();

    // Assert the expected permission ids exist
    val page = MAPPER.readTree(r2.getBody());
    val existingPermissionIds =
        Streams.stream(page.path("resultSet").iterator())
            .map(x -> x.get("id"))
            .map(JsonNode::asText)
            .collect(toImmutableSet());
    assertThat(existingPermissionIds).hasSize(permissionRequests.size());

    // Delete the permissions for the group
    val r3 = initStringRequest()
            .endpoint(
                "groups/%s/permissions/%s",
                group1.getId().toString(), COMMA.join(existingPermissionIds))
            .delete();
    assertThat(r3.getStatusCode()).isEqualTo(OK);

    // Assert the expected permissions were deleted
    val r4 = initStringRequest().endpoint("groups/%s/permissions", group1.getId().toString()).get();
    assertThat(r4.getStatusCode()).isEqualTo(OK);
    assertThat(r4.getBody()).isNotNull();
    val page4 = MAPPER.readTree(r4.getBody());
    val existingPermissionIds4 =
        Streams.stream(page4.path("resultSet").iterator())
            .map(x -> x.get("id"))
            .map(JsonNode::asText)
            .collect(toImmutableSet());
    assertThat(existingPermissionIds4).isEmpty();

    // Assert that the policies still exists
    policies.forEach(p -> {
      val r5 = initStringRequest().endpoint("policies/%s", p.getId().toString()).get();
      assertThat(r5.getStatusCode()).isEqualTo(OK);
      assertThat(r5.getBody()).isNotNull();
    });

    // Assert the group still exists
    val r6 = initStringRequest().endpoint("groups/%s", group1.getId().toString()).get();
    assertThat(r6.getStatusCode()).isEqualTo(OK);
    assertThat(r6.getBody()).isNotNull();
  }

  /**
   * Using the group controller, attempt to read a permission belonging to a non-existent group
   */
  @Test
  public void readGroupPermissionsForGroup_NonExistent_NotFound() {
    val nonExistentGroupId = generateNonExistentId(groupService);
    val r1 = initStringRequest()
        .endpoint("groups/%s/permissions", nonExistentGroupId)
        .get();
    assertThat(r1.getStatusCode()).isEqualTo(NOT_FOUND);
  }

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


  @Test
  public void uuidValidationForGroup_MalformedUUID_BadRequest(){
    val r1 = initStringRequest()
        .endpoint("groups/%s/permissions", INVALID_UUID)
        .body(permissionRequests)
        .post();
    assertThat(r1.getStatusCode()).isEqualTo(BAD_REQUEST);

    val r4 = initStringRequest()
        .endpoint("groups/%s/permissions", INVALID_UUID)
        .get();
    assertThat(r4.getStatusCode()).isEqualTo(BAD_REQUEST);

    val r5 = initStringRequest()
        .endpoint( "groups/%s/permissions/%s", UUID.randomUUID(), INVALID_UUID)
        .delete();
    assertThat(r5.getStatusCode()).isEqualTo(BAD_REQUEST);

    val r6 = initStringRequest()
        .endpoint( "groups/%s/permissions/%s", INVALID_UUID, UUID.randomUUID())
        .delete();
    assertThat(r6.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void uuidValidationForPolicy_MalformedUUID_BadRequest(){
    val r1 = initStringRequest()
        .endpoint("policies/%s/permission/group/%s",
            UUID.randomUUID(), INVALID_UUID)
        .delete();
    assertThat(r1.getStatusCode()).isEqualTo(BAD_REQUEST);

    val r2 = initStringRequest()
        .endpoint( "policies/%s/permission/group/%s",
            UUID.randomUUID(), INVALID_UUID)
        .body(createMaskJson(WRITE.toString()))
        .post();
    assertThat(r2.getStatusCode()).isEqualTo(BAD_REQUEST);

    val r3 = initStringRequest()
        .endpoint( "policies/%s/permission/group/%s",
            INVALID_UUID, UUID.randomUUID())
        .body(createMaskJson(WRITE.toString()))
        .post();
    assertThat(r3.getStatusCode()).isEqualTo(BAD_REQUEST);

    val r4 = initStringRequest()
        .endpoint("policies/%s/permission/group/%s",
            INVALID_UUID, UUID.randomUUID())
        .delete();
    assertThat(r4.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  @Test
  @SneakyThrows
  public void addGroupPermissionsToPolicy_DuplicateRequests_Conflict() {
    val permRequest = permissionRequests.get(0);
    // Create 2 identical requests with same policy and group
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
            permRequest.getPolicyId(), group1.getId().toString())
        .body(createMaskJson(permRequest.getMask().toString()))
        .post();
    assertThat(r2.getStatusCode()).isEqualTo(CONFLICT);
  }

  @Test
  @SneakyThrows
  public void updateGroupPermissionsToGroup_AlreadyExists_Success() {
    val permRequest1 = permissionRequests.get(0);
    val permRequest2 = permissionRequests.get(1);
    val updatedMask = permRequest2.getMask();
    val updatedPermRequest1 = PermissionRequest.builder()
        .policyId(permRequest1.getPolicyId())
        .mask(updatedMask)
        .build();

    assertThat(updatedMask).isNotEqualTo(permRequest1.getMask());
    assertThat(permRequest1.getMask()).isNotEqualTo(permRequest2.getMask());

    // Create permission for group
    val r1 = initRequest(Group.class)
        .endpoint("groups/%s/permissions", group1.getId().toString())
        .body(ImmutableList.of(permRequest1))
        .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Get created permissions
    val r2 = initStringRequest()
        .endpoint("groups/%s/permissions", group1.getId())
        .get();
    assertThat(r2.getStatusCode()).isEqualTo(OK);
    assertThat(r2.getBody()).isNotNull();

    // Assert created permission is correct mask
    val page = MAPPER.readTree(r2.getBody());
    val existingPermissions =
        Streams.stream(page.path("resultSet").iterator())
            .map(x -> MAPPER.convertValue(x, GroupPermission.class))
            .collect(toImmutableList());
    assertThat(existingPermissions).hasSize(1);
    val permission = existingPermissions.get(0);
    assertThat(permission.getAccessLevel()).isEqualTo(permRequest1.getMask());

    // Update the group permission
    val r3 = initRequest(Group.class)
        .endpoint("groups/%s/permissions", group1.getId().toString())
        .body(ImmutableList.of(updatedPermRequest1))
        .post();
    assertThat(r3.getStatusCode()).isEqualTo(OK);

    // Get updated permissions
    val r4 = initStringRequest()
        .endpoint("groups/%s/permissions", group1.getId())
        .get();
    assertThat(r4.getStatusCode()).isEqualTo(OK);
    assertThat(r4.getBody()).isNotNull();

    // Assert updated permission is correct mask
    val page2 = MAPPER.readTree(r4.getBody());
    val existingPermissions2 =
        Streams.stream(page2.path("resultSet").iterator())
            .map(x -> MAPPER.convertValue(x, GroupPermission.class))
            .collect(toImmutableList());
    assertThat(existingPermissions2).hasSize(1);
    val permission2 = existingPermissions2.get(0);
    assertThat(permission2.getAccessLevel()).isEqualTo(updatedMask);
  }

  @Test
  @SneakyThrows
  public void updateGroupPermissionsToPolicy_AlreadyExists_Success() {
    val permRequest1 = permissionRequests.get(0);
    val permRequest2 = permissionRequests.get(1);
    assertThat(permRequest1.getMask()).isNotEqualTo(permRequest2.getMask());

    // Create permission for group and policy
    val r1 = initRequest(Group.class)
        .endpoint(
            "policies/%s/permission/group/%s",
            permRequest1.getPolicyId(), group1.getId().toString())
        .body(createMaskJson(permRequest1.getMask().toString()))
        .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Get created permissions
    val r2 = initStringRequest()
        .endpoint("groups/%s/permissions", group1.getId())
        .get();
    assertThat(r2.getStatusCode()).isEqualTo(OK);
    assertThat(r2.getBody()).isNotNull();

    // Assert created permission is correct mask
    val page = MAPPER.readTree(r2.getBody());
    val existingPermissions =
        Streams.stream(page.path("resultSet").iterator())
            .map(x -> MAPPER.convertValue(x, GroupPermission.class))
            .collect(toImmutableList());
    assertThat(existingPermissions).hasSize(1);
    val permission = existingPermissions.get(0);
    assertThat(permission.getAccessLevel()).isEqualTo(permRequest1.getMask());

    // Update the group permission
    val r3 = initRequest(Group.class)
        .endpoint(
            "policies/%s/permission/group/%s",
            permRequest1.getPolicyId(), group1.getId().toString())
        .body(createMaskJson(permRequest2.getMask().toString()))
        .post();
    assertThat(r3.getStatusCode()).isEqualTo(OK);

    // Get updated permissions
    val r4 = initStringRequest()
        .endpoint("groups/%s/permissions", group1.getId())
        .get();
    assertThat(r4.getStatusCode()).isEqualTo(OK);
    assertThat(r4.getBody()).isNotNull();

    // Assert updated permission is correct mask
    val page2 = MAPPER.readTree(r4.getBody());
    val existingPermissions2 =
        Streams.stream(page2.path("resultSet").iterator())
            .map(x -> MAPPER.convertValue(x, GroupPermission.class))
            .collect(toImmutableList());
    assertThat(existingPermissions2).hasSize(1);
    val permission2 = existingPermissions2.get(0);
    assertThat(permission2.getAccessLevel()).isEqualTo(permRequest2.getMask());
  }

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
