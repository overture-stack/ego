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

  @Test
  public void testGetPermissionsGroups() {
    entityGenerator.setupSimpleUsers();
    entityGenerator.setupSimpleGroups();
    val groups = groupService
        .listGroups(Collections.emptyList(), new PageableResolver().getPageable())
        .getContent();
    entityGenerator.setupSimpleAclEntities(groups);

    val user = userService.getByName("FirstUser@domain.com");
    val study001 = aclEntityService.getByName("Study001");
    val study002 = aclEntityService.getByName("Study002");
    val study003 = aclEntityService.getByName("Study003");

    val permissions = Arrays.asList(
        Pair.of(study001, AclMask.READ),
        Pair.of(study002, AclMask.WRITE),
        Pair.of(study003, AclMask.DENY)
    );

    userService.addUserPermissions(Integer.toString(user.getId()), permissions);

    assertThat(user.getPermissions()).containsExactlyInAnyOrder(
        "Study001.deny"
    );
  }

}
