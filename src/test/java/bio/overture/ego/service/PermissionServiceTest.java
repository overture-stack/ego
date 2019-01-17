package bio.overture.ego.service;

import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.params.PolicyIdStringWithAccessLevel;
import bio.overture.ego.utils.EntityGenerator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class PermissionServiceTest {
  @Autowired private UserService userService;

  @Autowired private GroupService groupService;

  @Autowired private PolicyService policyService;

  @Autowired private UserPermissionService userPermissionService;

  @Autowired private GroupPermissionService groupPermissionService;

  @Autowired private EntityGenerator entityGenerator;

  @Test
  public void testFindGroupIdsByPolicy() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestPolicies();

    val policy = policyService.getByName("Study001");

    val name1 = "Group One";
    val name2 = "Group Three";

    val group1 = groupService.getByName(name1);
    val group2 = groupService.getByName(name2);

    val permissions = asList(new PolicyIdStringWithAccessLevel(policy.getId().toString(), "READ"));
    groupService.addGroupPermissions(group1.getId().toString(), permissions);
    groupService.addGroupPermissions(group2.getId().toString(), permissions);

    val expected =
        asList(
            new PolicyResponse(group1.getId().toString(), name1, AccessLevel.READ),
            new PolicyResponse(group2.getId().toString(), name2, AccessLevel.READ));

    val actual = groupPermissionService.findByPolicy(policy.getId().toString());

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testFindUserIdsByPolicy() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestPolicies();

    val policy = policyService.getByName("Study001");
    val name1 = "FirstUser@domain.com";
    val name2 = "SecondUser@domain.com";
    val user1 = userService.getByName(name1);
    val user2 = userService.getByName(name2);

    val permissions = asList(new PolicyIdStringWithAccessLevel(policy.getId().toString(), "READ"));
    userService.addUserPermissions(user1.getId().toString(), permissions);
    userService.addUserPermissions(user2.getId().toString(), permissions);

    val expected =
        asList(
            new PolicyResponse(user1.getId().toString(), name1, AccessLevel.READ),
            new PolicyResponse(user2.getId().toString(), name2, AccessLevel.READ));

    val actual = userPermissionService.findByPolicy(policy.getId().toString());
    ;
    System.out.printf("%s", actual.get(0).toString());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void createGroupPerm_NotExisting_Success(){
    // Setup dependencies
    val group = entityGenerator.setupGroup("RoblexGroup");
    val policy = entityGenerator.setupPolicy("myPol", group.getName());

    // Create Request1
    val request1 = new GroupPermission();
    request1.setAccessLevel(AccessLevel.WRITE);
    request1.setOwner(group);
    request1.setPolicy(policy);

    // Assert that Request1 created a new GroupPermission object successfully by reading it back
    val expected1 = groupPermissionService.create(request1);
    val actual1 = groupPermissionService.getById(expected1.getId());
    assertThat(actual1).isEqualTo(expected1);
  }

  @Test
  public void createUserPerm_NotExisting_Success(){
    // Setup dependencies
    val user = entityGenerator.setupUser("Roblex Lepisma");
    val group = entityGenerator.setupGroup("AwesomeGroup");
    val policy = entityGenerator.setupPolicy("myPol", group.getName());

    // Create Request1
    val request1 = new UserPermission();
    request1.setAccessLevel(AccessLevel.WRITE);
    request1.setOwner(user);
    request1.setPolicy(policy);

    // Assert that Request1 created a new UserPermission object successfully by reading it back
    val expected1 = userPermissionService.create(request1);
    val actual1 = userPermissionService.getById(expected1.getId());
    assertThat(actual1).isEqualTo(expected1);
  }

}
