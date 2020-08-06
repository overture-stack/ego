package bio.overture.ego.controller;

import static bio.overture.ego.model.enums.AccessLevel.*;
import static com.google.common.collect.Lists.newArrayList;
import static org.junit.Assert.*;
import static org.springframework.http.HttpStatus.OK;

import bio.overture.ego.model.dto.ResolvedPermissionResponse;
import bio.overture.ego.model.entity.AbstractPermission;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.entity.NameableEntity;
import java.util.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;

@Slf4j
public abstract class AbstractResolvablePermissionControllerTest<
        O extends NameableEntity<UUID>, P extends AbstractPermission<O>>
    extends AbstractPermissionControllerTest<O, P> {

  @Test
  @SneakyThrows
  public void resolveOwnerAndGroupPermissions_noPermissionOverlap_Success() {
    // setup group with random name to prevent conflict on add permission request
    val group = getEntityGenerator().setupGroup(UUID.randomUUID().toString());

    // create policies
    val readPolicy = getEntityGenerator().setupSinglePolicy("READ Policy");
    val writePolicy = getEntityGenerator().setupSinglePolicy("WRITE Policy");
    val denyPolicy = getEntityGenerator().setupSinglePolicy("DENY Policy");

    // Add permissions to owner
    val r1 =
        initStringRequest()
            .endpoint(
                getAddPermissionEndpoint(readPolicy.getId().toString(), owner1.getId().toString()))
            .body(createMaskJson(READ.toString()))
            .post();
    val r2 =
        initStringRequest()
            .endpoint(
                getAddPermissionEndpoint(denyPolicy.getId().toString(), owner1.getId().toString()))
            .body(createMaskJson(DENY.toString()))
            .post();

    assertEquals(r1.getStatusCode(), OK);
    assertEquals(r2.getStatusCode(), OK);

    // Add permission to group
    val g1 = addGroupPermissionToGroupPostRequestAnd(group, writePolicy, WRITE).assertOk();

    // Add owner to group
    Collection<String> ownerIds = new ArrayList<String>();
    ownerIds.add(owner1.getId().toString());
    val addToGroupRequest =
        initStringRequest()
            .endpoint(getAddOwnerToGroupEndpoint(group.getId().toString()))
            .body(ownerIds)
            .post();

    assertEquals(addToGroupRequest.getStatusCode(), OK);
    // assert user is in group
    val groupUsers =
        initStringRequest().endpoint(getAddOwnerToGroupEndpoint(group.getId().toString())).get();
    assertNotNull(groupUsers.getBody());
    assertTrue(groupUsers.getBody().contains(owner1.getId().toString()));

    // assert group has expected permission
    getGroupPermissionsForGroupGetRequestAnd(group)
        .assertPageResultHasSize(GroupPermission.class, 1);

    List<String> policies =
        Arrays.asList(denyPolicy.getName(), readPolicy.getName(), writePolicy.getName());

    val resolvedPerms =
        initStringRequest()
            .endpoint(getOwnerAndGroupPermissionsForOwnerEndpoint(owner1.getId().toString()));
    resolvedPerms.getAnd().assertPageResultsOfType(ResolvedPermissionResponse.class);
    val responseBody = resolvedPerms.get().getBody();
    assertNotNull(responseBody);
    val responseJson = MAPPER.readTree(responseBody);
    assertEquals(responseJson.size(), 3);

    assertTrue(responseBody.contains(policies.get(0)));
    assertTrue(responseBody.contains(policies.get(1)));
    assertTrue(responseBody.contains(policies.get(2)));
  }

  @Test
  @SneakyThrows
  public void resolveOwnerAndGroupPermissions_hasPermissionOverlap_Success() {
    // setup groups with random name to prevent conflict on add permission request
    val group1 = getEntityGenerator().setupGroup(UUID.randomUUID().toString());
    val group2 = getEntityGenerator().setupGroup(UUID.randomUUID().toString());
    val group3 = getEntityGenerator().setupGroup(UUID.randomUUID().toString());

    // create policies
    val policy1 = getEntityGenerator().setupSinglePolicy("Policy 1");
    val policy2 = getEntityGenerator().setupSinglePolicy("Policy 2");
    val policy3 = getEntityGenerator().setupSinglePolicy("Policy 3");

    val ownerId1 = owner1.getId().toString();
    val ownerId2 = owner2.getId().toString();
    val ownerId3 = owner3.getId().toString();

    // Add permissions to owners
    val r1 =
        initStringRequest()
            .endpoint(getAddPermissionEndpoint(policy1.getId().toString(), ownerId1))
            .body(createMaskJson(READ.toString()))
            .post();

    val r2 =
        initStringRequest()
            .endpoint(getAddPermissionEndpoint(policy2.getId().toString(), ownerId2))
            .body(createMaskJson(WRITE.toString()))
            .post();

    val r3 =
        initStringRequest()
            .endpoint(getAddPermissionEndpoint(policy3.getId().toString(), ownerId3))
            .body(createMaskJson(READ.toString()))
            .post();

    assertEquals(r1.getStatusCode(), OK);

    // Add permission to group
    val g1 = addGroupPermissionToGroupPostRequestAnd(group1, policy1, DENY).assertOk();
    val g2 = addGroupPermissionToGroupPostRequestAnd(group2, policy2, READ).assertOk();
    val g3 = addGroupPermissionToGroupPostRequestAnd(group3, policy3, READ).assertOk();

    // Add owners to groups
    List<String> owner1Body = newArrayList(ownerId1);
    List<String> owner2Body = newArrayList(ownerId2);
    List<String> owner3Body = newArrayList(ownerId3);

    val addToGroup1Request =
        initStringRequest()
            .endpoint(getAddOwnerToGroupEndpoint(group1.getId().toString()))
            .body(owner1Body)
            .post();
    val addToGroup2Request =
        initStringRequest()
            .endpoint(getAddOwnerToGroupEndpoint(group2.getId().toString()))
            .body(owner2Body)
            .post();
    val addToGroup3Request =
        initStringRequest()
            .endpoint(getAddOwnerToGroupEndpoint(group3.getId().toString()))
            .body(owner3Body)
            .post();

    assertEquals(addToGroup1Request.getStatusCode(), OK);
    assertEquals(addToGroup2Request.getStatusCode(), OK);
    assertEquals(addToGroup3Request.getStatusCode(), OK);

    // assert user is in all groups
    val group1Users =
        initStringRequest().endpoint(getAddOwnerToGroupEndpoint(group1.getId().toString())).get();
    assertNotNull(group1Users.getBody());
    assertTrue(group1Users.getBody().contains(ownerId1));
    val group2Users =
        initStringRequest().endpoint(getAddOwnerToGroupEndpoint(group2.getId().toString())).get();
    assertNotNull(group2Users.getBody());
    assertTrue(group2Users.getBody().contains(ownerId2));
    val group3Users =
        initStringRequest().endpoint(getAddOwnerToGroupEndpoint(group3.getId().toString())).get();
    assertNotNull(group3Users.getBody());
    assertTrue(group3Users.getBody().contains(ownerId3));

    // assert group has expected permission
    getGroupPermissionsForGroupGetRequestAnd(group1)
        .assertPageResultHasSize(GroupPermission.class, 1);
    getGroupPermissionsForGroupGetRequestAnd(group2)
        .assertPageResultHasSize(GroupPermission.class, 1);
    getGroupPermissionsForGroupGetRequestAnd(group3)
        .assertPageResultHasSize(GroupPermission.class, 1);

    // test final perms for owner1 + group1
    val resolvedPerms1 =
        initStringRequest().endpoint(getOwnerAndGroupPermissionsForOwnerEndpoint(ownerId1));
    resolvedPerms1.getAnd().assertPageResultsOfType(ResolvedPermissionResponse.class);
    val responseBody1 = resolvedPerms1.get().getBody();
    assertNotNull(responseBody1);
    val responseJson1 = MAPPER.readTree(responseBody1);
    assertEquals(responseJson1.size(), 1);

    val finalAcl1 = responseJson1.get(0).get("accessLevel").asText();
    val finalPerm1 = responseJson1.get(0).path("policy").path("id").asText();

    assertEquals(DENY.toString(), finalAcl1);
    assertEquals(policy1.getId().toString(), finalPerm1);

    // test final perms for owner2 + group2
    val resolvedPerms2 =
        initStringRequest().endpoint(getOwnerAndGroupPermissionsForOwnerEndpoint(ownerId2));
    resolvedPerms2.getAnd().assertPageResultsOfType(ResolvedPermissionResponse.class);
    val responseBody2 = resolvedPerms2.get().getBody();
    assertNotNull(responseBody2);
    val responseJson2 = MAPPER.readTree(responseBody2);
    assertEquals(responseJson2.size(), 1);

    val finalAcl2 = responseJson2.get(0).get("accessLevel").asText();
    val finalPerm2 = responseJson2.get(0).path("policy").path("id").asText();

    assertEquals(WRITE.toString(), finalAcl2);
    assertEquals(policy2.getId().toString(), finalPerm2);

    // test final perms for owner3 + group3
    val resolvedPerms3 =
        initStringRequest().endpoint(getOwnerAndGroupPermissionsForOwnerEndpoint(ownerId3));
    resolvedPerms3.getAnd().assertPageResultsOfType(ResolvedPermissionResponse.class);
    val responseBody3 = resolvedPerms3.get().getBody();
    assertNotNull(responseBody3);
    val responseJson3 = MAPPER.readTree(responseBody3);
    assertEquals(responseJson3.size(), 1);

    val finalAcl3 = responseJson3.get(0).get("accessLevel").asText();
    val finalPerm3 = responseJson3.get(0).path("policy").path("id").asText();

    assertEquals(READ.toString(), finalAcl3);
    assertEquals(policy3.getId().toString(), finalPerm3);
  }
}
