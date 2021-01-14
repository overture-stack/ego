package bio.overture.ego.token;

import static bio.overture.ego.model.enums.ProviderType.GOOGLE;
import static bio.overture.ego.model.enums.UserType.ADMIN;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import bio.overture.ego.model.entity.User;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import java.util.Date;
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
    idToken.setFamilyName("foo");
    idToken.setGivenName("bar");
    idToken.setEmail("foobar@domain.com");
    idToken.setProviderType(GOOGLE);
    idToken.setProviderSubjectId("12345");
    User user = entityGenerator.setupUser("foo bar", ADMIN, "12345", GOOGLE);

    assertNull(
        " Verify before generatedUserToken, last login after fetching the user should be null. ",
        userService
            .getByProviderTypeAndProviderSubjectId(
                idToken.getProviderType(), idToken.getProviderSubjectId())
            .getLastLogin());

    tokenService.generateUserToken(idToken);

    val lastLogin =
        userService
            .getByProviderTypeAndProviderSubjectId(
                idToken.getProviderType(), idToken.getProviderSubjectId())
            .getLastLogin();
    userService.delete(user.getId());

    assertNotNull("Verify after generatedUserToken, last login is not null.", lastLogin);
    val tolerance = 2 * 1000; // 2 seconds
    val now = new Date().getTime();
    assert (now - lastLogin.getTime()) <= tolerance;
  }
}
