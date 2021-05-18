package bio.overture.ego.controller;

import static bio.overture.ego.model.enums.ProviderType.*;
import static bio.overture.ego.model.enums.UserType.ADMIN;
import static bio.overture.ego.model.enums.UserType.USER;
import static bio.overture.ego.utils.EntityGenerator.*;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static org.junit.Assert.*;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.ProviderType;
import bio.overture.ego.model.exceptions.MalformedRequestException;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CreateUserControllerTest extends AbstractMockedTokenControllerTest {

  @Rule public ExpectedException exceptionRule = ExpectedException.none();

  @SneakyThrows
  @Test
  public void idToken_serializedTokenHasAllFields_Success() {
    val user = entityGenerator.setupUser("IdToken Test");

    idToken.setEmail(user.getEmail());
    idToken.setFamilyName(user.getLastName());
    idToken.setGivenName(user.getFirstName());
    idToken.setProviderType(user.getProviderType());
    idToken.setProviderSubjectId(user.getProviderSubjectId());

    val jsonString = MAPPER.writeValueAsString(idToken);
    val json = MAPPER.readTree(jsonString);

    Stream.of("given_name", "family_name", "provider_type", "provider_subject_id", "email")
        .forEach(
            fieldname -> {
              assertTrue(json.has(fieldname));
            });
    assertEquals(idToken.getEmail(), json.path("email").asText());
    assertEquals(idToken.getFamilyName(), json.path("family_name").asText());
    assertEquals(idToken.getGivenName(), json.path("given_name").asText());
    assertEquals(idToken.getProviderType().toString(), json.path("provider_type").asText());
    assertEquals(idToken.getProviderSubjectId(), json.path("provider_subject_id").asText());
  }

  // not in db (default/non default)	not in db	  not in db	  create OK	  Empty Slate
  @Test
  public void nonExistingProviderTypeAndIdNonExistingEmail_createUser() {
    val response = getTokenResponse();

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
    assertEquals(newUser.getProviderSubjectId(), idToken.getProviderSubjectId());
    assertEquals(newUser.getEmail(), idToken.getEmail());
    assertEquals(newUser.getFirstName(), idToken.getGivenName());
    assertEquals(newUser.getLastName(), idToken.getFamilyName());
  }

  // existing (default/non default)	not in db	  not in db	  create OK
  @Test
  public void existingProviderTypeNonExistingProviderSubjectIdNonExistingEmail_createUser() {
    // set up user with default providerType
    val user = entityGenerator.setupUser("Existing ProviderType");

    val response = getTokenResponse();

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
    assertEquals(newUser.getProviderSubjectId(), idToken.getProviderSubjectId());
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
    assertNotEquals(newUser.getProviderSubjectId(), existingUser.getProviderSubjectId());
    assertNotEquals(newUser.getEmail(), existingUser.getEmail());
    assertNotEquals(newUser.getFirstName(), existingUser.getFirstName());
    assertNotEquals(newUser.getLastName(), existingUser.getLastName());
  }

  // not in db (default/non default)	existing id 	email not in db	  create OK
  @Test
  public void nonExistingProviderTypeExistingProviderSubjectIdNonExistingEmail_createUser() {
    // set up user with default providerType
    val user = entityGenerator.setupUser("Existing ProviderType");

    // create idToken for a user with same providerSubjectId, different providerType
    idToken.setProviderType(entityGenerator.createNonDefaultProviderType());
    idToken.setProviderSubjectId(user.getProviderSubjectId());

    val response = getTokenResponse();

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
    assertEquals(newUser.getProviderSubjectId(), idToken.getProviderSubjectId());
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
    assertEquals(newUser.getProviderSubjectId(), existingUser.getProviderSubjectId());
    assertNotEquals(newUser.getEmail(), existingUser.getEmail());
    assertNotEquals(newUser.getFirstName(), existingUser.getFirstName());
    assertNotEquals(newUser.getLastName(), existingUser.getLastName());
  }

  // not in db (default/non default)	not in db	  existing email	create OK
  @Test
  public void nonExistingProviderTypeAndIdExistingEmail_createUser() {
    // setup for existing user with default providerType
    val names = entityGenerator.generateNonExistentUserName().split(" ");
    val firstName = names[0];
    val lastName = names[1];
    val user =
        entityGenerator.setupUser(
            format("%s %s", firstName, lastName),
            ADMIN,
            generateNonExistentProviderSubjectId(userService),
            GITHUB);

    // create idToken for a user with same email, non existing providerType and providerSubjectId
    idToken.setProviderType(randomEnumExcluding(ProviderType.class, user.getProviderType()));
    idToken.setEmail(user.getEmail());
    idToken.setGivenName(user.getFirstName());
    idToken.setFamilyName(user.getLastName());

    val response = getTokenResponse();

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
    assertEquals(newUser.getProviderSubjectId(), idToken.getProviderSubjectId());
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
    assertNotEquals(newUser.getProviderSubjectId(), existingUser.getProviderSubjectId());
  }

  //  existing provider(default/non default)	providerSubjectId not in db	  existing email	Create OK
  @Test
  public void existingProviderTypeNonExistingProviderSubjectIdExistingEmail_createUser() {
    // setup existing user
    val user = entityGenerator.setupUser(entityGenerator.generateNonExistentUserName());

    val nonExistingProviderSubjectId = generateNonExistentProviderSubjectId(userService);

    //    idToken.setProviderType(defaultProviderType);
    idToken.setProviderSubjectId(nonExistingProviderSubjectId);
    idToken.setEmail(user.getEmail());
    idToken.setFamilyName(user.getLastName());
    idToken.setGivenName(user.getFirstName());

    val response = getTokenResponse();

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
    assertEquals(newUser.getProviderSubjectId(), idToken.getProviderSubjectId());
    assertEquals(newUser.getEmail(), idToken.getEmail());
    assertEquals(newUser.getFirstName(), idToken.getGivenName());
    assertEquals(newUser.getLastName(), idToken.getFamilyName());

    // assert newUser is different from existingUser
    assertNotEquals(newUser.getId(), existingUser.getId());

    // assert newUser has same provider type but different providerSubjectId
    assertEquals(newUser.getProviderType(), existingUser.getProviderType());
    assertNotEquals(newUser.getProviderSubjectId(), existingUser.getProviderSubjectId());
  }

  // existing provider (default/non default)	existing id 	existing email	user found, no change
  @Test
  public void existingProviderTypeAndIdExistingEmail_noUpdate() {
    // setup user
    val user = entityGenerator.setupUser(entityGenerator.generateNonExistentUserName());

    // set idToken with user fields
    idToken.setProviderType(user.getProviderType());
    idToken.setProviderSubjectId(user.getProviderSubjectId());
    idToken.setEmail(user.getEmail());
    idToken.setFamilyName(user.getLastName());
    idToken.setGivenName(user.getFirstName());

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

    // assert next user matches idToken
    assertEquals(user1.getProviderType(), idToken.getProviderType());
    assertEquals(user1.getProviderSubjectId(), idToken.getProviderSubjectId());
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
    assertEquals(user1.getProviderSubjectId(), user2.getProviderSubjectId());
    assertEquals(user1.getEmail(), user2.getEmail());
    assertEquals(user1.getLastName(), user2.getLastName());
    assertEquals(user1.getFirstName(), user2.getFirstName());
    assertEquals(user1.getPreferredLanguage(), user2.getPreferredLanguage());
    assertEquals(user1.getStatus(), user2.getStatus());
    assertEquals(user1.getType(), user2.getType());
  }

  // provider not in db (default/non default type)	existing id 	existing email	create OK
  @Test
  public void nonExistingProviderTypeExistingProviderSubjectIdExistingEmail_createUser() {
    // setup existing user with default providerType
    val user = entityGenerator.setupUser(entityGenerator.generateNonExistentUserName());

    // assert user has default providerType
    assertEquals(user.getProviderType(), defaultProviderType);
    // setup idToken with same providerSubjectId, email as user, different providerType
    idToken.setProviderType(entityGenerator.createNonDefaultProviderType());
    idToken.setProviderSubjectId(user.getProviderSubjectId());
    idToken.setEmail(user.getEmail());
    idToken.setFamilyName(user.getLastName());
    idToken.setGivenName(user.getFirstName());

    val response = getTokenResponse();

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
    assertEquals(newUser.getProviderSubjectId(), idToken.getProviderSubjectId());
    assertEquals(newUser.getEmail(), idToken.getEmail());
    assertEquals(newUser.getFirstName(), idToken.getGivenName());
    assertEquals(newUser.getLastName(), idToken.getFamilyName());

    // assert newUser is different from existingUser
    assertNotEquals(newUser.getId(), existingUser.getId());

    // assert newUser has same providerSubjectId and email but different providerType
    assertNotEquals(newUser.getProviderType(), existingUser.getProviderType());
    assertEquals(newUser.getProviderSubjectId(), existingUser.getProviderSubjectId());
    assertEquals(newUser.getEmail(), existingUser.getEmail());
  }

  // not in db (non default)
  // providerSubjectId existing as email
  // existing email
  // user found, update providerType, providerSubjectId OK
  @Test
  public void nonDefaultNonExistingProviderTypeProviderSubjectIdAsEmailExistingEmail_createUser() {
    // setup an existing user with default providerType and providerSubjectId from migration
    val migratedUser =
        entityGenerator.setupUser(
            "Migrated User", USER, "MigratedUser@domain.com", defaultProviderType);

    // assert migratedUser providerSubjectId matches email
    assertEquals(migratedUser.getProviderSubjectId(), migratedUser.getEmail());

    // setup idToken with same email, actual providerType and providerSubjectId
    idToken.setProviderType(entityGenerator.createNonDefaultProviderType());
    idToken.setEmail(migratedUser.getEmail());
    idToken.setGivenName(migratedUser.getFirstName());
    idToken.setFamilyName(migratedUser.getLastName());

    val response = getTokenResponse();

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
    assertEquals(newUser.getProviderSubjectId(), idToken.getProviderSubjectId());
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
    assertNotEquals(newUser.getProviderSubjectId(), existingUser.getProviderSubjectId());
    assertEquals(newUser.getEmail(), existingUser.getEmail());

    // assert migratedUser has not updated providerSubjectId
    assertEquals(migratedUser.getProviderSubjectId(), migratedUser.getEmail());
    assertEquals(migratedUser.getProviderSubjectId(), existingUser.getProviderSubjectId());
  }

  @Test
  public void
      nonDefaultNonExistingProviderTypeProviderSubjectIdAsEmailExistingEmailMixedCase_createUser() {
    // setup an existing user with default providerType and providerSubjectId from migration
    val migratedUser =
        entityGenerator.setupUser(
            "Migrated User", USER, "MigratedUser@domain.com", defaultProviderType);

    // assert migratedUser providerSubjectId matches email
    assertEquals(migratedUser.getProviderSubjectId(), migratedUser.getEmail());

    val emailWithMixedCase = "mIGRatedUsER@domain.com";
    // setup idToken with same email but mixed case, actual providerType and providerSubjectId
    idToken.setProviderType(entityGenerator.createNonDefaultProviderType());
    idToken.setEmail(emailWithMixedCase);
    idToken.setGivenName(migratedUser.getFirstName());
    idToken.setFamilyName(migratedUser.getLastName());

    val response = getTokenResponse();

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
    assertEquals(newUser.getProviderSubjectId(), idToken.getProviderSubjectId());
    assertEquals(newUser.getEmail(), idToken.getEmail());
    assertEquals(newUser.getFirstName(), idToken.getGivenName());
    assertEquals(newUser.getLastName(), idToken.getFamilyName());

    val existingUser =
        initStringRequest()
            .endpoint("/users/%s", migratedUser.getId())
            .getAnd()
            .assertOk()
            .extractOneEntity(User.class);

    // assert user1 and user2 are distinct users with same email (ignoring case)
    assertNotEquals(newUser.getId(), existingUser.getId());
    assertNotEquals(newUser.getProviderType(), existingUser.getProviderType());
    assertNotEquals(newUser.getProviderSubjectId(), existingUser.getProviderSubjectId());
    assertNotEquals(newUser.getEmail(), existingUser.getEmail());
    assertTrue(newUser.getEmail().equalsIgnoreCase(existingUser.getEmail()));

    // assert migratedUser has not updated providerSubjectId
    assertEquals(migratedUser.getProviderSubjectId(), migratedUser.getEmail());
    assertEquals(migratedUser.getProviderSubjectId(), existingUser.getProviderSubjectId());
  }

  @Test
  public void idTokenHasProviderInfoButNoEmail_createUser() {
    idToken.setGivenName("UserHas");
    idToken.setFamilyName("NoEmail");
    idToken.setEmail(null);

    val response = getTokenResponse();

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
    assertEquals(newUser.getProviderSubjectId(), idToken.getProviderSubjectId());
    assertEquals(newUser.getEmail(), idToken.getEmail());
    assertEquals(newUser.getFirstName(), idToken.getGivenName());
    assertEquals(newUser.getLastName(), idToken.getFamilyName());

    // assert email is null
    assertTrue(isNull(newUser.getEmail()));
  }

  // no test required for a blank providerType as this value is hardcoded in each SSOFilter
  @Test
  public void createUser_BlankProviderSubjectId_BadRequest() {
    val idToken = entityGenerator.setupUserIDToken(defaultProviderType, "");

    exceptionRule.expect(MalformedRequestException.class);
    exceptionRule.expectMessage("ProviderSubjectId cannot be blank.");
    userService.getUserByToken(idToken);
  }
}
