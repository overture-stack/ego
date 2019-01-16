package bio.overture.ego.model.entity;

import static bio.overture.ego.service.UserService.extractScopes;
import static org.assertj.core.api.Assertions.assertThat;

import bio.overture.ego.controller.resolver.PageableResolver;
import bio.overture.ego.model.params.PolicyIdStringWithAccessLevel;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.PolicyService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import java.util.Arrays;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class UserTest {
  @Autowired private UserService userService;

  @Autowired private GroupService groupService;

  @Autowired private PolicyService policyService;

  @Autowired private EntityGenerator entityGenerator;

  @Test
  public void testGetPermissionsNoPermissions() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestPolicies();

    val user = userService.getByName("FirstUser@domain.com");

    assertThat(user.getPermissions().size()).isEqualTo(0);
  }

  @Test
  public void testGetPermissionsNoGroups() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestPolicies();

    val user = userService.getByName("FirstUser@domain.com");
    val study001id = policyService.getByName("Study001").getId().toString();

    val permissions =
        Arrays.asList(
            new PolicyIdStringWithAccessLevel(study001id, "WRITE"),
            new PolicyIdStringWithAccessLevel(study001id, "READ"),
            new PolicyIdStringWithAccessLevel(study001id, "DENY"));

    userService.addUserPermissions(user.getId().toString(), permissions);

    assertThat(user.getPermissions()).containsExactlyInAnyOrder("Study001.DENY");
  }

  private void setupUsers() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();
    val groups =
        groupService
            .listGroups(Collections.emptyList(), new PageableResolver().getPageable())
            .getContent();
    entityGenerator.setupTestPolicies();

    // Get Users and Groups
    val alex = userService.getByName("FirstUser@domain.com");
    val alexId = alex.getId().toString();

    val bob = userService.getByName("SecondUser@domain.com");
    val bobId = bob.getId().toString();

    val marry = userService.getByName("ThirdUser@domain.com");
    val marryId = marry.getId().toString();

    val wizards = groups.get(0);
    val wizardsId = wizards.getId().toString();

    val robots = groups.get(1);
    val robotsId = robots.getId().toString();

    // Add user's to their respective groups
    userService.addUserToGroups(alexId, Arrays.asList(wizardsId, robotsId));
    userService.addUserToGroups(bobId, Arrays.asList(robotsId));
    userService.addUserToGroups(marryId, Arrays.asList(robotsId));

    // Get the studies so we can
    val study001 = policyService.getByName("Study001");
    val study001id = study001.getId().toString();

    val study002 = policyService.getByName("Study002");
    val study002id = study002.getId().toString();

    val study003 = policyService.getByName("Study003");
    val study003id = study003.getId().toString();

    // Assign ACL Permissions for each user/group
    userService.addUserPermissions(
        alexId,
        Arrays.asList(
            new PolicyIdStringWithAccessLevel(study001id, "WRITE"),
            new PolicyIdStringWithAccessLevel(study002id, "READ"),
            new PolicyIdStringWithAccessLevel(study003id, "DENY")));

    userService.addUserPermissions(
        bobId,
        Arrays.asList(
            new PolicyIdStringWithAccessLevel(study001id, "READ"),
            new PolicyIdStringWithAccessLevel(study002id, "DENY"),
            new PolicyIdStringWithAccessLevel(study003id, "WRITE")));

    userService.addUserPermissions(
        marryId,
        Arrays.asList(
            new PolicyIdStringWithAccessLevel(study001id, "DENY"),
            new PolicyIdStringWithAccessLevel(study002id, "WRITE"),
            new PolicyIdStringWithAccessLevel(study003id, "READ")));

    groupService.addGroupPermissions(
        wizardsId,
        Arrays.asList(
            new PolicyIdStringWithAccessLevel(study001id, "WRITE"),
            new PolicyIdStringWithAccessLevel(study002id, "READ"),
            new PolicyIdStringWithAccessLevel(study003id, "DENY")));

    groupService.addGroupPermissions(
        robotsId,
        Arrays.asList(
            new PolicyIdStringWithAccessLevel(study001id, "DENY"),
            new PolicyIdStringWithAccessLevel(study002id, "WRITE"),
            new PolicyIdStringWithAccessLevel(study003id, "READ")));
  }

  /**
   * This is the acl permission -> JWT output uber test, if this passes we can be assured that we
   * are correctly coalescing permissions from the individual user and their groups, squashing on
   * aclEntity while prioritizing the aclMask order of (DENY -> WRITE -> READ)
   *
   * <p>Original github issue with manual SQL: https://github.com/overture-stack/ego/issues/105
   */
  @Test
  public void testGetPermissionsUberTest() {
    setupUsers();
    // Get Users and Groups
    val alex = userService.getByName("FirstUser@domain.com");
    val bob = userService.getByName("SecondUser@domain.com");
    val marry = userService.getByName("ThirdUser@domain.com");

    /**
     * Expected Result Computations Alex (Wizards and Robots) - Study001 (WRITE/WRITE/DENY) == DENY
     * - Study002 (READ/READ/WRITE) == WRITE - Study003 (DENY/DENY/READ) == DENY Bob (Robots) -
     * Study001 (READ/DENY) == DENY - Study002 (DENY/WRITE) == DENY - Study003 (WRITE/READ) == WRITE
     * Marry (Robots) - Study001 (DENY/DENY) == DENY - Study002 (WRITE/WRITE) == WRITE - Study003
     * (READ/READ) == READ
     *
     * <p>Test Matrix | Group R | Group W | Group D ----------------------------------------- User R
     * | Marry | Alex | Bob User W | Bob | Marry | Alex User D | Alex | Bob | Marry
     */

    // Test that all is well
    assertThat(alex.getPermissions())
        .containsExactlyInAnyOrder("Study001.DENY", "Study002.WRITE", "Study003.DENY");

    assertThat(bob.getPermissions())
        .containsExactlyInAnyOrder("Study001.DENY", "Study002.DENY", "Study003.WRITE");

    assertThat(marry.getPermissions())
        .containsExactlyInAnyOrder("Study001.DENY", "Study002.WRITE", "Study003.READ");
  }

  @Test
  public void testGetScopes() {
    setupUsers();
    val alex = userService.getByName("FirstUser@domain.com");
    assertThat(alex).isNotNull();

    val s = extractScopes(alex);
    assertThat(s).isNotNull();

    val expected = entityGenerator.getScopes("Study001.DENY", "Study002.WRITE", "STUDY003.DENY");
    assertThat(s).isEqualTo(expected);
  }
}
