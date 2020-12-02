package bio.overture.ego.controller;

import static bio.overture.ego.model.enums.ProviderType.*;
import static bio.overture.ego.model.enums.UserType.ADMIN;
import static bio.overture.ego.model.enums.UserType.USER;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentProviderId;
import static bio.overture.ego.utils.EntityGenerator.randomEnumExcluding;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.ProviderType;
import bio.overture.ego.model.exceptions.MalformedRequestException;
import bio.overture.ego.provider.google.GoogleTokenService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.token.IDToken;
import bio.overture.ego.utils.EntityGenerator;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
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
import org.springframework.web.context.WebApplicationContext;

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

  @Value("${providerType.default:GOOGLE}")
  private ProviderType DEFAULT_PROVIDER_TYPE;

  private IDToken idToken = new IDToken();

  private HttpHeaders tokenHeaders = new HttpHeaders();

  /** Dependencies */
  @Autowired private EntityGenerator entityGenerator;

  @Autowired private UserService userService;
  @Autowired private TokenService tokenService;
  @Autowired private AuthController authController;

  @Autowired private WebApplicationContext webApplicationContext;

  private GoogleTokenService actualGoogleTokenService;

  @Rule public ExpectedException exceptionRule = ExpectedException.none();

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

    actualGoogleTokenService =
        (GoogleTokenService) ReflectionTestUtils.getField(authController, "googleTokenService");
    ReflectionTestUtils.setField(authController, "googleTokenService", mockGoogleTokenService);
  }

  @After
  public void removeMocks() {
    // replace mock tokenServices with actual services
    ReflectionTestUtils.setField(authController, "googleTokenService", actualGoogleTokenService);
  }

  @Test
  public void idToken_serializedTokenHasAllFields_Success() {
    val user = entityGenerator.setupUser("IdToken Test");

    val idToken = new IDToken();
    idToken.setEmail(user.getEmail());
    idToken.setFamilyName(user.getLastName());
    idToken.setGivenName(user.getFirstName());
    idToken.setProviderType(user.getProviderType());
    idToken.setProviderId(user.getProviderId());

    try {
      val jsonString = MAPPER.writeValueAsString(idToken);
      val json = MAPPER.readTree(jsonString);

      Stream.of("given_name", "family_name", "provider_type", "provider_id", "email")
          .forEach(
              fieldname -> {
                assertTrue(json.has(fieldname));
              });
      assertEquals(idToken.getEmail(), json.path("email").asText());
      assertEquals(idToken.getFamilyName(), json.path("family_name").asText());
      assertEquals(idToken.getGivenName(), json.path("given_name").asText());
      assertEquals(idToken.getProviderType().toString(), json.path("provider_type").asText());
      assertEquals(idToken.getProviderId(), json.path("provider_id").asText());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // not in db (default/non default)	not in db	  not in db	  create OK	  Empty Slate
  @Test
  public void nonExistingProviderTypeAndIdNonExistingEmail_createUser() {
    val firstName = entityGenerator.generateNonExistentUserName();
    val lastName = entityGenerator.generateNonExistentUserName();

    idToken.setProviderType(GOOGLE);
    idToken.setProviderId(generateNonExistentProviderId(userService));
    idToken.setEmail(format("%s%s@domain.com", firstName, lastName));
    idToken.setGivenName(firstName);
    idToken.setFamilyName(lastName);

    val response =
        initStringRequest()
            .endpoint("/oauth/google/token")
            .headers(tokenHeaders)
            .getAnd()
            .assertOk()
            .getResponse()
            .getBody();

    // assert valid token is returned
    assertTrue(tokenService.isValidToken(response));

    val user = tokenService.getTokenUserInfo(response);
    val newUser =
        initStringRequest()
            .endpoint("/users/%s", user.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    // assert new user matches idToken
    assertEquals(newUser.getProviderType(), idToken.getProviderType());
    assertEquals(newUser.getProviderId(), idToken.getProviderId());
    assertEquals(newUser.getEmail(), idToken.getEmail());
    assertEquals(newUser.getFirstName(), idToken.getGivenName());
    assertEquals(newUser.getLastName(), idToken.getFamilyName());
  }

  // existing (default/non default)	not in db	  not in db	  create OK
  @Test
  public void existingProviderTypeNonExistingProviderIdNonExistingEmail_createUser() {
    // set up user with default providerType
    val user = entityGenerator.setupUser("Existing ProviderType");

    // create idToken for a user with same providerType
    val firstName = entityGenerator.generateNonExistentUserName();
    val lastName = entityGenerator.generateNonExistentUserName();

    idToken.setProviderType(DEFAULT_PROVIDER_TYPE);
    idToken.setProviderId(generateNonExistentProviderId(userService));
    idToken.setEmail(format("%s%s@domain.com", firstName, lastName));
    idToken.setGivenName(firstName);
    idToken.setFamilyName(lastName);

    val response =
        initStringRequest()
            .endpoint("/oauth/google/token")
            .headers(tokenHeaders)
            .getAnd()
            .assertOk()
            .getResponse()
            .getBody();

    // assert valid token is returned
    assertTrue(tokenService.isValidToken(response));

    val newUserTokenInfo = tokenService.getTokenUserInfo(response);
    val newUser =
        initStringRequest()
            .endpoint("/users/%s", newUserTokenInfo.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    // assert new user matches idToken
    assertEquals(newUser.getProviderType(), idToken.getProviderType());
    assertEquals(newUser.getProviderId(), idToken.getProviderId());
    assertEquals(newUser.getEmail(), idToken.getEmail());
    assertEquals(newUser.getFirstName(), idToken.getGivenName());
    assertEquals(newUser.getLastName(), idToken.getFamilyName());

    val existingUser =
        initStringRequest()
            .endpoint("/users/%s", user.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    // assert new user does not match existing user with same providerType
    assertNotEquals(newUser.getId(), existingUser.getId());
    assertEquals(newUser.getProviderType(), existingUser.getProviderType());
    assertNotEquals(newUser.getProviderId(), existingUser.getProviderId());
    assertNotEquals(newUser.getEmail(), existingUser.getEmail());
    assertNotEquals(newUser.getFirstName(), existingUser.getFirstName());
    assertNotEquals(newUser.getLastName(), existingUser.getLastName());
  }

  // not in db (default/non default)	existing id 	email not in db	  create OK
  @Test
  public void nonExistingProviderTypeExistingProviderIdNonExistingEmail_createUser() {
    // set up user with default providerType
    val user = entityGenerator.setupUser("Existing ProviderType");

    // create idToken for a user with same providerId, different providerType
    val firstName = entityGenerator.generateNonExistentUserName();
    val lastName = entityGenerator.generateNonExistentUserName();

    idToken.setProviderType(GITHUB);
    idToken.setProviderId(user.getProviderId());
    idToken.setEmail(format("%s%s@domain.com", firstName, lastName));
    idToken.setGivenName(firstName);
    idToken.setFamilyName(lastName);

    val response =
        initStringRequest()
            .endpoint("/oauth/google/token")
            .headers(tokenHeaders)
            .getAnd()
            .assertOk()
            .getResponse()
            .getBody();

    // assert valid token is returned
    assertTrue(tokenService.isValidToken(response));

    val newUserTokenInfo = tokenService.getTokenUserInfo(response);
    val newUser =
        initStringRequest()
            .endpoint("/users/%s", newUserTokenInfo.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    // assert new user matches idToken
    assertEquals(newUser.getProviderType(), idToken.getProviderType());
    assertEquals(newUser.getProviderId(), idToken.getProviderId());
    assertEquals(newUser.getEmail(), idToken.getEmail());
    assertEquals(newUser.getFirstName(), idToken.getGivenName());
    assertEquals(newUser.getLastName(), idToken.getFamilyName());

    val existingUser =
        initStringRequest()
            .endpoint("/users/%s", user.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    // assert new user does not match existing user with same providerType
    assertNotEquals(newUser.getId(), existingUser.getId());
    assertNotEquals(newUser.getProviderType(), existingUser.getProviderType());
    assertEquals(newUser.getProviderId(), existingUser.getProviderId());
    assertNotEquals(newUser.getEmail(), existingUser.getEmail());
    assertNotEquals(newUser.getFirstName(), existingUser.getFirstName());
    assertNotEquals(newUser.getLastName(), existingUser.getLastName());
  }

  // not in db (default/non default)	not in db	  existing email	create OK
  @Test
  public void nonExistingProviderTypeAndIdExistingEmail_createUser() {
    // setup for existing user with default providerType
    val firstName = entityGenerator.generateNonExistentUserName();
    val lastName = entityGenerator.generateNonExistentUserName();
    val user =
        entityGenerator.setupUser(
            format(firstName, lastName), ADMIN, generateNonExistentProviderId(userService), GITHUB);

    // create idToken for a user with same email, non existing providerType and providerId
    idToken.setProviderType(FACEBOOK);
    idToken.setProviderId(generateNonExistentProviderId(userService));
    idToken.setEmail(user.getEmail());
    idToken.setGivenName(user.getFirstName());
    idToken.setFamilyName(user.getLastName());

    val response =
        initStringRequest()
            .endpoint("/oauth/google/token")
            .headers(tokenHeaders)
            .getAnd()
            .assertOk()
            .getResponse()
            .getBody();

    // assert valid token is returned
    assertTrue(tokenService.isValidToken(response));

    val newUserTokenInfo = tokenService.getTokenUserInfo(response);
    val newUser =
        initStringRequest()
            .endpoint("/users/%s", newUserTokenInfo.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    // assert new user matches idToken
    assertEquals(newUser.getProviderType(), idToken.getProviderType());
    assertEquals(newUser.getProviderId(), idToken.getProviderId());
    assertEquals(newUser.getEmail(), idToken.getEmail());
    assertEquals(newUser.getFirstName(), idToken.getGivenName());
    assertEquals(newUser.getLastName(), idToken.getFamilyName());

    val existingUser =
        initStringRequest()
            .endpoint("/users/%s", user.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    // assert new user does not match existing user
    assertNotEquals(newUser.getId(), existingUser.getId());
    assertNotEquals(newUser.getProviderType(), existingUser.getProviderType());
    assertNotEquals(newUser.getProviderId(), existingUser.getProviderId());
  }

  // existing provider(default)	existing id-as-email	  existing email	  user found,
  // update providerId OK
  @Test
  public void existingDefaultProviderTypeExistingProviderIdAsEmailExistingEmail_updateUser() {
    // setup for a migrated user with default providerType
    val migratedUser =
        entityGenerator.setupUser(
            "ExistingUser WithEmail",
            USER,
            "ExistingUserWithEmail@domain.com",
            DEFAULT_PROVIDER_TYPE);

    // create idToken for a user with same providerType and email, and actual providerId
    idToken.setProviderType(migratedUser.getProviderType());
    idToken.setProviderId(generateNonExistentProviderId(userService));
    idToken.setEmail(migratedUser.getEmail());
    idToken.setGivenName(migratedUser.getFirstName());
    idToken.setFamilyName(migratedUser.getLastName());

    val response =
        initStringRequest()
            .endpoint("/oauth/google/token")
            .headers(tokenHeaders)
            .getAnd()
            .assertOk()
            .getResponse()
            .getBody();

    // assert valid token is returned
    assertTrue(tokenService.isValidToken(response));

    val userTokenInfo = tokenService.getTokenUserInfo(response);
    val updatedUser =
        initStringRequest()
            .endpoint("/users/%s", userTokenInfo.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    // assert new user matches idToken
    assertEquals(updatedUser.getProviderType(), idToken.getProviderType());
    assertEquals(updatedUser.getProviderId(), idToken.getProviderId());
    assertEquals(updatedUser.getEmail(), idToken.getEmail());
    assertEquals(updatedUser.getFirstName(), idToken.getGivenName());
    assertEquals(updatedUser.getLastName(), idToken.getFamilyName());

    val existingUser =
        initStringRequest()
            .endpoint("/users/%s", migratedUser.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    // assert updatedUser and existingUser are the same
    assertEquals(updatedUser.getId(), existingUser.getId());
    assertEquals(updatedUser.getProviderType(), existingUser.getProviderType());
    assertEquals(updatedUser.getProviderId(), existingUser.getProviderId());
    assertEquals(updatedUser.getEmail(), existingUser.getEmail());

    // assert migrated user and updatedUser are the same, but providerId was updated
    assertEquals(migratedUser.getId(), updatedUser.getId());
    assertNotEquals(migratedUser.getProviderId(), updatedUser.getProviderId());
  }

  // existing provider(default/non default)	existing id 	email not in db	  user found, update email
  // OK
  @Ignore("Will be implemented with updateUser modification ticket")
  @Test
  public void existingProviderTypeExistingProviderIdNonExistingEmail_updateUser() {
    // setup for existing user with default providerType
    val user =
        entityGenerator.setupUser(
            "Old Email", USER, generateNonExistentProviderId(userService), GITHUB);

    // create idToken for a user with same providerType and email, and providerId-as-email
    idToken.setProviderType(user.getProviderType());
    idToken.setProviderId(user.getProviderId());
    idToken.setEmail(format("NewEmail@domain.com"));
    idToken.setGivenName(user.getFirstName());
    idToken.setFamilyName(user.getLastName());

    val response =
        initStringRequest()
            .endpoint("/oauth/google/token")
            .headers(tokenHeaders)
            .getAnd()
            .assertOk()
            .getResponse()
            .getBody();

    // assert valid token is returned
    assertTrue(tokenService.isValidToken(response));

    val userTokenInfo = tokenService.getTokenUserInfo(response);
    val updatedUser =
        initStringRequest()
            .endpoint("/users/%s", userTokenInfo.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    // assert new user matches idToken
    assertEquals(updatedUser.getProviderType(), idToken.getProviderType());
    assertEquals(updatedUser.getProviderId(), idToken.getProviderId());
    assertEquals(updatedUser.getEmail(), idToken.getEmail());
    assertEquals(updatedUser.getFirstName(), idToken.getGivenName());
    assertEquals(updatedUser.getLastName(), idToken.getFamilyName());

    val existingUser =
        initStringRequest()
            .endpoint("/users/%s", user.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    // assert initial user is the same as updatedUser
    assertEquals(user.getId(), updatedUser.getId());
    assertNotEquals(user.getEmail(), updatedUser.getEmail());

    // assert updatedUser and existingUser are the same
    assertEquals(updatedUser.getId(), existingUser.getId());
    assertEquals(updatedUser.getProviderType(), existingUser.getProviderType());
    assertEquals(updatedUser.getProviderId(), existingUser.getProviderId());
    assertEquals(updatedUser.getEmail(), existingUser.getEmail());
  }

  //  existing provider(default/non default)	providerId not in db	  existing email	Create OK
  @Test
  public void existingProviderTypeNonExistingProviderIdExistingEmail_createUser() {
    // setup existing user
    val user = entityGenerator.setupUser(entityGenerator.generateNonExistentUserName());

    val nonExistingProviderId = generateNonExistentProviderId(userService);

    idToken.setProviderType(DEFAULT_PROVIDER_TYPE);
    idToken.setProviderId(nonExistingProviderId);
    idToken.setEmail(user.getEmail());
    idToken.setFamilyName(user.getLastName());
    idToken.setGivenName(user.getFirstName());

    val response =
        initStringRequest()
            .endpoint("/oauth/google/token")
            .headers(tokenHeaders)
            .getAnd()
            .assertOk()
            .getResponse()
            .getBody();

    // assert valid token is returned
    assertTrue(tokenService.isValidToken(response));

    val userTokenInfo = tokenService.getTokenUserInfo(response);
    val newUser =
        initStringRequest()
            .endpoint("/users/%s", userTokenInfo.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);
    val existingUser =
        initStringRequest()
            .endpoint("/users/%s", user.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    // assert newUser matches idToken
    assertEquals(newUser.getProviderType(), idToken.getProviderType());
    assertEquals(newUser.getProviderId(), idToken.getProviderId());
    assertEquals(newUser.getEmail(), idToken.getEmail());
    assertEquals(newUser.getFirstName(), idToken.getGivenName());
    assertEquals(newUser.getLastName(), idToken.getFamilyName());

    // assert newUser is different from existingUser
    assertNotEquals(newUser.getId(), existingUser.getId());

    // assert newUser has same provider type but different providerId
    assertEquals(newUser.getProviderType(), existingUser.getProviderType());
    assertNotEquals(newUser.getProviderId(), existingUser.getProviderId());
  }

  // existing provider (default/non default)	existing id 	existing email	user found, no change
  @Test
  public void existingProviderTypeAndIdExistingEmail_noUpdate() {
    // setup user
    val user = entityGenerator.setupUser(entityGenerator.generateNonExistentUserName());

    // set idToken with user fields
    idToken.setProviderType(user.getProviderType());
    idToken.setProviderId(user.getProviderId());
    idToken.setEmail(user.getEmail());
    idToken.setFamilyName(user.getLastName());
    idToken.setGivenName(user.getFirstName());

    val response =
        initStringRequest()
            .endpoint("/oauth/google/token")
            .headers(tokenHeaders)
            .getAnd()
            .assertOk()
            .getResponse()
            .getBody();

    // assert valid token is returned
    assertTrue(tokenService.isValidToken(response));

    val tokenInfo = tokenService.getTokenUserInfo(response);
    val user1 =
        initStringRequest()
            .endpoint("/users/%s", tokenInfo.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    // assert next user matches idToken
    assertEquals(user1.getProviderType(), idToken.getProviderType());
    assertEquals(user1.getProviderId(), idToken.getProviderId());
    assertEquals(user1.getEmail(), idToken.getEmail());
    assertEquals(user1.getFirstName(), idToken.getGivenName());
    assertEquals(user1.getLastName(), idToken.getFamilyName());

    val user2 =
        initStringRequest()
            .endpoint("/users/%s", user.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    // assert user1 and user2 are the same and no properties have been updated
    assertEquals(user1.getId(), user2.getId());
    assertEquals(user1.getProviderType(), user2.getProviderType());
    assertEquals(user1.getProviderId(), user2.getProviderId());
    assertEquals(user1.getEmail(), user2.getEmail());
    assertEquals(user1.getLastName(), user2.getLastName());
    assertEquals(user1.getFirstName(), user2.getFirstName());
    assertEquals(user1.getPreferredLanguage(), user2.getPreferredLanguage());
    assertEquals(user1.getStatus(), user2.getStatus());
    assertEquals(user1.getType(), user2.getType());
  }

  // provider not in db (default/non default type)	existing id 	existing email	create OK
  @Test
  public void nonExistingProviderTypeExistingProviderIdExistingEmail_createUser() {
    // setup existing user with default providerType
    val user = entityGenerator.setupUser(entityGenerator.generateNonExistentUserName());

    // setup idToken with same providerId, email as user, different providerType
    idToken.setProviderType(randomEnumExcluding(ProviderType.class, DEFAULT_PROVIDER_TYPE));
    idToken.setProviderId(user.getProviderId());
    idToken.setEmail(user.getEmail());
    idToken.setFamilyName(user.getLastName());
    idToken.setGivenName(user.getFirstName());

    val response =
        initStringRequest()
            .endpoint("/oauth/google/token")
            .headers(tokenHeaders)
            .getAnd()
            .assertOk()
            .getResponse()
            .getBody();

    // assert valid token is returned
    assertTrue(tokenService.isValidToken(response));

    val userTokenInfo = tokenService.getTokenUserInfo(response);
    val newUser =
        initStringRequest()
            .endpoint("/users/%s", userTokenInfo.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);
    val existingUser =
        initStringRequest()
            .endpoint("/users/%s", user.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    // assert newUser matches idToken
    assertEquals(newUser.getProviderType(), idToken.getProviderType());
    assertEquals(newUser.getProviderId(), idToken.getProviderId());
    assertEquals(newUser.getEmail(), idToken.getEmail());
    assertEquals(newUser.getFirstName(), idToken.getGivenName());
    assertEquals(newUser.getLastName(), idToken.getFamilyName());

    // assert newUser is different from existingUser
    assertNotEquals(newUser.getId(), existingUser.getId());

    // assert newUser has same providerId and email but different providerType
    assertNotEquals(newUser.getProviderType(), existingUser.getProviderType());
    assertEquals(newUser.getProviderId(), existingUser.getProviderId());
    assertEquals(newUser.getEmail(), existingUser.getEmail());
  }

  // not in db (non default)
  // providerId existing as email
  // existing email
  // user found, update providerType,providerId OK
  @Test
  public void nonDefaultNonExistingProviderTypeProviderIdAsEmailExistingEmail_createUser() {
    // setup an existing user with default providerType and providerId from migration
    val migratedUser =
        entityGenerator.setupUser(
            "Migrated User", USER, "MigratedUser@domain.com", DEFAULT_PROVIDER_TYPE);

    // setup idToken with same email, actual providerType and providerId
    idToken.setProviderType(GITHUB);
    idToken.setProviderId(generateNonExistentProviderId(userService));
    idToken.setEmail(migratedUser.getEmail());
    idToken.setGivenName(migratedUser.getFirstName());
    idToken.setFamilyName(migratedUser.getLastName());

    val response =
        initStringRequest()
            .endpoint("/oauth/google/token")
            .headers(tokenHeaders)
            .getAnd()
            .assertOk()
            .getResponse()
            .getBody();

    // assert valid token is returned
    assertTrue(tokenService.isValidToken(response));

    val tokenInfo = tokenService.getTokenUserInfo(response);
    val newUser =
        initStringRequest()
            .endpoint("/users/%s", tokenInfo.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    // assert next user matches idToken
    assertEquals(newUser.getProviderType(), idToken.getProviderType());
    assertEquals(newUser.getProviderId(), idToken.getProviderId());
    assertEquals(newUser.getEmail(), idToken.getEmail());
    assertEquals(newUser.getFirstName(), idToken.getGivenName());
    assertEquals(newUser.getLastName(), idToken.getFamilyName());

    val existingUser =
        initStringRequest()
            .endpoint("/users/%s", migratedUser.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    // assert user1 and user2 are distinct users with same email
    assertNotEquals(newUser.getId(), existingUser.getId());
    assertNotEquals(newUser.getProviderType(), existingUser.getProviderType());
    assertNotEquals(newUser.getProviderId(), existingUser.getProviderId());
    assertEquals(newUser.getEmail(), existingUser.getEmail());

    // assert migratedUser has not updated providerId
    assertEquals(migratedUser.getProviderId(), migratedUser.getEmail());
    assertEquals(migratedUser.getProviderId(), existingUser.getProviderId());
  }

  @Test
  public void idTokenHasProviderInfoButNoEmail_createUser() {
    idToken.setProviderType(GOOGLE);
    idToken.setProviderId(generateNonExistentProviderId(userService));
    idToken.setGivenName("UserHas");
    idToken.setFamilyName("NoEmail");

    val response =
        initStringRequest()
            .endpoint("/oauth/google/token")
            .headers(tokenHeaders)
            .getAnd()
            .assertOk()
            .getResponse()
            .getBody();

    // assert valid token is returned
    assertTrue(tokenService.isValidToken(response));

    val user = tokenService.getTokenUserInfo(response);
    // user is created without an email
    val newUser =
        initStringRequest()
            .endpoint("/users/%s", user.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    // assert new user matches idToken
    assertEquals(newUser.getProviderType(), idToken.getProviderType());
    assertEquals(newUser.getProviderId(), idToken.getProviderId());
    assertEquals(newUser.getEmail(), idToken.getEmail());
    assertEquals(newUser.getFirstName(), idToken.getGivenName());
    assertEquals(newUser.getLastName(), idToken.getFamilyName());

    // assert email is null
    assertTrue(isNull(newUser.getEmail()));
  }

  @Test
  public void createUser_BlankProviderId_BadRequest() {
    val idToken = entityGenerator.setupUserIDToken(DEFAULT_PROVIDER_TYPE, "");

    exceptionRule.expect(MalformedRequestException.class);
    exceptionRule.expectMessage("Provider id cannot be blank.");
    userService.getUserByToken(idToken);
  }
}
