package bio.overture.ego.controller;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.ProviderType;
import bio.overture.ego.provider.facebook.FacebookTokenService;
import bio.overture.ego.provider.google.GoogleTokenService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.token.IDToken;
import bio.overture.ego.utils.EntityGenerator;
import com.google.api.client.auth.openidconnect.IdToken;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.WebApplicationContext;

import static bio.overture.ego.model.enums.ProviderType.GOOGLE;
import static java.lang.String.format;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@Slf4j
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
  classes = AuthorizationServiceMain.class,
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CreateUserControllerTest extends AbstractControllerTest {

  private static boolean hasRunEntitySetup = false;
  private MockMvc mockMvc;
  private static final ProviderType DEFAULT_PROVIDER = GOOGLE;

  private IDToken idToken = new IDToken();
  private ProviderType provider;
  private String providerId;
  private String familyName;
  private String givenName;
  private String email;

  private HttpHeaders tokenHeaders = new HttpHeaders();

  /** Dependencies */
  @Autowired private EntityGenerator entityGenerator;

  @Autowired private UserService userService;
  @Autowired private TokenService tokenService;
  @Autowired private GoogleTokenService googleTokenService;
  @Autowired private AuthController authController;

  @Autowired private WebApplicationContext webApplicationContext;

  private GoogleTokenService actualGoogleTokenService;

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Value("${logging.test.controller.enable}")
  private boolean enableLogging;

  @Override
  protected boolean enableLogging() {
    return enableLogging;
  }

  @Override
  protected void beforeTest() {
    // Initial setup of entities (run once)
    if (!hasRunEntitySetup) {
      entityGenerator.setupTestUsers();
      hasRunEntitySetup = true;
    }
  }

  @Before
  public void beforeEachTest() {
    tokenHeaders.set("token", "aValidTokenHeader");
    this.mockMvc = webAppContextSetup(webApplicationContext).build();

    val mockGoogleTokenService = mock(GoogleTokenService.class);

    Mockito.when(mockGoogleTokenService.validToken(Mockito.anyString())).thenReturn(true);
    Mockito.when(mockGoogleTokenService.decode(Mockito.anyString())).thenReturn(idToken);

    actualGoogleTokenService = (GoogleTokenService) ReflectionTestUtils.getField(authController, "googleTokenService");
    ReflectionTestUtils.setField(authController, "googleTokenService", mockGoogleTokenService);

  }

  @After
  public void removeMocks() {
    // replace mock tokenServices with actual services
    ReflectionTestUtils.setField(authController, "googleTokenService", actualGoogleTokenService);
  }

  @Test
  public void createUser_NonExistingProviderInfoNonExistingEmail_Success() {
    val firstName = entityGenerator.generateNonExistentUserName();
    val lastName = entityGenerator.generateNonExistentUserName();

    idToken.setProviderType(GOOGLE);
    idToken.setProviderId(EntityGenerator.generateNonExistentProviderId(userService));
    idToken.setEmail(format("%s%s@domain.com", firstName, lastName));
    idToken.setGivenName(firstName);
    idToken.setFamilyName(lastName);

    val loginReq = initStringRequest().endpoint("/oauth/google/token").headers(tokenHeaders).getAnd();

    loginReq.assertOk();
    val response = loginReq.getResponse().getBody();

    // assert valid token is returned
    assertTrue(tokenService.isValidToken(response));

    val newUser = tokenService.getTokenUserInfo(response);

    // assert new user matches idToken
    assertEquals(newUser.getProviderType(), idToken.getProviderType());
    assertEquals(newUser.getProviderId(), idToken.getProviderId());
    assertEquals(newUser.getEmail(), idToken.getEmail());
    assertEquals(newUser.getFirstName(), idToken.getGivenName());
    assertEquals(newUser.getLastName(), idToken.getFamilyName());
  }

  @Test
  public void createUser_ExistingProviderNonExistingProviderIdNonExistingEmail_Success() {
    // set up user with default providerType
    val user = entityGenerator.setupUser("Existing ProviderType");

    // create idToken for a user with same providerType
    val firstName = entityGenerator.generateNonExistentUserName();
    val lastName = entityGenerator.generateNonExistentUserName();

    idToken.setProviderType(DEFAULT_PROVIDER);
    idToken.setProviderId(EntityGenerator.generateNonExistentProviderId(userService));
    idToken.setEmail(format("%s%s@domain.com", firstName, lastName));
    idToken.setGivenName(firstName);
    idToken.setFamilyName(lastName);

    val loginReq = initStringRequest().endpoint("/oauth/google/token").headers(tokenHeaders).getAnd();

    loginReq.assertOk();
    val response = loginReq.getResponse().getBody();
    // assert valid token is returned
    assertTrue(tokenService.isValidToken(response));

    val newUser = tokenService.getTokenUserInfo(response);

    // assert new user matches idToken
    assertEquals(newUser.getProviderType(), idToken.getProviderType());
    assertEquals(newUser.getProviderId(), idToken.getProviderId());
    assertEquals(newUser.getEmail(), idToken.getEmail());
    assertEquals(newUser.getFirstName(), idToken.getGivenName());
    assertEquals(newUser.getLastName(), idToken.getFamilyName());

    val existingUser = initStringRequest().endpoint("/users/%s", user.getId()).getAnd().assertOk().extractOneEntity(User.class);

    // assert new user does not match existing user with same providerType
    assertNotEquals(newUser.getId(), existingUser.getId());
    assertEquals(newUser.getProviderType(), existingUser.getProviderType());
    assertNotEquals(newUser.getProviderId(), existingUser.getProviderId());
    assertNotEquals(newUser.getEmail(), existingUser.getEmail());
    assertNotEquals(newUser.getFirstName(), existingUser.getFirstName());
    assertNotEquals(newUser.getLastName(), existingUser.getLastName());
  }
}
