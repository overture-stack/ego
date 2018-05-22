package org.overture.ego.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.overture.ego.utils.EntityGenerator;
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
public class UserServiceTest {
  @Autowired
  private ApplicationService applicationService;

  @Autowired
  private UserService userService;

  @Autowired
  private GroupService groupService;

  @Autowired
  private EntityGenerator entityGenerator;

  @Test
  public void testCreate() {

  }

  @Test
  public void testCreateUniqueName() {

  }

  @Test
  public void testCreateUniqueEmail() {

  }

  @Test
  public void testCreateFromIDToken() {

  }

  @Test
  public void testCreateFromIDTokenUniqueName() {

  }

  @Test
  public void testCreateFromIDTokenUniqueEmail() {

  }

  @Test
  public void testGet() {

  }

  @Test
  public void testGetEntityNotFoundException() {

  }

  @Test
  public void testGetByName() {

  }

  @Test
  public void testGetByNameAllCaps() {

  }

  @Test
  public void testGetByNameNotFound() {

  }

  @Test
  public void testGetOrCreateDemoUser() {

  }

  @Test
  public void testUpdate() {

  }

  @Test
  public void testUpdateNonexistentEntity() {

  }

  @Test
  public void testUpdateIdNotAllowed() {

  }

  @Test
  public void testUpdateNameNotAllowed() {

  }

  @Test
  public void testUpdateEmailNotAllowed() {

  }

  @Test
  public void testUpdateClientIdNotAllowed() {

  }

  @Test
  public void testGet() {

  }

  @Test
  public void testGet() {

  }

  @Test
  public void testGet() {

  }

  @Test
  public void testGet() {

  }

  @Test
  public void testGet() {

  }
}
