package bio.overture.ego.model.domain;

import static bio.overture.ego.model.enums.StatusType.DISABLED;
import static bio.overture.ego.model.enums.StatusType.PENDING;
import static bio.overture.ego.model.enums.StatusType.REJECTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import bio.overture.ego.model.exceptions.ForbiddenException;
import bio.overture.ego.model.exceptions.UnauthorizedException;
import bio.overture.ego.repository.RefreshTokenRepository;
import bio.overture.ego.service.RefreshContextService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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

  @Autowired RefreshContextService refreshContextService;
  @Autowired EntityGenerator entityGenerator;
  @Autowired TokenService tokenService;
  @Autowired RefreshTokenRepository refreshTokenRepository;
  @Autowired UserService userService;

  @Rule public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void validate_tokenIsExpired_Unauthorized() {
    val user1 = entityGenerator.setupUser("User One");
    val user1Token = tokenService.generateUserToken(user1);
    val refreshToken1 = refreshContextService.createRefreshToken(user1Token);

    val mockExpiry = Instant.now().minus(100, ChronoUnit.MILLIS);
    refreshToken1.setExpiryDate(Date.from(mockExpiry));

    val storedRefreshToken = refreshContextService.getById(refreshToken1.getId());
    val incomingRefreshContext =
        refreshContextService.createRefreshContext(refreshToken1.getId().toString(), user1Token);
    assertTrue(storedRefreshToken.getSecondsUntilExpiry() == 0);

    exceptionRule.expect(UnauthorizedException.class);
    exceptionRule.expectMessage(
        String.format("RefreshToken %s is expired", storedRefreshToken.getId()));
    incomingRefreshContext.validate();
  }

  @Test
  public void validate_tokenIsNotExpired_ValidRefreshContext() {
    val user1 = entityGenerator.setupUser("User One");
    val user1Token = tokenService.generateUserToken(user1);
    val refreshToken1 = refreshContextService.createRefreshToken(user1Token);

    // mock incoming refreshId to match what we have stored. This would normally come from request
    // cookie
    val incomingRefreshId = refreshToken1.getId();
    val incomingRefreshContext =
        refreshContextService.createRefreshContext(incomingRefreshId.toString(), user1Token);

    assertTrue(refreshToken1.getSecondsUntilExpiry() > 0);
    assertTrue(incomingRefreshContext.validate());
  }

  @Test
  public void validate_UserApprovedStatus_ValidRefreshContext() {
    val user1 = entityGenerator.setupUser("User One");

    val user1Token = tokenService.generateUserToken(user1);

    val refreshToken1 = refreshContextService.createRefreshToken(user1Token);

    val refreshContext1 =
        refreshContextService.createRefreshContext(refreshToken1.getId().toString(), user1Token);

    assertTrue(refreshContext1.hasApprovedUser());
    assertTrue(refreshContext1.validate());
  }

  @Test
  public void validate_UserNonApprovedStatus_ForbiddenException() {
    val disabledUser = entityGenerator.setupUser("Disabled User");
    disabledUser.setStatus(DISABLED);
    val rejectedUser = entityGenerator.setupUser("Rejected User");
    rejectedUser.setStatus(REJECTED);
    val pendingUser = entityGenerator.setupUser("Pending User");
    pendingUser.setStatus(PENDING);

    val disabledUserToken = tokenService.generateUserToken(disabledUser);
    val rejectedUserToken = tokenService.generateUserToken(rejectedUser);
    val pendingUserToken = tokenService.generateUserToken(pendingUser);

    val disabledUserRefreshToken = refreshContextService.createRefreshToken(disabledUserToken);
    val rejectedUserRefreshToken = refreshContextService.createRefreshToken(rejectedUserToken);
    val pendingUserRefreshToken = refreshContextService.createRefreshToken(pendingUserToken);

    val disabledUserRefreshContext =
        refreshContextService.createRefreshContext(
            disabledUserRefreshToken.getId().toString(), disabledUserToken);
    val rejectedUserRefreshContext =
        refreshContextService.createRefreshContext(
            rejectedUserRefreshToken.getId().toString(), rejectedUserToken);
    val pendingUserRefreshContext =
        refreshContextService.createRefreshContext(
            pendingUserRefreshToken.getId().toString(), pendingUserToken);

    assertFalse(disabledUserRefreshContext.hasApprovedUser());
    assertFalse(rejectedUserRefreshContext.hasApprovedUser());
    assertFalse(pendingUserRefreshContext.hasApprovedUser());

    exceptionRule.expect(ForbiddenException.class);
    exceptionRule.expectMessage("User does not have approved status, rejecting.");
    disabledUserRefreshContext.validate();
    rejectedUserRefreshContext.validate();
    pendingUserRefreshContext.validate();
  }

  @Test
  public void validate_jwtClaimsMatchRefreshToken_validRefreshContext() {
    val user1 = entityGenerator.setupUser("User One");
    val user1Token = tokenService.generateUserToken(user1);
    val refreshToken1 = refreshContextService.createRefreshToken(user1Token);

    val storedRefreshToken = refreshContextService.getById(refreshToken1.getId());
    val incomingRefreshContext =
        refreshContextService.createRefreshContext(
            storedRefreshToken.getId().toString(), user1Token);
    val incomingTokenClaims = tokenService.getTokenClaims(user1Token);

    assertEquals(storedRefreshToken.getUser().getId().toString(), incomingTokenClaims.getSubject());
    assertEquals(storedRefreshToken.getJti().toString(), incomingTokenClaims.getId());
    assertTrue(incomingRefreshContext.validate());
  }

  @Test
  public void validate_jtiDoesNotMatchRefreshTokenJti_ForbiddenException() {
    val user1 = entityGenerator.setupUser("User One");
    val user1Token = tokenService.generateUserToken(user1);

    // this generates a refreshToken with a random jti
    val refreshToken1 = entityGenerator.generateRandomRefreshToken(43200000);
    refreshToken1.associateWithUser(user1);
    refreshTokenRepository.save(refreshToken1);

    // insert into db
    refreshContextService.getById(refreshToken1.getId());
    val incomingRefreshContext =
        refreshContextService.createRefreshContext(refreshToken1.getId().toString(), user1Token);
    val incomingJti = tokenService.getTokenClaims(user1Token).getId();
    assertNotEquals(refreshToken1.getJti().toString(), incomingJti);

    exceptionRule.expect(ForbiddenException.class);
    exceptionRule.expectMessage(
        String.format("Invalid token claims for refreshId %s.", refreshToken1.getId()));
    incomingRefreshContext.validate();
  }

  @Test
  public void validate_jwtSubDoesNotMatchRefreshTokenUser_ForbiddenException() {
    val user1 = entityGenerator.setupUser("User One");
    val user2 = entityGenerator.setupUser("User Two");

    val user1Token = tokenService.generateUserToken(user1);
    val user2Token = tokenService.generateUserToken(user2);

    val refreshToken1 = refreshContextService.createRefreshToken(user1Token);

    val incomingRefreshContext =
        refreshContextService.createRefreshContext(refreshToken1.getId().toString(), user2Token);
    val incomingUserClaims = tokenService.getTokenClaims(user2Token);
    assertNotEquals(refreshToken1.getUser().getId().toString(), incomingUserClaims.getSubject());

    exceptionRule.expect(ForbiddenException.class);
    exceptionRule.expectMessage(
        String.format("Invalid token claims for refreshId %s.", refreshToken1.getId()));
    incomingRefreshContext.validate();
  }
}
