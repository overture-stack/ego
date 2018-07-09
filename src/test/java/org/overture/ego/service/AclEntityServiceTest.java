package org.overture.ego.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.overture.ego.controller.resolver.PageableResolver;
import org.overture.ego.model.entity.Group;
import org.overture.ego.utils.EntityGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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

  @Test
  public void testCreate() {
    val aclEntity = aclEntityService
        .create(entityGenerator.createOneAclEntity(Pair.of("Study001", groups.get(0).getId())));
    assertThat(aclEntity.getName()).isEqualTo("Study001");
  }

}
