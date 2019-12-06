package bio.overture.ego.model.domain;

import bio.overture.ego.model.enums.StatusType;
import bio.overture.ego.model.exceptions.ForbiddenException;
import bio.overture.ego.service.RefreshContextService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.utils.EntityGenerator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
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
public class RefreshContextTest {

  private static boolean hasRunEntitySetup = false;

  @Autowired RefreshContextService refreshContextService;
  @Autowired EntityGenerator entityGenerator;
  @Autowired TokenService tokenService;

  @Test
  public void expiredRefreshTokenIsRejected() {

  }

  @Test
  public void testExpiredRefreshTokenDoesNotRenew() {}

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void validate_UserApprovedStatus_ValidRefreshContext() {
    val user1 = entityGenerator.setupUser("User One");

    val user1Token = tokenService.generateUserToken(user1);

    val refreshToken1 = refreshContextService.createRefreshToken(user1Token);

    val refreshContext1 = refreshContextService.createRefreshContext(refreshToken1.getId().toString(), user1Token);

    Assert.assertTrue(refreshContext1.hasApprovedUser());
    Assert.assertTrue(refreshContext1.validate());
  }

  @Test
  public void validate_UserNonApprovedStatus_ForbiddenException() {
    val user1 = entityGenerator.setupUser("User One");
    user1.setStatus(StatusType.DISABLED);

    val user1Token = tokenService.generateUserToken(user1);
    val refreshToken1 = refreshContextService.createRefreshToken(user1Token);

    val refreshContext1 = refreshContextService.createRefreshContext(refreshToken1.getId().toString(), user1Token);

    Assert.assertFalse(refreshContext1.hasApprovedUser());

    exceptionRule.expect(ForbiddenException.class);
    exceptionRule.expectMessage("User does not have approved status, rejecting.");
    refreshContext1.validate();
  }

  // validRefreshTokenReturnsNewAccessAndRefreshTokens
  // expiredRefreshTokenDoesNotReturnNewAccessAndRefreshTokens
  // mismatchedClaimsDoNotValidateRefreshContext
  // cookieMissingRefreshTokenDoesNotValidate
  // mismatchedClientIdDoesNotValidate
  // mismatchedRefreshTokenDoesNotValidate

}

// user id matches
// claims -> jti matches
// refresh expiry is valid
// client id matches. client id passed in request
// user status is approved

// set up different user types to test
// return validContext: boolean
// validations:
// jwt user + jti + refresh token id (tokenClaims + cookie) matches db row
// refresh token is not expired
// client id passed as query parameter to help verify cookie
// check for scope changes
