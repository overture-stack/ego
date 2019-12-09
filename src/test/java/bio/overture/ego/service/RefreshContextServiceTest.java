package bio.overture.ego.service;

import bio.overture.ego.model.domain.RefreshContext;
import bio.overture.ego.repository.RefreshTokenRepository;
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
import org.springframework.security.oauth2.common.exceptions.UnauthorizedClientException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

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

  @Test
  public void refreshTokenIsDeletedAfterUse() {
  }

  @Test
  public void userWithRefreshTokenIsUnique() {
    // create a refreshToken + assoc user
    // try to create another refreshToken and assoc with same user
    // Assert this should fail
  }

  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

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
    Assert.assertTrue(refreshContextService.createRefreshContext(incomingRefreshId, user1Token).getClass() == RefreshContext.class);
  }

}

//  cookie refreshId is NOT found in db -> 401 Unauthorized
//  cookie refreshId is found in db -> new refreshContext
// refresh token is deleted after use
// valid refreshContext creates new refreshToken
// valid refreshContext creates new accessToken???