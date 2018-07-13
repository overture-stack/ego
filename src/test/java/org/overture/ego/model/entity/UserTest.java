package org.overture.ego.model.entity;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.overture.ego.controller.resolver.PageableResolver;
import org.overture.ego.model.enums.AclMask;
import org.overture.ego.service.AclEntityService;
import org.overture.ego.service.GroupService;
import org.overture.ego.service.UserService;
import org.overture.ego.utils.EntityGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class UserTest {
  @Autowired
  private UserService userService;

  @Autowired
  private GroupService groupService;

  @Autowired
  private AclEntityService aclEntityService;

  @Autowired
  private EntityGenerator entityGenerator;

  @Test
  public void testGetPermissionsNoPermissions() {
    entityGenerator.setupSimpleUsers();
    entityGenerator.setupSimpleGroups();
    val groups = groupService
        .listGroups(Collections.emptyList(), new PageableResolver().getPageable())
        .getContent();
    entityGenerator.setupSimpleAclEntities(groups);

    val user = userService.getByName("FirstUser@domain.com");

    assertThat(user.getPermissions().size()).isEqualTo(0);
  }

  @Test
  public void testGetPermissionsNoGroups() {
    entityGenerator.setupSimpleUsers();
    entityGenerator.setupSimpleGroups();
    val groups = groupService
        .listGroups(Collections.emptyList(), new PageableResolver().getPageable())
        .getContent();
    entityGenerator.setupSimpleAclEntities(groups);

    val user = userService.getByName("FirstUser@domain.com");
    val study001 = aclEntityService.getByName("Study001");

    val permissions = Arrays.asList(
        Pair.of(study001, AclMask.READ),
        Pair.of(study001, AclMask.WRITE),
        Pair.of(study001, AclMask.DENY)
    );

    userService.addUserPermissions(Integer.toString(user.getId()), permissions);

    assertThat(user.getPermissions()).containsExactlyInAnyOrder(
        "Study001.deny"
    );
  }

  /**
   * This is the acl permission -> JWT output uber test,
   * if this passes we can be assured that we are correctly
   * coalescing permissions from the individual user and their
   * groups, squashing on aclEntity while prioritizing the
   * aclMask order of (DENY -> WRITE -> READ)
   * <p>
   * Original github issue with manual SQL:
   * https://github.com/overture-stack/ego/issues/105
   */
  @Test
  public void testGetPermissionsUberTest() {
    entityGenerator.setupSimpleUsers();
    entityGenerator.setupSimpleGroups();
    val groups = groupService
        .listGroups(Collections.emptyList(), new PageableResolver().getPageable())
        .getContent();
    entityGenerator.setupSimpleAclEntities(groups);

    // Get Users and Groups
    val alex = userService.getByName("FirstUser@domain.com");
    val alexId = Integer.toString(alex.getId());

    val bob = userService.getByName("SecondUser@domain.com");
    val bobId = Integer.toString(bob.getId());

    val marry = userService.getByName("ThirdUser@domain.com");
    val marryId = Integer.toString(marry.getId());

    val wizards = groups.get(0);
    val wizardsId = Integer.toString(wizards.getId());

    val robots = groups.get(1);
    val robotsId = Integer.toString(robots.getId());

    // Add user's to their respective groups
    userService.addUserToGroups(alexId, Arrays.asList(wizardsId, robotsId));
    userService.addUserToGroups(bobId, Arrays.asList(robotsId));
    userService.addUserToGroups(marryId, Arrays.asList(robotsId));

    // Get the studies so we can
    val study001 = aclEntityService.getByName("Study001");
    val study002 = aclEntityService.getByName("Study002");
    val study003 = aclEntityService.getByName("Study003");

    // Assign ACL Permissions for each user/group
    userService.addUserPermissions(alexId, Arrays.asList(
        Pair.of(study001, AclMask.WRITE),
        Pair.of(study002, AclMask.READ),
        Pair.of(study003, AclMask.DENY)
    ));

    userService.addUserPermissions(bobId, Arrays.asList(
        Pair.of(study001, AclMask.READ),
        Pair.of(study002, AclMask.DENY),
        Pair.of(study003, AclMask.WRITE)
    ));

    userService.addUserPermissions(marryId, Arrays.asList(
        Pair.of(study001, AclMask.DENY),
        Pair.of(study002, AclMask.WRITE),
        Pair.of(study003, AclMask.READ)
    ));

    groupService.addGroupPermissions(wizardsId, Arrays.asList(
        Pair.of(study001, AclMask.WRITE),
        Pair.of(study002, AclMask.READ),
        Pair.of(study003, AclMask.DENY)
    ));

    groupService.addGroupPermissions(robotsId, Arrays.asList(
        Pair.of(study001, AclMask.DENY),
        Pair.of(study002, AclMask.WRITE),
        Pair.of(study003, AclMask.READ)
    ));

    /**
     * Expected Result Computations
     * Alex (Wizards and Robots)
     *  - Study001 (WRITE/WRITE/DENY) == DENY
     *  - Study002 (READ/READ/WRITE) == WRITE
     *  - Study003 (DENY/DENY/READ) == DENY
     * Bob (Robots)
     *  - Study001 (READ/DENY) == DENY
     *  - Study002 (DENY/WRITE) == DENY
     *  - Study003 (WRITE/READ) == WRITE
     * Marry (Robots)
     *  - Study001 (DENY/DENY) == DENY
     *  - Study002 (WRITE/WRITE) == WRITE
     *  - Study003 (READ/READ) == READ
     *
     *  Test Matrix | Group R | Group W | Group D
     *  -----------------------------------------
     *  User R      |  Marry  |  Alex   | Bob
     *  User W      |  Bob    |  Marry  | Alex
     *  User D      |  Alex   |  Bob    | Marry
     *
     */

    // Test that all is well
    assertThat(alex.getPermissions()).containsExactlyInAnyOrder(
        "Study001.deny",
        "Study002.write",
        "Study003.deny"
    );

    assertThat(bob.getPermissions()).containsExactlyInAnyOrder(
        "Study001.deny",
        "Study002.deny",
        "Study003.write"
    );

    assertThat(marry.getPermissions()).containsExactlyInAnyOrder(
        "Study001.deny",
        "Study002.write",
        "Study003.read"
    );
  }

}
