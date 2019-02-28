package bio.overture.ego.service;

import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.dto.PolicyResponse;
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

import static bio.overture.ego.model.enums.AccessLevel.READ;
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

    val permissions = asList(new PermissionRequest(policy.getId(), READ));
    groupPermissionService.addGroupPermissions(group1.getId(), permissions);
    groupPermissionService.addGroupPermissions(group2.getId(), permissions);

    val expected =
        asList(
            new PolicyResponse(group1.getId().toString(), name1, READ),
            new PolicyResponse(group2.getId().toString(), name2, READ));

    val actual = groupPermissionService.findByPolicy(policy.getId());

    assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
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

    val permissions = asList(new PermissionRequest(policy.getId(), READ));
    userService.addUserPermissions(user1.getId(), permissions);
    userService.addUserPermissions(user2.getId(), permissions);

    val expected =
        asList(
            new PolicyResponse(user1.getId().toString(), name1, READ),
            new PolicyResponse(user2.getId().toString(), name2, READ));

    val actual = userPermissionService.findByPolicy(policy.getId());
    ;
    System.out.printf("%s", actual.get(0).toString());
    assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
  }
}
