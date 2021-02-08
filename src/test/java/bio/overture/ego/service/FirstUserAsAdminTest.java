package bio.overture.ego.service;

import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.UserType;
import bio.overture.ego.utils.EntityGenerator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "default.user.firstUserAsAdmin=true")
@Transactional
public class FirstUserAsAdminTest {

  @Autowired private UserService userService;
  @Autowired private EntityGenerator entityGenerator;

  @Test
  public void testOnlyFirstUserShouldBeAdminByDefault() {
    val usersCount = userService.countAll();
    Assert.assertEquals(0, usersCount);
    User u = entityGenerator.setupUser("First User", UserType.USER);
    val user = userService.findById(u.getId()).get();
    Assert.assertEquals(user.getType(), UserType.ADMIN);

    //add another user make sure they don't get ADMIN type
    User u2 = entityGenerator.setupUser("Second User", UserType.USER);
    val user2 = userService.findById(u2.getId()).get();
    Assert.assertEquals(user2.getType(), UserType.USER);
  }

}
