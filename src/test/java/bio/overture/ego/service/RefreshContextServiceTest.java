package bio.overture.ego.service;

import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static bio.overture.ego.model.enums.StatusType.PENDING;

import bio.overture.ego.model.domain.RefreshContext;
import bio.overture.ego.model.exceptions.ForbiddenException;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.model.exceptions.UniqueViolationException;
import bio.overture.ego.repository.RefreshTokenRepository;
import bio.overture.ego.utils.EntityGenerator;
import java.util.UUID;
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
public class RefreshContextServiceTest {

  @Autowired RefreshTokenRepository refreshTokenRepository;
  @Autowired TokenService tokenService;
  @Autowired EntityGenerator entityGenerator;
  @Autowired RefreshContextService refreshContextService;
  @Autowired UserService userService;

  @Rule public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void refresh_invalidContext_usedTokenIsDeleted() {
    val user1 = entityGenerator.setupUserWithRefreshToken("User One");
    // force invalid refreshContext with invalid user status
    user1.setStatus(PENDING);
    val user1Token = tokenService.generateUserToken(user1);
    val refreshToken1 = user1.getRefreshToken();

    Assert.assertTrue(refreshContextService.findById(refreshToken1.getId()).isPresent());
    Assert.assertTrue(refreshContextService.getById(refreshToken1.getId()) != null);

    val incomingRefreshContext =
        refreshContextService.createRefreshContext(refreshToken1.getId().toString(), user1Token);
    refreshContextService.disassociateUserAndDelete(user1Token);

    exceptionRule.expect(NotFoundException.class);
    exceptionRule.expectMessage(
        String.format("RefreshToken '%s' does not exist", refreshToken1.getId()));
    Assert.assertEquals(refreshContextService.get(refreshToken1.getId(), true), refreshToken1);
    refreshContextService.get(refreshToken1.getId(), false);

    exceptionRule.expect(ForbiddenException.class);
    exceptionRule.expectMessage("User does not have approved status, rejecting.");
    incomingRefreshContext.validate();
  }

  @Test
  public void refresh_validContext_usedTokenIsDeleted() {
    val user1 = entityGenerator.setupUserWithRefreshToken("User One");
    val user1Token = tokenService.generateUserToken(user1);
    val refreshToken1 = user1.getRefreshToken();

    Assert.assertTrue(refreshContextService.findById(refreshToken1.getId()).isPresent());
    Assert.assertTrue(refreshContextService.getById(refreshToken1.getId()) != null);
    refreshContextService.disassociateUserAndDelete(user1Token);

    exceptionRule.expect(NotFoundException.class);
    exceptionRule.expectMessage(
        String.format("RefreshToken '%s' does not exist", refreshToken1.getId()));
    Assert.assertEquals(refreshContextService.get(refreshToken1.getId(), true), refreshToken1);
    refreshContextService.get(refreshToken1.getId(), false);
  }

  @Test
  public void userWithRefreshTokenIsUnique() {
    val user1 = entityGenerator.setupUser("User One");
    val user2 = entityGenerator.setupUser("User Two");
    val user1Token = tokenService.generateUserToken(user1);
    val user2Token = tokenService.generateUserToken(user2);

    val refreshToken1 = refreshContextService.createRefreshToken(user1Token);
    Assert.assertNotNull(refreshContextService.get(refreshToken1.getId(), true));
    Assert.assertEquals(refreshToken1, refreshContextService.get(refreshToken1.getId(), true));
    exceptionRule.expect(UniqueViolationException.class);
    exceptionRule.expectMessage(
        String.format("A refresh token already exists for %s", user1.getId()));
    refreshContextService.createRefreshToken(user1Token);
  }

  @Test
  public void createContext_incomingRefreshIdDoesNotMatch_NotFound() {
    val user1 = entityGenerator.setupUser("User One");
    val storedRefreshToken = entityGenerator.generateRandomRefreshToken(43200000);
    val user1Token = tokenService.generateUserToken(user1);

    storedRefreshToken.associateWithUser(user1);
    refreshTokenRepository.save(storedRefreshToken);

    val incomingRefreshId = UUID.randomUUID();
    val incomingRefreshIdAsString = incomingRefreshId.toString();

    Assert.assertNotEquals(storedRefreshToken.getId(), incomingRefreshId);
    Assert.assertTrue(refreshTokenRepository.findById(incomingRefreshId).isEmpty());

    exceptionRule.expect(NotFoundException.class);
    exceptionRule.expectMessage(String.format("RefreshToken %s is not found.", incomingRefreshId));
    refreshContextService.createRefreshContext(incomingRefreshIdAsString, user1Token);
  }

  @Test
  public void createContext_incomingRefreshIdDoesMatch_newContext() {
    val user1 = entityGenerator.setupUser("User One");
    val storedRefreshToken = entityGenerator.generateRandomRefreshToken(43200000);
    val user1Token = tokenService.generateUserToken(user1);

    storedRefreshToken.associateWithUser(user1);
    refreshTokenRepository.save(storedRefreshToken);

    val incomingRefreshId = storedRefreshToken.getId().toString();

    Assert.assertEquals(storedRefreshToken.getId(), UUID.fromString(incomingRefreshId));
    Assert.assertEquals(
        refreshContextService.createRefreshContext(incomingRefreshId, user1Token).getClass(),
        RefreshContext.class);
  }

  @Test
  public void validateAndReturnToken_mismatchedRefreshToken_NotFound() {
    val incomingUser = entityGenerator.setupUserWithRefreshToken("User One");
    val incomingUserToken = tokenService.generateUserToken(incomingUser);

    val mockIncomingUserRefreshId = UUID.randomUUID();

    exceptionRule.expect(NotFoundException.class);
    exceptionRule.expectMessage(
        String.format("RefreshToken %s is not found.", mockIncomingUserRefreshId));
    refreshContextService.validateAndReturnNewUserToken(
        mockIncomingUserRefreshId.toString(), incomingUserToken);
    Assert.assertFalse(
        refreshContextService.findById(incomingUser.getRefreshToken().getId()).isPresent());
  }

  @Test
  public void validateAndReturn_invalidClaims_Forbidden() {
    val incomingUser = entityGenerator.setupUserWithRefreshToken("Incoming User");
    val incomingUserToken = tokenService.generateUserToken(incomingUser);

    tokenService.getTokenClaims(incomingUserToken).setId(UUID.randomUUID().toString());
    exceptionRule.expect(ForbiddenException.class);
    exceptionRule.expectMessage(
        String.format(
            "Invalid token claims for refreshId %s.", incomingUser.getRefreshToken().getId()));
    refreshContextService.validateAndReturnNewUserToken(
        incomingUser.getRefreshToken().getId().toString(), incomingUserToken);
    Assert.assertNull(incomingUser.getRefreshToken());
    Assert.assertFalse(
        refreshContextService.findById(incomingUser.getRefreshToken().getId()).isPresent());
  }

  @Test
  public void validateAndReturnToken_validClaims_newAccessToken() {
    val incomingUser = entityGenerator.setupUser("User One");
    val incomingUserToken = tokenService.generateUserToken(incomingUser);
    val incomingRefreshToken = refreshContextService.createRefreshToken(incomingUserToken);
    val incomingRefreshId = incomingRefreshToken.getId();

    val incomingClaims = tokenService.getTokenClaims(incomingUserToken);

    val outgoingUserToken =
        refreshContextService.validateAndReturnNewUserToken(
            incomingRefreshId.toString(), incomingUserToken);
    Assert.assertFalse(refreshContextService.findById(incomingRefreshId).isPresent());
    val outgoingUser = tokenService.getTokenUserInfo(outgoingUserToken);
    val outgoingUserClaims = tokenService.getTokenClaims(outgoingUserToken);

    Assert.assertEquals(incomingUser, outgoingUser);
    Assert.assertEquals(incomingClaims.get("context"), outgoingUserClaims.get("context"));
    Assert.assertNotEquals(incomingClaims, outgoingUserClaims);
    Assert.assertNotEquals(incomingClaims.getId(), outgoingUserClaims.getId());
  }

  @Test
  public void createNewRefreshContext_existingRefreshToken_validContext() {
    val user1 = entityGenerator.setupUserWithRefreshToken("User One");
    val user1Token = tokenService.generateUserToken(user1);
    val existingRefreshId = user1.getRefreshToken().getId();

    Assert.assertTrue(refreshContextService.findById(user1.getRefreshToken().getId()).isPresent());
    val user1Context = refreshContextService.createInitialRefreshContext(user1Token);

    Assert.assertTrue(user1Context.getClass().equals(RefreshContext.class));
    Assert.assertTrue(user1Context.validate());
    Assert.assertNotEquals(existingRefreshId, user1Context.getRefreshToken().getId());
  }

  @Test
  public void createInitialRefreshContext_noExistingRefreshToken_validContext() {
    val user1 = entityGenerator.setupUser("User One");
    val user1Token = tokenService.generateUserToken(user1);

    Assert.assertNull(user1.getRefreshToken());
    val user1Context = refreshContextService.createInitialRefreshContext(user1Token);

    Assert.assertNotNull(user1.getRefreshToken());
    Assert.assertTrue(refreshContextService.findById(user1.getRefreshToken().getId()).isPresent());
    Assert.assertTrue(user1Context.getClass().equals(RefreshContext.class));
    Assert.assertTrue(user1Context.validate());
  }

  @Test
  public void createInitialRefreshContext_nonApprovedUser_Forbidden() {
    val pendingUser = entityGenerator.setupUser("User One");
    pendingUser.setStatus(PENDING);
    val pendingUserToken = tokenService.generateUserToken(pendingUser);

    Assert.assertFalse(pendingUser.getStatus() == APPROVED);

    exceptionRule.expect(ForbiddenException.class);
    exceptionRule.expectMessage("User does not have approved status, rejecting.");
    refreshContextService.createInitialRefreshContext(pendingUserToken);
  }

  @Test
  public void createInitialRefreshContext_approvedUser_validContext() {
    val approvedUser = entityGenerator.setupUser("User One");
    val approvedUserToken = tokenService.generateUserToken(approvedUser);

    Assert.assertTrue(approvedUser.getStatus() == APPROVED);

    val approvedUserContext = refreshContextService.createInitialRefreshContext(approvedUserToken);
    Assert.assertEquals(approvedUserContext.getClass(), RefreshContext.class);
    Assert.assertTrue(approvedUserContext.validate());
  }
}
