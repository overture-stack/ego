package org.overture.ego.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.overture.ego.controller.resolver.PageableResolver;
import org.overture.ego.model.entity.Group;
import org.overture.ego.model.search.SearchFilter;
import org.overture.ego.utils.EntityGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
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

  @Autowired
  private AclEntityService aclEntityService;

  @Autowired
  private GroupService groupService;

  @Autowired
  private EntityGenerator entityGenerator;

  private List<Group> groups;

  @Before
  public void setUp() {
    // We need groups to be owners of aclEntities
    entityGenerator.setupSimpleGroups();
    groups = groupService
        .listGroups(Collections.emptyList(), new PageableResolver().getPageable())
        .getContent();
  }

  // Create
  @Test
  public void testCreate() {
    val policy = aclEntityService
        .create(entityGenerator.createOneAclEntity(Pair.of("Study001", groups.get(0).getId())));
    assertThat(policy.getName()).isEqualTo("Study001");
  }

  @Test
  @Ignore
  public void testCreateUniqueName() {
//    aclEntityService.create(entityGenerator.createOneAclEntity(Pair.of("Study001", groups.get(0).getId())));
//    aclEntityService.create(entityGenerator.createOneAclEntity(Pair.of("Study002", groups.get(0).getId())));
//    assertThatExceptionOfType(DataIntegrityViolationException.class)
//        .isThrownBy(() -> aclEntityService.create(entityGenerator.createOneAclEntity(Pair.of("Study001", groups.get(0).getId()))));
    assertThat(1).isEqualTo(2);
    // TODO Check for uniqueness in application, currently only SQL
  }

  // Read
  @Test
  public void testGet() {
    val policy = aclEntityService.create(entityGenerator.createOneAclEntity(Pair.of("Study001", groups.get(0).getId())));
    val savedPolicy = aclEntityService.get(policy.getId().toString());
    assertThat(savedPolicy.getName()).isEqualTo("Study001");
  }

  @Test
  public void testGetEntityNotFoundException() {
    assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> aclEntityService.get(UUID.randomUUID().toString()));
  }

  @Test
  public void testGetByName() {
    aclEntityService.create(entityGenerator.createOneAclEntity(Pair.of("Study001", groups.get(0).getId())));
    val savedUser = aclEntityService.getByName("Study001");
    assertThat(savedUser.getName()).isEqualTo("Study001");
  }

  @Test
  public void testGetByNameAllCaps() {
    aclEntityService.create(entityGenerator.createOneAclEntity(Pair.of("Study001", groups.get(0).getId())));
    val savedUser = aclEntityService.getByName("STUDY001");
    assertThat(savedUser.getName()).isEqualTo("Study001");
  }

  @Test
  @Ignore
  public void testGetByNameNotFound() {
    // TODO Currently returning null, should throw exception (EntityNotFoundException?)
    assertThatExceptionOfType(EntityNotFoundException.class)
        .isThrownBy(() -> aclEntityService.getByName("Study000"));
  }

  @Test
  public void testListUsersNoFilters() {
    entityGenerator.setupSimpleAclEntities(groups);
    val aclEntities = aclEntityService
        .listAclEntities(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(aclEntities.getTotalElements()).isEqualTo(3L);
  }

  @Test
  public void testListUsersNoFiltersEmptyResult() {
    val aclEntities = aclEntityService
        .listAclEntities(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(aclEntities.getTotalElements()).isEqualTo(0L);
  }

  @Test
  public void testListUsersFiltered() {
    entityGenerator.setupSimpleAclEntities(groups);
    val userFilter = new SearchFilter("name", "Study001");
    val aclEntities = aclEntityService
        .listAclEntities(Arrays.asList(userFilter), new PageableResolver().getPageable());
    assertThat(aclEntities.getTotalElements()).isEqualTo(1L);
  }

  @Test
  public void testListUsersFilteredEmptyResult() {
    entityGenerator.setupSimpleAclEntities(groups);
    val userFilter = new SearchFilter("name", "Study004");
    val aclEntities = aclEntityService
        .listAclEntities(Arrays.asList(userFilter), new PageableResolver().getPageable());
    assertThat(aclEntities.getTotalElements()).isEqualTo(0L);
  }


  // Update
  @Test
  public void testUpdate() {
    val policy = aclEntityService.create(entityGenerator.createOneAclEntity(Pair.of("Study001", groups.get(0).getId())));
    policy.setName("StudyOne");
    val updated = aclEntityService.update(policy);
    assertThat(updated.getName()).isEqualTo("StudyOne");
  }

  // Delete
  @Test
  public void testDelete() {
    entityGenerator.setupSimpleAclEntities(groups);

    val policy = aclEntityService.getByName("Study001");

    aclEntityService.delete(policy.getId().toString());

    val remainingAclEntities = aclEntityService.listAclEntities(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(remainingAclEntities.getTotalElements()).isEqualTo(2L);
    assertThat(remainingAclEntities.getContent()).doesNotContain(policy);
  }

}
