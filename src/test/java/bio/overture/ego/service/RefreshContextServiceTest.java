package bio.overture.ego.service;

import bio.overture.ego.model.domain.RefreshContext;
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
import org.springframework.security.oauth2.common.exceptions.UnauthorizedClientException;
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
  public void refresh_usedTokenIsDeleted() {
    val user1 = entityGenerator.setupUserWithRefreshToken("User One");
    val user1Token = tokenService.generateUserToken(user1);
    val refreshToken1 = user1.getRefreshToken();
    userService.get(user1.getId(), false, false, false, true);

    Assert.assertTrue(refreshContextService.getById(refreshToken1.getId()) != null);
    refreshContextService.disassociateUserAndDelete(user1Token);

    exceptionRule.expect(NotFoundException.class);
    exceptionRule.expectMessage(String.format("RefreshToken '%s' does not exist", refreshToken1.getId()));
    Assert.assertTrue(refreshContextService.get(refreshToken1.getId(), false) == refreshToken1);
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
    exceptionRule.expectMessage(String.format("A refresh token already exists for %s", user1.getId()));
    refreshContextService.createRefreshToken(user1Token);
  }

  //  cookie refreshId is NOT found in db -> 401 Unauthorized
  @Test
  public void createContext_incomingRefreshIdDoesNotMatch_Unauthorized() {
    // if you get a not found with the incoming refreshId, return 401
    val user1 = entityGenerator.setupUser("User One");
    val storedRefreshToken = entityGenerator.generateRandomRefreshToken(43200000);
    val user1Token = tokenService.generateUserToken(user1);

    storedRefreshToken.associateWithUser(user1);
    refreshTokenRepository.save(storedRefreshToken);

    val incomingRefreshId = UUID.randomUUID();
    val incomingRefreshIdAsString = incomingRefreshId.toString();

    Assert.assertNotEquals(storedRefreshToken.getId(), incomingRefreshId);
    Assert.assertTrue(refreshTokenRepository.findById(incomingRefreshId).isEmpty());

    exceptionRule.expect(UnauthorizedClientException.class);
    exceptionRule.expectMessage(String.format("RefreshToken %s is not found.", incomingRefreshId));
    refreshContextService.createRefreshContext(incomingRefreshIdAsString, user1Token);
  }

  //  cookie refreshId is found in db -> new refreshContext
  @Test
  public void createContext_incomingRefreshIdDoesMatch_newContext() {
    val user1 = entityGenerator.setupUser("User One");
    val storedRefreshToken = entityGenerator.generateRandomRefreshToken(43200000);
    val user1Token = tokenService.generateUserToken(user1);

    storedRefreshToken.associateWithUser(user1);
    refreshTokenRepository.save(storedRefreshToken);

    val incomingRefreshId = storedRefreshToken.getId().toString();

    Assert.assertEquals(storedRefreshToken.getId(), UUID.fromString(incomingRefreshId));
    Assert.assertTrue(
        refreshContextService.createRefreshContext(incomingRefreshId, user1Token).getClass()
            == RefreshContext.class);
  }
}

//  cookie refreshId is NOT found in db -> 401 Unauthorized
//  cookie refreshId is found in db -> new refreshContext
// refresh token is deleted after use
// valid refreshContext creates new refreshToken
// valid refreshContext creates new accessToken???
