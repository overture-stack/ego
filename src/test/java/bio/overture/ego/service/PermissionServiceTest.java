package bio.overture.ego.service;

import bio.overture.ego.controller.resolver.PageableResolver;
import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.AccessLevel;
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
import static org.assertj.core.api.Assertions.setMaxElementsForPrinting;

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

    val name1 = "Group One";
    val name2 = "Group Three";

    val group1 = groupService.getByName(name1);
    val group2 = groupService.getByName(name2);

    val permissions = asList(new PolicyIdStringWithAccessLevel(policy.getId().toString(), "READ"));
    groupService.addGroupPermissions(group1.getId().toString(), permissions);
    groupService.addGroupPermissions(group2.getId().toString(), permissions);

    val expected = asList(
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

    val expected = asList(
      new PolicyResponse(user1.getId().toString(), name1, AccessLevel.READ),
      new PolicyResponse(user2.getId().toString(), name2, AccessLevel.READ));

    val actual = userPermissionService.findByPolicy(policy.getId().toString());;
    System.out.printf("%s",actual.get(0).toString());
    assertThat(actual).isEqualTo(expected);
  }

}


