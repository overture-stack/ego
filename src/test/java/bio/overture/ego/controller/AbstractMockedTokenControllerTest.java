package bio.overture.ego.controller;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.ProviderType;
import bio.overture.ego.provider.google.GoogleTokenService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.token.IDToken;
import bio.overture.ego.utils.EntityGenerator;
import java.util.List;
import lombok.val;
import org.junit.After;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

public abstract class AbstractMockedTokenControllerTest extends AbstractControllerTest {

  protected boolean hasRunEntitySetup = false;
  protected List<User> testUsers;
  protected MockMvc mockMvc;

  @Value("${spring.flyway.placeholders.default-provider:GOOGLE}")
  protected ProviderType defaultProviderType;

  protected IDToken idToken;

  protected final HttpHeaders tokenHeaders = new HttpHeaders();

  /** Dependencies */
  @Autowired protected EntityGenerator entityGenerator;

  @Autowired protected UserService userService;
  @Autowired protected TokenService tokenService;
  @Autowired protected AuthController authController;

  @Autowired protected WebApplicationContext webApplicationContext;

  protected GoogleTokenService actualGoogleTokenService;

  @Value("${logging.test.controller.enable}")
  protected boolean enableLogging;

  @Override
  protected boolean enableLogging() {
    return enableLogging;
  }

  @Override
  protected void beforeTest() {
    // Initial setup of entities (run once)
    if (!hasRunEntitySetup) {
      testUsers = entityGenerator.setupTestUsers();
      hasRunEntitySetup = true;
    }

    // we are mocking the googleTokenService because we're not looking to test the response from the
    // IdPs,
    // we just need a dummy accessToken to test the login flow once ego receives this token from a
    // given IdP
    tokenHeaders.set("token", "aValidTokenHeader");
    this.mockMvc = webAppContextSetup(webApplicationContext).build();

    val mockGoogleTokenService = mock(GoogleTokenService.class);
    idToken = entityGenerator.createNewIdToken();

    Mockito.when(mockGoogleTokenService.validToken(Mockito.anyString())).thenReturn(true);
    Mockito.when(mockGoogleTokenService.decode(Mockito.anyString())).thenReturn(idToken);

    actualGoogleTokenService =
        (GoogleTokenService) ReflectionTestUtils.getField(authController, "googleTokenService");
    ReflectionTestUtils.setField(authController, "googleTokenService", mockGoogleTokenService);
  }

  @After
  public void removeMocks() {
    // replace mock tokenServices with actual services
    ReflectionTestUtils.setField(authController, "googleTokenService", actualGoogleTokenService);
  }

  protected String getTokenResponse() {
    return initStringRequest()
        .endpoint("/oauth/google/token")
        .headers(tokenHeaders)
        .getAnd()
        .assertOk()
        .getResponse()
        .getBody();
  }
}
