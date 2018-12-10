package bio.overture.ego.token;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import bio.overture.ego.model.entity.User;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class LastloginTest {

  @Autowired private TokenService tokenService;

  @Autowired private UserService userService;

  @Autowired private EntityGenerator entityGenerator;

  @Test
  @SneakyThrows
  public void testLastloginUpdate() {

    val idToken = new IDToken();
    idToken.setFamily_name("foo");
    idToken.setGiven_name("bar");
    idToken.setEmail("foobar@domain.com");
    User user = userService.create(entityGenerator.createUser("foo", "bar"));

    assertNull(
        " Verify before generatedUserToken, last login after fetching the user should be null. ",
        userService.getByName(idToken.getEmail()).getLastLogin());

    tokenService.generateUserToken(idToken);

    // Another thread is setting user.lastlogin, make main thread wait until setting is complete.
    Thread.sleep(200);

    val lastLogin = userService.getByName(idToken.getEmail()).getLastLogin();

    // Must manually delete user. Using @Transactional will
    // trigger exception, as there are two
    // threads involved, new thread will try to find user in an empty repo which
    // will cause exception. This is done even if lastLogin assertion fails
    userService.delete(user.getId().toString());

    assertNotNull("Verify after generatedUserToken, last login is not null.", lastLogin);
  }
}
