package bio.overture.ego.service;

import bio.overture.ego.controller.resolver.PageableResolver;
import bio.overture.ego.model.dto.PolicyRequest;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.model.exceptions.UniqueViolationException;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.utils.EntityGenerator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class PolicyServiceTest {

  @Autowired private PolicyService policyService;

  @Autowired private EntityGenerator entityGenerator;

  private List<Group> groups;

  @Before
  public void setUp() {
    groups = entityGenerator.setupGroups("Group One", "GroupTwo", "Group Three");
  }

  // Create
  @Test
  public void testCreate() {
    val policy = entityGenerator.setupPolicy("Study001,Group One");
    assertThat(policy.getName()).isEqualTo("Study001");
  }

  @Test
  @Ignore
  public void testCreateUniqueName() {
    //    policyService.create(entityGenerator.createPolicy(Pair.of("Study001",
    // groups.get(0).getId())));
    //    policyService.create(entityGenerator.createPolicy(Pair.of("Study002",
    // groups.get(0).getId())));
    //    assertThatExceptionOfType(DataIntegrityViolationException.class)
    //        .isThrownBy(() ->
    // policyService.create(entityGenerator.createPolicy(Pair.of("Study001",
    // groups.get(0).getId()))));
    assertThat(1).isEqualTo(2);
    // TODO Check for uniqueness in application, currently only SQL
  }

  // Read
  @Test
  public void testGet() {
    val policy = entityGenerator.setupPolicy("Study001", groups.get(0).getName());
    val savedPolicy = policyService.get(policy.getId().toString());
    assertThat(savedPolicy.getName()).isEqualTo("Study001");
  }

  @Test
  public void testGetNotFoundException() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> policyService.get(UUID.randomUUID().toString()));
  }

  @Test
  public void testGetByName() {
    entityGenerator.setupPolicy("Study001", groups.get(0).getName());
    val savedUser = policyService.getByName("Study001");
    assertThat(savedUser.getName()).isEqualTo("Study001");
  }

  @Test
  public void testGetByNameAllCaps() {
    entityGenerator.setupPolicy("Study001", groups.get(0).getName());
    val savedUser = policyService.getByName("STUDY001");
    assertThat(savedUser.getName()).isEqualTo("Study001");
  }

  @Test
  @Ignore
  public void testGetByNameNotFound() {
    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> policyService.getByName("Study000"));
  }

  @Test
  public void testListUsersNoFilters() {
    entityGenerator.setupTestPolicies();
    val aclEntities =
        policyService.listPolicies(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(aclEntities.getTotalElements()).isEqualTo(3L);
  }

  @Test
  public void testListUsersNoFiltersEmptyResult() {
    val aclEntities =
        policyService.listPolicies(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(aclEntities.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testListUsersFiltered() {
    entityGenerator.setupTestPolicies();
    val userFilter = new SearchFilter("name", "Study001");
    val aclEntities =
        policyService.listPolicies(Arrays.asList(userFilter), new PageableResolver().getPageable());
    assertThat(aclEntities.getTotalElements()).isEqualTo(1L);
  }

  @Test
  public void uniqueNameCheck_CreatePolicy_ThrowsUniqueConstraintException(){
    val r1 = PolicyRequest.builder()
        .name(UUID.randomUUID().toString())
        .build();

    val p1 = policyService.create(r1);
    assertThat(policyService.isExist(p1.getId())).isTrue();

    assertThat(p1.getName()).isEqualTo(r1.getName());
    assertThatExceptionOfType(UniqueViolationException.class)
        .isThrownBy(() -> policyService.create(r1));
  }

  @Test
  public void uniqueNameCheck_UpdatePolicy_ThrowsUniqueConstraintException(){
    val name1 = UUID.randomUUID().toString();
    val name2 = UUID.randomUUID().toString();
    val cr1 = PolicyRequest.builder()
        .name(name1)
        .build();

    val cr2 = PolicyRequest.builder()
        .name(name2)
        .build();

    val p1 = policyService.create(cr1);
    assertThat(policyService.isExist(p1.getId())).isTrue();
    val p2 = policyService.create(cr2);
    assertThat(policyService.isExist(p2.getId())).isTrue();

    val ur3 = PolicyRequest.builder()
        .name(name1)
        .build();

    assertThat(p1.getName()).isEqualTo(ur3.getName());
    assertThat(p2.getName()).isNotEqualTo(ur3.getName());
    assertThatExceptionOfType(UniqueViolationException.class)
        .isThrownBy(() -> policyService.partialUpdate(p2.getId().toString(), ur3));
  }

  @Test
  public void testListUsersFilteredEmptyResult() {
    entityGenerator.setupTestPolicies();
    val userFilter = new SearchFilter("name", "Study004");
    val aclEntities =
        policyService.listPolicies(Arrays.asList(userFilter), new PageableResolver().getPageable());
    assertThat(aclEntities.getTotalElements()).isEqualTo(0L);
  }

  // Update
  @Test
  public void testUpdate() {
    val policy = entityGenerator.setupPolicy("Study001", groups.get(0).getName());
    val updateRequest = PolicyRequest.builder()
        .name("StudyOne")
        .build();
    val updated = policyService.partialUpdate(policy.getId().toString(), updateRequest);
    assertThat(updated.getName()).isEqualTo("StudyOne");
  }

  // Delete
  @Test
  public void testDelete() {
    entityGenerator.setupTestPolicies();
    val policy = policyService.getByName("Study001");
    policyService.delete(policy.getId().toString());

    val remainingAclEntities =
        policyService.listPolicies(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(remainingAclEntities.getTotalElements()).isEqualTo(2L);
    assertThat(remainingAclEntities.getContent()).doesNotContain(policy);
  }
}
