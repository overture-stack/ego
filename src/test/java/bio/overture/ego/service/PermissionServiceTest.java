package bio.overture.ego.service;

import bio.overture.ego.controller.resolver.PageableResolver;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.params.PolicyIdStringWithAccessLevel;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.queryspecification.GroupPermissionSpecification;
import bio.overture.ego.token.IDToken;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.PolicyPermissionUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class PermissionServiceTest {
  @Autowired
  private UserService userService;

  @Autowired
  private GroupService groupService;

  @Autowired
  private PolicyService policyService;

  @Autowired
  private UserPermissionService userPermissionService;

  @Autowired
  private GroupPermissionService groupPermissionService;

  @Autowired
  private EntityGenerator entityGenerator;

  @Test
  public void testFindGroupIdsByPolicy() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestPolicies();

    val policy = policyService.getByName("Study001");
    val group1 = groupService.getByName("Group One");
    val group2 = groupService.getByName("Group Three");

    val permissions = asList(new PolicyIdStringWithAccessLevel(policy.getId().toString(), "READ"));
    groupService.addGroupPermissions(group1.getId().toString(), permissions);
    groupService.addGroupPermissions(group2.getId().toString(), permissions);

    val expected = asList(group1.getId(), group2.getId());

    val actual = groupPermissionService.findGroupIdsByPolicy(policy.getId().toString());

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testFindUserIdsByPolicy() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestGroups();
    entityGenerator.setupTestPolicies();

    val policy = policyService.getByName("Study001");
    val user1 = userService.getByName("FirstUser@domain.com");
    val user2 = userService.getByName("SecondUser@domain.com");

    val permissions = asList(new PolicyIdStringWithAccessLevel(policy.getId().toString(), "READ"));
    userService.addUserPermissions(user1.getId().toString(), permissions);
    userService.addUserPermissions(user2.getId().toString(), permissions);

    val expected = asList(user1.getId(), user2.getId());

    val actual = userPermissionService.findUserIdsByPolicy(policy.getId().toString());

    assertThat(actual).isEqualTo(expected);
  }

}


