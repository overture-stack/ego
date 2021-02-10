package bio.overture.ego.controller;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.ProviderType;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.UUID;

import static bio.overture.ego.model.enums.UserType.ADMIN;
import static bio.overture.ego.model.enums.UserType.USER;
import static java.lang.String.format;
import static org.junit.Assert.*;

@Slf4j
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "default.user.firstUserAsAdmin=true")
public class FirstUserAsAdminUserControllerTest extends AbstractMockedTokenControllerTest {

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void createFirstUserShouldBeAdmin() {
    this.userService.getRepository().deleteAll();
    val user1 = createUserAndFetchInfo("first@gmail.com", "first", "last");
    assertEquals(ADMIN, user1.getType());

    val user2 = createUserAndFetchInfo("second@gmail.com", "second", "third");
    assertEquals(USER, user2.getType());
  }


  private User createUserAndFetchInfo(String email, String first, String last) {
    // set id token mock
    idToken.setProviderType(ProviderType.GOOGLE);
    idToken.setProviderSubjectId(UUID.randomUUID().toString());
    idToken.setEmail(email);
    idToken.setFamilyName(first);
    idToken.setGivenName(last);

    val response = getTokenResponse();

    // assert valid token is returned
    assertTrue(tokenService.isValidToken(response));
    val tokenInfo = tokenService.getTokenUserInfo(response);
    val user1 =
        initStringRequest()
            .endpoint("/users/%s", tokenInfo.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    return user1;
  }
}


