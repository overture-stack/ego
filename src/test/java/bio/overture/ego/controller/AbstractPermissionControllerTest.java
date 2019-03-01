package bio.overture.ego.controller;

import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.entity.AbstractPermission;
import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.entity.NameableEntity;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.service.AbstractPermissionService;
import bio.overture.ego.service.NamedService;
import bio.overture.ego.service.PolicyService;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.Streams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static bio.overture.ego.model.enums.AccessLevel.DENY;
import static bio.overture.ego.model.enums.AccessLevel.WRITE;
import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static bio.overture.ego.utils.Collectors.toImmutableList;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentId;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentName;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.uniqueIndex;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
public abstract class AbstractPermissionControllerTest< O extends NameableEntity<UUID>,
    P extends AbstractPermission<O >> extends AbstractControllerTest {

  /** Constants */
  private static final String INVALID_UUID = "invalidUUID000";

  /** State */

  private O owner1;
  private O owner2;
  private List<Policy> policies;
  private List<PermissionRequest> permissionRequests;

  @Override
  protected void beforeTest() {
    // Initial setup of entities (run once)
    this.owner1 = generateOwner(generateNonExistentOwnerName());
    this.owner2 = generateOwner(generateNonExistentOwnerName());
    this.policies =
        IntStream.range(0, 2)
            .boxed()
            .map(x -> generateNonExistentName(getPolicyService()))
            .map(x -> getEntityGenerator().setupSinglePolicy(x))
            .collect(toImmutableList());

    this.permissionRequests =
        ImmutableList.<PermissionRequest>builder()
            .add(PermissionRequest.builder().policyId(policies.get(0).getId()).mask(WRITE).build())
            .add(PermissionRequest.builder().policyId(policies.get(1).getId()).mask(DENY).build())
            .build();

    // Sanity check
    assertThat(getOwnerService().isExist(owner1.getId())).isTrue();
    policies.forEach(p -> assertThat(getPolicyService().isExist(p.getId())).isTrue());
  }

  /** Add permissions to a non-existent owner */
  @Test
  public void addPermissionsToOwner_NonExistentOwner_NotFound() {
    val nonExistentOwnerId = generateNonExistentId(getOwnerService());
    val r1 =
        initStringRequest()
            .endpoint(getAddPermissionsEndpoint(nonExistentOwnerId))
            .body(permissionRequests)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(r1.getBody()).contains(nonExistentOwnerId.toString());
  }

  /** Attempt to add an empty list of permission request to an owner */
  @Test
  @SneakyThrows
  public void addPermissionsToOwner_EmptyPermissionRequests_Conflict() {
    // Add some of the permissions
    val r1 =
        initStringRequest()
            .endpoint(getAddPermissionsEndpoint(owner1.getId()))
            .body(newArrayList())
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  /** Add permissions to an owner that has SOME those permissions */
  @Test
  @SneakyThrows
  public void addPermissionsToOwner_SomeAlreadyExists_Conflict() {
    val somePermissionRequests = ImmutableList.of(permissionRequests.get(0));

    // Add some of the permissions
    val r1 =
        initRequest(getOwnerType())
            .endpoint(getAddPermissionsEndpoint(owner1.getId()))
            .body(somePermissionRequests)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);
    assertThat(r1.getBody()).isNotNull();
    val r1body = r1.getBody();
    assertThat(r1body.getId()).isEqualTo(owner1.getId());

    // Add all the permissions, including the one before
    val r2 =
        initRequest(getOwnerType())
            .endpoint(getAddPermissionsEndpoint(owner1.getId()))
            .body(permissionRequests)
            .post();
    assertThat(r2.getStatusCode()).isEqualTo(CONFLICT);
    assertThat(r2.getBody()).isNotNull();
  }

  /** Add permissions to an owner that has all those permissions */
  @Test
  @SneakyThrows
  public void addPermissionsToOwner_DuplicateRequest_Conflict() {
    log.info("Initially adding permissions to the owner");
    val r1 =
        initRequest(getOwnerType())
            .endpoint(getAddPermissionsEndpoint(owner1.getId()))
            .body(permissionRequests)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);
    assertThat(r1.getBody()).isNotNull();
    val r1body = r1.getBody();
    assertThat(r1body.getId()).isEqualTo(owner1.getId());

    log.info("Add the same permissions to the owner. This means duplicates are being added");
    val r2 =
        initRequest(getOwnerType())
            .endpoint(getAddPermissionsEndpoint(owner1.getId()))
            .body(permissionRequests)
            .post();
    assertThat(r2.getStatusCode()).isEqualTo(CONFLICT);
    assertThat(r2.getBody()).isNotNull();
  }

  /**
   * Create permissions for the owner, using one addPermissionRequest with multiple masks for a
   * policyId
   */
  @Test
  public void addPermissionsToOwner_MultipleMasks_Conflict() {
    val result =
        stream(AccessLevel.values())
            .filter(x -> !x.equals(permissionRequests.get(0).getMask()))
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
            .endpoint(getAddPermissionsEndpoint(owner1.getId()))
            .body(newPolicyIdStringWithAccessLevel)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(CONFLICT);
    assertThat(r1.getBody()).isNotNull();
  }

  /** Add permissions containing a non-existing policyId to an owner */
  @Test
  public void addPermissionsToOwner_NonExistentPolicy_NotFound() {
    val nonExistentPolicyId = generateNonExistentId(getPolicyService());

    // inject a non existent id
    permissionRequests.get(1).setPolicyId(nonExistentPolicyId);

    val r1 =
        initStringRequest()
            .endpoint(getAddPermissionsEndpoint(owner1.getId()))
            .body(permissionRequests)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(r1.getBody()).contains(nonExistentPolicyId.toString());
    assertThat(r1.getBody()).doesNotContain(permissionRequests.get(0).getPolicyId().toString());
  }

  @Test
  @SneakyThrows
  public void addPermissions_CreateAndUpdate_Success() {
    val permRequest1 = permissionRequests.get(0);
    val permRequest2 = permissionRequests.get(1);
    assertThat(permRequest1.getMask()).isNotEqualTo(permRequest2.getMask());

    // Add initial Permission
    val r1 =
        initStringRequest()
            .endpoint(getAddPermissionsEndpoint(owner1.getId()))
            .body(ImmutableList.of(permRequest1))
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Update permRequest1 locally
    val updatePermRequest1 =
        PermissionRequest.builder()
            .policyId(permRequest1.getPolicyId())
            .mask(permRequest2.getMask())
            .build();

    // call addPerms for [updatedPermRequest1, permRequest2]
    val r2 =
        initStringRequest()
            .endpoint(getAddPermissionsEndpoint(owner1.getId()))
            .body(ImmutableList.of(updatePermRequest1, permRequest2))
            .post();
    assertThat(r2.getStatusCode()).isEqualTo(OK);

    // Get permissions for owner
    val r3 = initStringRequest()
        .endpoint(getReadPermissionsEndpoint(owner1.getId()))
        .get();
    assertThat(r3.getStatusCode()).isEqualTo(OK);
    assertThat(r3.getBody()).isNotNull();

    // Assert created permission is correct mask
    val page = MAPPER.readTree(r3.getBody());
    val existingPermissionIndex =
        Streams.stream(page.path("resultSet").iterator())
            .map(x -> MAPPER.convertValue(x, getPermissionType()))
            .collect(toMap(x -> x.getPolicy().getId(), identity()));
    assertThat(existingPermissionIndex.values()).hasSize(2);

    // verify permission with permRequest1.getPolicyId() and owner, has same mask as
    // updatedPermRequest1.getMask()
    assertThat(existingPermissionIndex).containsKey(updatePermRequest1.getPolicyId());
    assertThat(existingPermissionIndex.get(updatePermRequest1.getPolicyId()).getAccessLevel())
        .isEqualTo(updatePermRequest1.getMask());

    // verify permission with permRequest2.getPolicyId() and owner, has same mask as
    // permRequest2.getMask();
    assertThat(existingPermissionIndex).containsKey(permRequest2.getPolicyId());
    assertThat(existingPermissionIndex.get(permRequest2.getPolicyId()).getAccessLevel())
        .isEqualTo(permRequest2.getMask());
  }

  /** Happy path Add non-existent permissions to an owner, and read it back */
  @Test
  @SneakyThrows
  public void addPermissionsToOwner_Unique_Success() {
    // Add Permissions to owner
    val r1 =
        initStringRequest()
            .endpoint(getAddPermissionsEndpoint(owner1.getId()))
            .body(permissionRequests)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Get the policies for this owner
    val r3 = initStringRequest()
        .endpoint(getReadPermissionsEndpoint(owner1.getId()))
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
    assertThat(outputMap.get(policies.get(0).getId().toString())).isEqualTo(WRITE.toString());
    assertThat(outputMap.get(policies.get(1).getId().toString())).isEqualTo(DENY.toString());
  }

  @Test
  @SneakyThrows
  public void deletePolicyWithPermissions_AlreadyExists_Success() {
    // Add Permissions to owner
    val permRequest = permissionRequests.get(0);
    val body = ImmutableList.of(permRequest);
    val r1 =
        initStringRequest()
            .endpoint(getAddPermissionsEndpoint(owner1.getId()))
            .body(body)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Get the policies for this owner
    val r2 = initStringRequest()
        .endpoint(getReadPermissionsEndpoint(owner1.getId()))
        .get();
    assertThat(r2.getStatusCode()).isEqualTo(OK);

    // Assert the expected permission ids exist
    val page = MAPPER.readTree(r2.getBody());
    val existingPermissionIds =
        Streams.stream(page.path("resultSet").iterator())
            .map(x -> x.get("id"))
            .map(JsonNode::asText)
            .collect(toImmutableSet());
    assertThat(existingPermissionIds).hasSize(1);

    // Delete the policy
    val r3 = initStringRequest()
        .endpoint("policies/%s", permRequest.getPolicyId())
        .delete();
    assertThat(r3.getStatusCode()).isEqualTo(OK);

    // Assert that the policy deletion cascaded the delete to the permissions
    existingPermissionIds
        .stream()
        .map(UUID::fromString)
        .forEach(x -> assertThat(getPermissionService().isExist(x)).isFalse());

    // Assert that the policy deletion DID NOT cascade past the permissions and delete the owner
    assertThat(getOwnerService().isExist(owner1.getId())).isTrue();
  }

  @Test
  @SneakyThrows
  public void deleteOwnerWithPermissions_AlreadyExists_Success() {
    // Add Permissions to owner
    val r1 =
        initStringRequest()
            .endpoint(getAddPermissionsEndpoint(owner1.getId()))
            .body(permissionRequests)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Get the policies for this owner
    val r2 = initStringRequest()
        .endpoint(getReadPermissionsEndpoint(owner1.getId()))
        .get();
    assertThat(r2.getStatusCode()).isEqualTo(OK);

    // Assert the expected permission ids exist
    val page = MAPPER.readTree(r2.getBody());
    val existingPermissionIds =
        Streams.stream(page.path("resultSet").iterator())
            .map(x -> x.get("id"))
            .map(JsonNode::asText)
            .collect(toImmutableSet());
    assertThat(existingPermissionIds).hasSize(permissionRequests.size());

    // Delete the owner
    val r3 = initStringRequest()
        .endpoint(getDeleteOwnerEndpoint(owner1.getId()))
        .delete();
    assertThat(r3.getStatusCode()).isEqualTo(OK);

    // Assert that the owner deletion cascaded the delete to the permissions
    existingPermissionIds
        .stream()
        .map(UUID::fromString)
        .forEach(x -> assertThat(getPermissionService().isExist(x)).isFalse());

    // Assert that the owner deletion DID NOT cascade past the permission and deleted policies
    permissionRequests
        .stream()
        .map(PermissionRequest::getPolicyId)
        .distinct()
        .forEach(x -> assertThat(getPolicyService().isExist(x)).isTrue());
  }

  @Test
  @SneakyThrows
  public void deletePermissionsForOwner_NonExistent_NotFound() {
    // Add permissions to owner
    val r1 = initStringRequest()
            .endpoint(getAddPermissionsEndpoint(owner1.getId()))
            .body(permissionRequests)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Get permissions for owner
    val r2 = initStringRequest()
        .endpoint(getReadPermissionsEndpoint(owner1.getId()))
        .get();
    assertThat(r2.getStatusCode()).isEqualTo(OK);
    assertThat(r2.getBody()).isNotNull();

    // Assert the expected permission ids exist
    val page = MAPPER.readTree(r2.getBody());
    val existingPermissionIds =
        Streams.stream(page.path("resultSet").iterator())
            .map(x -> x.get("id"))
            .map(JsonNode::asText)
            .map(UUID::fromString)
            .collect(toImmutableSet());
    assertThat(existingPermissionIds).hasSize(permissionRequests.size());

    // Attempt to delete permissions for a nonExistent owner
    val nonExistentOwnerId = generateNonExistentId(getOwnerService());
    val r3 =
        initStringRequest()
            .endpoint(getDeletePermissionsEndpoint(nonExistentOwnerId, existingPermissionIds))
            .delete();
    assertThat(r3.getStatusCode()).isEqualTo(NOT_FOUND);

    // Attempt to delete permissions for an existing owner but a non-existent permission id
    val nonExistentPermissionId = generateNonExistentId(getPermissionService());
    val someExistingPermissionIds = Sets.<UUID>newHashSet();
    someExistingPermissionIds.addAll(existingPermissionIds);
    someExistingPermissionIds.add(nonExistentPermissionId);
    assertThat(getOwnerService().isExist(owner1.getId())).isTrue();
    val r4 = initStringRequest()
            .endpoint(getDeletePermissionsEndpoint(owner1.getId(), someExistingPermissionIds))
            .delete();
    assertThat(r4.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  @SneakyThrows
  public void deletePermissionsForPolicy_NonExistentOwner_NotFound() {
    val permRequest = permissionRequests.get(0);
    val policyId = permRequest.getPolicyId();
    val nonExistingOwnerId = generateNonExistentId(getOwnerService());
    val r3 = initStringRequest()
            .endpoint(getDeletePermissionEndpoint(policyId, nonExistingOwnerId))
            .delete();
    assertThat(r3.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  @SneakyThrows
  public void deletePermissionsForPolicy_NonExistentPolicy_NotFound() {
    val nonExistentPolicyId = generateNonExistentId(getPolicyService());
    val r3 = initStringRequest()
        .endpoint(getDeletePermissionEndpoint(nonExistentPolicyId, owner1.getId()))
        .delete();
    assertThat(r3.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  @Test
  @SneakyThrows
  public void deletePermissionsForPolicy_DuplicateRequest_NotFound() {
    val permRequest = permissionRequests.get(0);
    val policyId = permRequest.getPolicyId();
    val mask = permRequest.getMask();

    // Create a permission
    val r1 =
        initStringRequest()
            .endpoint(getAddPermissionEndpoint(policyId, owner1.getId()))
            .body(createMaskJson(mask.toString()))
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Assert the permission exists
    val r2 = initStringRequest()
        .endpoint(getReadPermissionsEndpoint(owner1.getId()))
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
    val r3 = initStringRequest()
        .endpoint(getDeletePermissionEndpoint(policyId, owner1.getId()))
        .delete();
    assertThat(r3.getStatusCode()).isEqualTo(OK);
    assertThat(r3.getBody()).isNotNull();

    // Assert the permission no longer exists
    val r4 = initStringRequest()
        .endpoint(getReadPermissionsEndpoint(owner1.getId()))
        .get();
    assertThat(r4.getStatusCode()).isEqualTo(OK);
    assertThat(r4.getBody()).isNotNull();
    val page2 = MAPPER.readTree(r4.getBody());
    val actualPermissionIds =
        Streams.stream(page2.path("resultSet").iterator())
            .map(x -> x.get("id"))
            .map(JsonNode::asText)
            .collect(toImmutableSet());
    assertThat(actualPermissionIds).isEmpty();

    // Delete an existing permission
    val r5 =
        initStringRequest()
            .endpoint(getDeletePermissionEndpoint(policyId, owner1.getId()))
            .delete();
    assertThat(r5.getStatusCode()).isEqualTo(NOT_FOUND);
    assertThat(r5.getBody()).isNotNull();
  }

  @Test
  @SneakyThrows
  public void deletePermissionsForPolicy_AlreadyExists_Success() {
    val permRequest = permissionRequests.get(0);
    val policyId = permRequest.getPolicyId();
    val mask = permRequest.getMask();

    // Create a permission
    val r1 =
        initStringRequest()
            .endpoint(getDeletePermissionEndpoint(policyId, owner1.getId()))
            .body(createMaskJson(mask.toString()))
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);
    assertThat(r1.getBody()).isNotNull();

    // Assert the permission exists
    val r2 = initStringRequest()
        .endpoint(getReadPermissionsEndpoint(owner1.getId()))
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
    val r3 =
        initStringRequest()
            .endpoint(getDeletePermissionEndpoint(policyId, owner1.getId()))
            .delete();
    assertThat(r3.getStatusCode()).isEqualTo(OK);
    assertThat(r3.getBody()).isNotNull();

    // Assert the permission no longer exists
    val r4 = initStringRequest()
        .endpoint(getReadPermissionsEndpoint(owner1.getId()))
        .get();
    assertThat(r4.getStatusCode()).isEqualTo(OK);
    assertThat(r4.getBody()).isNotNull();
    val page2 = MAPPER.readTree(r4.getBody());
    val actualPermissionIds =
        Streams.stream(page2.path("resultSet").iterator())
            .map(x -> x.get("id"))
            .map(JsonNode::asText)
            .collect(toImmutableSet());
    assertThat(actualPermissionIds).isEmpty();
  }

  @Test
  @SneakyThrows
  public void deletePermissionsForOwner_AlreadyExists_Success() {
    // Add owner permissions
    val r1 =
        initStringRequest()
            .endpoint(getAddPermissionsEndpoint(owner1.getId()))
            .body(permissionRequests)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Get permissions for the owner
    val r2 = initStringRequest()
        .endpoint(getReadPermissionsEndpoint(owner1.getId()))
        .get();
    assertThat(r2.getStatusCode()).isEqualTo(OK);
    assertThat(r2.getBody()).isNotNull();

    // Assert the expected permission ids exist
    val page = MAPPER.readTree(r2.getBody());
    val existingPermissionIds =
        Streams.stream(page.path("resultSet").iterator())
            .map(x -> x.get("id"))
            .map(JsonNode::asText)
            .map(UUID::fromString)
            .collect(toImmutableSet());
    assertThat(existingPermissionIds).hasSize(permissionRequests.size());

    // Delete the permissions for the owner
    val r3 =
        initStringRequest()
            .endpoint(getDeletePermissionsEndpoint(owner1.getId(), existingPermissionIds))
            .delete();
    assertThat(r3.getStatusCode()).isEqualTo(OK);

    // Assert the expected permissions were deleted
    val r4 = initStringRequest()
        .endpoint(getReadPermissionsEndpoint(owner1.getId()))
        .get();
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
    policies.forEach(
        p -> {
          val r5 = initStringRequest()
              .endpoint("policies/%s", p.getId().toString())
              .get();
          assertThat(r5.getStatusCode()).isEqualTo(OK);
          assertThat(r5.getBody()).isNotNull();
        });

    // Assert the owner still exists
    assertThat(getOwnerService().isExist(owner1.getId())).isTrue();
  }

  /** Using the owners controller, attempt to read a permission belonging to a non-existent owner */
  @Test
  public void readPermissionsForOwner_NonExistent_NotFound() {
    val nonExistentOwnerId = generateNonExistentId(getOwnerService());
    val r1 = initStringRequest()
        .endpoint(getReadPermissionsEndpoint(nonExistentOwnerId))
        .get();
    assertThat(r1.getStatusCode()).isEqualTo(NOT_FOUND);
  }

  /** PolicyController */

  /** Using the policy controller, add a single permission for a non-existent owner */
  @Test
  public void addPermissionToPolicy_NonExistentOwnerId_NotFound() {
    val nonExistentOwnerId = generateNonExistentId(getOwnerService());

    val r1 = initStringRequest()
        .endpoint(getAddPermissionEndpoint(policies.get(0).getId(), nonExistentOwnerId))
        .body(createMaskJson(DENY.toString()))
        .post();
    assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(r1.getBody()).contains(nonExistentOwnerId.toString());
  }

  /** Using the policy controller, add a single permission for a non-existent policy */
  @Test
  public void addPermissionToPolicy_NonExistentPolicyId_NotFound() {
    val nonExistentPolicyId = generateNonExistentId(getPolicyService());

    val r1 =
        initStringRequest()
            .endpoint(getAddPermissionEndpoint(nonExistentPolicyId, owner1.getId()))
            .body(createMaskJson(DENY.toString()))
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(r1.getBody()).contains(nonExistentPolicyId.toString());
  }

  /** Add a single permission using the policy controller */
  @Test
  @SneakyThrows
  public void addPermissionToPolicy_Unique_Success() {
    val permRequest = permissionRequests.get(0);

    // Create 2 requests with same policy but different owners
    val r1 = initStringRequest()
            .endpoint(getAddPermissionEndpoint(permRequest.getPolicyId(), owner1.getId()))
            .body(createMaskJson(permRequest.getMask().toString()))
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    val r2 = initStringRequest()
        .endpoint(getAddPermissionEndpoint(permRequest.getPolicyId(), owner2.getId()))
        .body(createMaskJson(permRequest.getMask().toString()))
        .post();
    assertThat(r2.getStatusCode()).isEqualTo(OK);

    // Get the owners for the policy previously used
    val r3 = initStringRequest()
        .endpoint(getReadOwnersForPolicyEndpoint(permRequest.getPolicyId()))
        .get();
    assertThat(r3.getStatusCode()).isEqualTo(OK);

    // Assert that response contains both ownerIds, ownerNames and policyId
    val body = MAPPER.readTree(r3.getBody());
    assertThat(body).isNotNull();

    val expectedMap = uniqueIndex(asList(owner1, owner2), Identifiable::getId);

    Streams.stream(body.iterator())
        .forEach(
            n -> {
              val actualOwnerId = UUID.fromString(n.path("id").asText());
              val actualOwnerName = n.path("name").asText();
              val actualMask = AccessLevel.fromValue(n.path("mask").asText());
              assertThat(expectedMap).containsKey(actualOwnerId);
              val expectedOwner = expectedMap.get(actualOwnerId);
              assertThat(actualOwnerName).isEqualTo(expectedOwner.getName());
              assertThat(actualMask).isEqualTo(permRequest.getMask());
            });
  }

  /** Using the owners controller, add a permission with an undefined mask */
  @Test
  public void addPermissionsToOwner_IncorrectMask_BadRequest() {
    // Corrupt the request
    val incorrectMask = "anIncorrectMask";
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> AccessLevel.fromValue(incorrectMask));

    val body = MAPPER.valueToTree(permissionRequests);
    val firstElement = (ObjectNode) body.get(0);
    firstElement.put("mask", incorrectMask);

    val r1 =
        initStringRequest()
            .endpoint(getAddPermissionsEndpoint(owner1.getId()))
            .body(body)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  /** Using the policy controller, add a permission with an undefined mask */
  @Test
  public void addPermissionsToPolicy_IncorrectMask_BadRequest() {
    // Corrupt the request
    val incorrectMask = "anIncorrectMask";
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> AccessLevel.fromValue(incorrectMask));

    // Using the policy controller
    val policyId = permissionRequests.get(0).getPolicyId();
    val r2 = initStringRequest()
        .endpoint(getAddPermissionEndpoint(policyId, owner1.getId()))
        .body(createMaskJson(incorrectMask))
        .post();
    assertThat(r2.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void uuidValidationForOwner_MalformedUUID_BadRequest() {
    val r1 =
        initStringRequest()
            .endpoint(getAddPermissionsEndpoint(INVALID_UUID))
            .body(permissionRequests)
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(BAD_REQUEST);

    val r4 = initStringRequest()
        .endpoint(getReadPermissionsEndpoint(INVALID_UUID))
        .get();
    assertThat(r4.getStatusCode()).isEqualTo(BAD_REQUEST);

    val r5 =
        initStringRequest()
            .endpoint(getDeletePermissionEndpoint(UUID.randomUUID().toString(), INVALID_UUID))
            .delete();
    assertThat(r5.getStatusCode()).isEqualTo(BAD_REQUEST);

    val r6 =
        initStringRequest()
            .endpoint(getDeletePermissionEndpoint(INVALID_UUID, UUID.randomUUID().toString()))
            .delete();
    assertThat(r6.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  @Test
  public void uuidValidationForPolicy_MalformedUUID_BadRequest() {
    val r1 =
        initStringRequest()
            .endpoint(getDeletePermissionEndpoint(UUID.randomUUID().toString(), INVALID_UUID))
            .delete();
    assertThat(r1.getStatusCode()).isEqualTo(BAD_REQUEST);

    val r2 =
        initStringRequest()
            .endpoint(getAddPermissionEndpoint(UUID.randomUUID().toString(), INVALID_UUID))
            .body(createMaskJson(WRITE.toString()))
            .post();
    assertThat(r2.getStatusCode()).isEqualTo(BAD_REQUEST);

    val r3 =
        initStringRequest()
            .endpoint(getAddPermissionEndpoint(INVALID_UUID, UUID.randomUUID().toString()))
            .body(createMaskJson(WRITE.toString()))
            .post();
    assertThat(r3.getStatusCode()).isEqualTo(BAD_REQUEST);

    val r4 =
        initStringRequest()
            .endpoint(getDeletePermissionEndpoint(INVALID_UUID, UUID.randomUUID().toString()))
            .delete();
    assertThat(r4.getStatusCode()).isEqualTo(BAD_REQUEST);
  }

  @Test
  @SneakyThrows
  public void addPermissionsToPolicy_DuplicateRequests_Conflict() {
    val permRequest = permissionRequests.get(0);
    // Create 2 identical requests with same policy and owner
    val r1 =
        initStringRequest()
            .endpoint(getAddPermissionEndpoint(permRequest.getPolicyId(), owner1.getId()))
            .body(createMaskJson(permRequest.getMask().toString()))
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    val r2 =
        initStringRequest()
            .endpoint(getAddPermissionEndpoint(permRequest.getPolicyId(), owner1.getId()))
            .body(createMaskJson(permRequest.getMask().toString()))
            .post();
    assertThat(r2.getStatusCode()).isEqualTo(CONFLICT);
  }

  @Test
  @SneakyThrows
  public void updatePermissionsToOwner_AlreadyExists_Success() {
    val permRequest1 = permissionRequests.get(0);
    val permRequest2 = permissionRequests.get(1);
    val updatedMask = permRequest2.getMask();
    val updatedPermRequest1 =
        PermissionRequest.builder().policyId(permRequest1.getPolicyId()).mask(updatedMask).build();

    assertThat(updatedMask).isNotEqualTo(permRequest1.getMask());
    assertThat(permRequest1.getMask()).isNotEqualTo(permRequest2.getMask());

    // Create permission for owner
    val r1 = initStringRequest()
            .endpoint(getAddPermissionsEndpoint(owner1.getId()))
            .body(ImmutableList.of(permRequest1))
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Get created permissions
    val r2 = initStringRequest()
        .endpoint(getReadPermissionsEndpoint(owner1.getId()))
        .get();
    assertThat(r2.getStatusCode()).isEqualTo(OK);
    assertThat(r2.getBody()).isNotNull();

    // Assert created permission is correct mask
    val page = MAPPER.readTree(r2.getBody());
    val existingPermissions =
        Streams.stream(page.path("resultSet").iterator())
            .map(x -> MAPPER.convertValue(x, getPermissionType()))
            .collect(toImmutableList());
    assertThat(existingPermissions).hasSize(1);
    val permission = existingPermissions.get(0);
    assertThat(permission.getAccessLevel()).isEqualTo(permRequest1.getMask());

    // Update the permission
    val r3 = initStringRequest()
        .endpoint(getAddPermissionsEndpoint(owner1.getId()))
        .body(ImmutableList.of(updatedPermRequest1))
        .post();
    assertThat(r3.getStatusCode()).isEqualTo(OK);

    // Get updated permissions
    val r4 = initStringRequest()
        .endpoint(getReadPermissionsEndpoint(owner1.getId()))
        .get();
    assertThat(r4.getStatusCode()).isEqualTo(OK);
    assertThat(r4.getBody()).isNotNull();

    // Assert updated permission is correct mask
    val page2 = MAPPER.readTree(r4.getBody());
    val existingPermissions2 =
        Streams.stream(page2.path("resultSet").iterator())
            .map(x -> MAPPER.convertValue(x, getPermissionType()))
            .collect(toImmutableList());
    assertThat(existingPermissions2).hasSize(1);
    val permission2 = existingPermissions2.get(0);
    assertThat(permission2.getAccessLevel()).isEqualTo(updatedMask);
  }

  @Test
  @SneakyThrows
  public void updatePermissionsToPolicy_AlreadyExists_Success() {
    val permRequest1 = permissionRequests.get(0);
    val permRequest2 = permissionRequests.get(1);
    assertThat(permRequest1.getMask()).isNotEqualTo(permRequest2.getMask());

    // Create permission for owner and policy
    val r1 = initStringRequest()
            .endpoint(getAddPermissionEndpoint(permRequest1.getPolicyId(), owner1.getId()))
            .body(createMaskJson(permRequest1.getMask().toString()))
            .post();
    assertThat(r1.getStatusCode()).isEqualTo(OK);

    // Get created permissions
    val r2 = initStringRequest()
        .endpoint(getReadPermissionsEndpoint(owner1.getId()))
        .get();
    assertThat(r2.getStatusCode()).isEqualTo(OK);
    assertThat(r2.getBody()).isNotNull();

    // Assert created permission is correct mask
    val page = MAPPER.readTree(r2.getBody());
    val existingPermissions =
        Streams.stream(page.path("resultSet").iterator())
            .map(x -> MAPPER.convertValue(x, getPermissionType()))
            .collect(toImmutableList());
    assertThat(existingPermissions).hasSize(1);
    val permission = existingPermissions.get(0);
    assertThat(permission.getAccessLevel()).isEqualTo(permRequest1.getMask());

    // Update the permission
    val r3 =
        initStringRequest()
            .endpoint(getAddPermissionEndpoint(permRequest1.getPolicyId(), owner1.getId()))
            .body(createMaskJson(permRequest2.getMask().toString()))
            .post();
    assertThat(r3.getStatusCode()).isEqualTo(OK);

    // Get updated permissions
    val r4 = initStringRequest()
        .endpoint(getReadPermissionsEndpoint(owner1.getId()))
        .get();
    assertThat(r4.getStatusCode()).isEqualTo(OK);
    assertThat(r4.getBody()).isNotNull();

    // Assert updated permission is correct mask
    val page2 = MAPPER.readTree(r4.getBody());
    val existingPermissions2 =
        Streams.stream(page2.path("resultSet").iterator())
            .map(x -> MAPPER.convertValue(x, getPermissionType()))
            .collect(toImmutableList());
    assertThat(existingPermissions2).hasSize(1);
    val permission2 = existingPermissions2.get(0);
    assertThat(permission2.getAccessLevel()).isEqualTo(permRequest2.getMask());
  }

  /**
   *  Necessary abstract methods for a generic abstract test
   */

  // Commonly used
  protected abstract EntityGenerator getEntityGenerator();
  protected abstract PolicyService getPolicyService();

  // Owner specific
  protected abstract Class<O> getOwnerType();
  protected abstract O generateOwner(String name);
  protected abstract NamedService<O, UUID> getOwnerService();
  protected abstract String generateNonExistentOwnerName();

  // Permission specific
  protected abstract Class<P> getPermissionType();
  protected abstract AbstractPermissionService<O,P> getPermissionService();

  // Endpoints
  protected abstract String getAddPermissionsEndpoint(String ownerId);
  protected abstract String getAddPermissionEndpoint(String policyId, String ownerId);
  protected abstract String getReadPermissionsEndpoint(String ownerId);
  protected abstract String getDeleteOwnerEndpoint(String ownerId);
  protected abstract String getDeletePermissionsEndpoint(String ownerId, Collection<String> permissionIds);
  protected abstract String getDeletePermissionEndpoint(String policyId, String  ownerId);
  protected abstract String getReadOwnersForPolicyEndpoint(String policyId);

  /**
   * For convenience
   */
  private String getReadOwnersForPolicyEndpoint(UUID policyId){
    return getReadOwnersForPolicyEndpoint(policyId.toString());
  }

  private String getAddPermissionsEndpoint(UUID ownerId){
    return getAddPermissionsEndpoint(ownerId.toString());
  }

  private String getAddPermissionEndpoint(UUID policyId, UUID ownerId){
    return getAddPermissionEndpoint(policyId.toString(), ownerId.toString());
  }

  private String getReadPermissionsEndpoint(UUID ownerId){
    return getReadPermissionsEndpoint(ownerId.toString());
  }

  private String getDeleteOwnerEndpoint(UUID ownerId){
    return getDeleteOwnerEndpoint(ownerId.toString());
  }

  private String getDeletePermissionsEndpoint(UUID ownerId, Collection<UUID> permissionIds){
    return getDeletePermissionsEndpoint(ownerId.toString(), mapToList(permissionIds, UUID::toString));
  }

  private String getDeletePermissionEndpoint(UUID policyId, UUID  ownerId){
    return getDeletePermissionEndpoint(policyId.toString(), ownerId.toString());
  }

  public static ObjectNode createMaskJson(String maskStringValue) {
    return MAPPER.createObjectNode().put("mask", maskStringValue);
  }

}
