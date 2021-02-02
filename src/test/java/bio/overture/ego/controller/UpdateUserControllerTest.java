package bio.overture.ego.controller;

import static bio.overture.ego.model.enums.JavaFields.*;
import static bio.overture.ego.model.enums.LanguageType.*;
import static bio.overture.ego.model.enums.ProviderType.FACEBOOK;
import static bio.overture.ego.model.enums.ProviderType.GITHUB;
import static bio.overture.ego.model.enums.StatusType.DISABLED;
import static bio.overture.ego.model.enums.UserType.USER;
import static bio.overture.ego.utils.CollectionUtils.repeatedCallsOf;
import static bio.overture.ego.utils.EntityGenerator.*;
import static bio.overture.ego.utils.Streams.stream;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpStatus.FORBIDDEN;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.UpdateUserRequest;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.LanguageType;
import bio.overture.ego.model.enums.StatusType;
import bio.overture.ego.model.enums.UserType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UpdateUserControllerTest extends AbstractMockedTokenControllerTest {

  /** * --- ui user update tests --- ** */
  @Test
  public void validateUpdateRequest_ProviderSubjectIdDoesntMatch_Forbidden() {
    // create a user with providerInfo
    val data = generateUniqueTestUserData();
    val user = data.getUsers().get(0);

    val nonExistentProviderSubjectId = generateNonExistentProviderSubjectId(userService);

    // Assert update with different providerSubjectId
    val r1 =
        UpdateUserRequest.builder()
            .providerType(user.getProviderType())
            .providerSubjectId(nonExistentProviderSubjectId)
            .preferredLanguage(SPANISH)
            .build();

    initStringRequest()
        .endpoint("/users/%s", user.getId())
        .body(r1)
        .putAnd()
        .assertStatusCode(FORBIDDEN);
  }

  @Test
  public void validateUpdateRequest_ProviderTypeDoesntMatch_Forbidden() {
    // create a user with providerInfo
    val data = generateUniqueTestUserData();
    val user = data.getUsers().get(0);

    // Assert update with different providerType
    val r1 =
        UpdateUserRequest.builder()
            .providerType(GITHUB)
            .providerSubjectId(user.getProviderSubjectId())
            .preferredLanguage(FRENCH)
            .build();

    initStringRequest()
        .endpoint("/users/%s", user.getId())
        .body(r1)
        .putAnd()
        .assertStatusCode(FORBIDDEN);
  }

  @Test
  @SneakyThrows
  public void updateUser_ExistingUserProviderInfoMatches_Success() {
    // Generate data
    val data = generateUniqueTestUserData();
    val user = data.getUsers().get(0);

    // create update request 1
    val r1 =
        UpdateUserRequest.builder()
            .preferredLanguage(randomEnumExcluding(LanguageType.class, user.getPreferredLanguage()))
            .status(randomEnumExcluding(StatusType.class, user.getStatus()))
            .type(randomEnumExcluding(UserType.class, user.getType()))
            .providerType(user.getProviderType())
            .providerSubjectId(user.getProviderSubjectId())
            .build();

    // Update user
    partialUpdateUserPutRequestAnd(user.getId(), r1).assertOk();

    // Assert update was correct
    val updatedUser = getUserEntityGetRequestAnd(user).extractOneEntity(User.class);

    assertEquals(updatedUser.getProviderType(), r1.getProviderType());
    assertEquals(updatedUser.getProviderSubjectId(), r1.getProviderSubjectId());

    assertNotEquals(updatedUser.getStatus(), user.getStatus());
    assertNotEquals(updatedUser.getType(), user.getType());
    assertNotEquals(updatedUser.getPreferredLanguage(), user.getPreferredLanguage());

    assertEquals(updatedUser.getStatus(), r1.getStatus());
    assertEquals(updatedUser.getType(), r1.getType());
    assertEquals(updatedUser.getPreferredLanguage(), r1.getPreferredLanguage());
  }

  @Test
  public void updateUser_NonExistentUser_NotFound() {
    // Create non existent user id
    val nonExistentId = generateNonExistentId(userService);

    // Create a request with dummy providerInfo
    val dummyUpdateUserRequest =
        UpdateUserRequest.builder()
            .providerType(defaultProviderType)
            .providerSubjectId(generateNonExistentProviderSubjectId(userService))
            .build();

    // Assert that you cannot get a non-existent id
    partialUpdateUserPutRequestAnd(nonExistentId, dummyUpdateUserRequest).assertNotFound();
  }

  @Test
  public void statusValidation_MalformedStatus_BadRequest() {
    val invalidStatus = "something123";
    val match = stream(StatusType.values()).anyMatch(x -> x.toString().equals(invalidStatus));
    assertFalse(match);

    val data = generateUniqueTestUserData();
    val user = data.getUsers().get(0);

    // Assert updateUser
    val templateR2 =
        UpdateUserRequest.builder()
            .providerType(user.getProviderType())
            .providerSubjectId(user.getProviderSubjectId())
            .type(USER)
            .preferredLanguage(ENGLISH)
            .build();
    val r2 = ((ObjectNode) MAPPER.valueToTree(templateR2)).put(STATUS, invalidStatus);
    initStringRequest().endpoint("/users/%s", user.getId()).body(r2).putAnd().assertBadRequest();
  }

  @Test
  public void typeValidation_MalformedType_BadRequest() {
    val invalidType = "something123";
    val match = stream(UserType.values()).anyMatch(x -> x.toString().equals(invalidType));
    assertFalse(match);

    val data = generateUniqueTestUserData();
    val user = data.getUsers().get(0);

    // Assert updateUser
    val templateR2 =
        UpdateUserRequest.builder()
            .providerType(user.getProviderType())
            .providerSubjectId(user.getProviderSubjectId())
            .status(DISABLED)
            .preferredLanguage(ENGLISH)
            .build();
    val r2 = ((ObjectNode) MAPPER.valueToTree(templateR2)).put(TYPE, invalidType);
    initStringRequest().endpoint("/users/%s", user.getId()).body(r2).putAnd().assertBadRequest();
  }

  @Test
  public void preferredLanguageValidation_MalformedPreferredLanguage_BadRequest() {
    val invalidLanguage = "something123";
    val match = stream(LanguageType.values()).anyMatch(x -> x.toString().equals(invalidLanguage));
    assertFalse(match);

    val data = generateUniqueTestUserData();
    val user = data.getUsers().get(0);

    // Assert updateUser
    val templateR2 =
        UpdateUserRequest.builder()
            .providerType(user.getProviderType())
            .providerSubjectId(user.getProviderSubjectId())
            .status(DISABLED)
            .type(USER)
            .build();
    val r2 = ((ObjectNode) MAPPER.valueToTree(templateR2)).put(PREFERREDLANGUAGE, invalidLanguage);
    initStringRequest().endpoint("/users/%s", user.getId()).body(r2).putAnd().assertBadRequest();
  }

  /** * --- login path user update tests --- ** */

  // existing provider(default/non default)	existing id 	email not in db	  user found, update email
  // OK
  @Test
  public void existingProviderTypeExistingProviderSubjectIdNewEmailFromToken_updateUser() {
    val user =
        entityGenerator.setupUser(
            "Old Email", USER, generateNonExistentProviderSubjectId(userService), GITHUB);

    // create idToken for a user with same providerType and email, and providerSubjectId-as-email
    idToken.setProviderType(user.getProviderType());
    idToken.setProviderSubjectId(user.getProviderSubjectId());
    idToken.setEmail("NewEmail@domain.com");
    idToken.setGivenName(user.getFirstName());
    idToken.setFamilyName(user.getLastName());

    val response = getTokenResponse();

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
    assertEquals(updatedUser.getProviderSubjectId(), idToken.getProviderSubjectId());
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
    assertEquals(updatedUser.getProviderSubjectId(), existingUser.getProviderSubjectId());
    assertEquals(updatedUser.getEmail(), existingUser.getEmail());
    assertEquals(updatedUser.getFirstName(), existingUser.getFirstName());
    assertEquals(updatedUser.getLastName(), existingUser.getLastName());
  }

  // existing provider(default/non default)	existing id 	email not in db	  user found, update
  // firstName
  // OK
  @Test
  public void existingProviderTypeExistingProviderSubjectIdNewFirstNameFromToken_updateUser() {
    val user =
        entityGenerator.setupUser(
            "Old FirstName", USER, generateNonExistentProviderSubjectId(userService), FACEBOOK);

    // create idToken for a user with same providerType and email, and providerSubjectId-as-email
    idToken.setProviderType(user.getProviderType());
    idToken.setProviderSubjectId(user.getProviderSubjectId());
    idToken.setEmail(user.getEmail());
    idToken.setGivenName("NewFirstName");
    idToken.setFamilyName(user.getLastName());

    val response = getTokenResponse();

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
    assertEquals(updatedUser.getProviderSubjectId(), idToken.getProviderSubjectId());
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
    assertNotEquals(user.getFirstName(), updatedUser.getFirstName());

    // assert updatedUser and existingUser are the same
    assertEquals(updatedUser.getId(), existingUser.getId());
    assertEquals(updatedUser.getProviderType(), existingUser.getProviderType());
    assertEquals(updatedUser.getProviderSubjectId(), existingUser.getProviderSubjectId());
    assertEquals(updatedUser.getEmail(), existingUser.getEmail());
    assertEquals(updatedUser.getFirstName(), existingUser.getFirstName());
    assertEquals(updatedUser.getLastName(), existingUser.getLastName());
  }

  // existing provider(default/non default)	existing id 	email not in db	  user found, update
  // lastName
  // OK
  @Test
  public void existingProviderTypeExistingProviderSubjectIdNewLastNameFromToken_updateUser() {
    val user =
        entityGenerator.setupUser(
            "Old LastName", USER, generateNonExistentProviderSubjectId(userService), GITHUB);

    // create idToken for a user with same providerType and email, and providerSubjectId-as-email
    idToken.setProviderType(user.getProviderType());
    idToken.setProviderSubjectId(user.getProviderSubjectId());
    idToken.setEmail(user.getEmail());
    idToken.setGivenName(user.getFirstName());
    idToken.setFamilyName("NewLastName");

    val response = getTokenResponse();

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
    assertEquals(updatedUser.getProviderSubjectId(), idToken.getProviderSubjectId());
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
    assertNotEquals(user.getLastName(), updatedUser.getLastName());

    // assert updatedUser and existingUser are the same
    assertEquals(updatedUser.getId(), existingUser.getId());
    assertEquals(updatedUser.getProviderType(), existingUser.getProviderType());
    assertEquals(updatedUser.getProviderSubjectId(), existingUser.getProviderSubjectId());
    assertEquals(updatedUser.getEmail(), existingUser.getEmail());
    assertEquals(updatedUser.getFirstName(), existingUser.getFirstName());
    assertEquals(updatedUser.getLastName(), existingUser.getLastName());
  }

  // existing provider(default)	existing id-as-email	  existing email	  user found,
  // update providerSubjectId OK
  @Test
  public void
      existingDefaultProviderTypeExistingProviderSubjectIdAsEmailExistingEmail_updateUser() {
    // setup for a migrated user with default providerType
    val migratedUser =
        entityGenerator.setupUser(
            "ExistingUser WithEmail",
            USER,
            "ExistingUserWithEmail@domain.com",
            defaultProviderType);

    // assert providerSubjectId matches email
    assertEquals(migratedUser.getEmail(), migratedUser.getProviderSubjectId());

    // create idToken for a user with same providerType and email, and valid providerSubjectId
    idToken.setProviderType(migratedUser.getProviderType());
    idToken.setEmail(migratedUser.getEmail());
    idToken.setGivenName(migratedUser.getFirstName());
    idToken.setFamilyName(migratedUser.getLastName());

    val response = getTokenResponse();

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
    assertEquals(updatedUser.getProviderSubjectId(), idToken.getProviderSubjectId());
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
    assertEquals(updatedUser.getProviderSubjectId(), existingUser.getProviderSubjectId());
    assertEquals(updatedUser.getEmail(), existingUser.getEmail());

    // assert migrated user and updatedUser are the same, but providerSubjectId was updated
    assertEquals(migratedUser.getId(), updatedUser.getId());
    assertNotEquals(migratedUser.getProviderSubjectId(), updatedUser.getProviderSubjectId());
  }

  @SneakyThrows
  private TestUserData generateUniqueTestUserData() {
    val users = repeatedCallsOf(() -> entityGenerator.generateRandomUser(), 2);

    return TestUserData.builder().users(users).build();
  }

  @lombok.Value
  @Builder
  public static class TestUserData {
    @NonNull List<User> users;
  }
}
