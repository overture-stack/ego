package org.overture.ego.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.overture.ego.controller.resolver.PageableResolver;
import org.overture.ego.model.entity.Group;
import org.overture.ego.utils.EntityGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.util.Pair;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class AclEntityServiceTest {

  @Autowired
  private AclEntityService aclEntityService;

  @Autowired
  private GroupService groupService;

  @Autowired
  private EntityGenerator entityGenerator;

  private List<Group> groups;

  @Before
  public void setup() {
    // We need groups to be owners of aclEntities
    entityGenerator.setupSimpleGroups();
    groups = groupService
        .listGroups(Collections.emptyList(), new PageableResolver().getPageable())
        .getContent();
  }

  // Create
  @Test
  public void testCreate() {
    val aclEntity = aclEntityService
        .create(entityGenerator.createOneAclEntity(Pair.of("Study001", groups.get(0).getId())));
    assertThat(aclEntity.getName()).isEqualTo("Study001");
  }

  @Test
  public void testCreateUniqueName() {
    aclEntityService.create(entityGenerator.createOneAclEntity(Pair.of("Study001", groups.get(0).getId())));
    aclEntityService.create(entityGenerator.createOneAclEntity(Pair.of("Study002", groups.get(0).getId())));
    assertThatExceptionOfType(DataIntegrityViolationException.class)
        .isThrownBy(() -> aclEntityService.create(entityGenerator.createOneAclEntity(Pair.of("Study001", groups.get(0).getId()))));
  }

  // Get
  @Test
  public void testGet() {
    val aclEntity = aclEntityService.create(entityGenerator.createOneAclEntity(Pair.of("Study001", groups.get(0).getId())));
    val savedEntity = aclEntityService.get(Integer.toString(aclEntity.getId()));
    assertThat(savedEntity.getName()).isEqualTo("Study001");
  }

  @Test
  public void testGetEntityNotFoundException() {
    assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(() -> aclEntityService.get("1"));
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

  // Update
  @Test
  public void testUpdate() {
    val aclEntity = aclEntityService.create(entityGenerator.createOneAclEntity(Pair.of("Study001", groups.get(0).getId())));
    aclEntity.setName("StudyOne");
    val updated = aclEntityService.update(aclEntity);
    assertThat(updated.getName()).isEqualTo("StudyOne");
  }

  // Delete
  @Test
  public void testDelete() {
    entityGenerator.setupSimpleAclEntities(groups);

    val aclEntity = aclEntityService.getByName("Study001");

    aclEntityService.delete(Integer.toString(aclEntity.getId()));

    val remainingAclEntities = aclEntityService.listAclEntities(Collections.emptyList(), new PageableResolver().getPageable());
    assertThat(remainingAclEntities.getTotalElements()).isEqualTo(2L);
    assertThat(remainingAclEntities.getContent()).doesNotContain(aclEntity);
  }

}
