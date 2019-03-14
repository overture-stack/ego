package bio.overture.ego.token;

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.TestData;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static bio.overture.ego.model.enums.UserType.ADMIN;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@Transactional
@ActiveProfiles("test")
@Ignore
public class RevokeTokenTest {

  public static TestData test = null;
  @Autowired private EntityGenerator entityGenerator;
  @Autowired private TokenService tokenService;

  @Rule public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    test = new TestData(entityGenerator);
  }

  @Test
  public void adminRevokeAnyToken() {
    // Admin users can revoke any tokens.
    val adminTokenString = "791044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val scopes = test.getScopes("song.WRITE", "id.WRITE");
    val applications = new HashSet<Application>();
    applications.add(test.score);

    entityGenerator.setupToken(test.user1, adminTokenString, 1000, scopes, applications);
    test.user1.setType(ADMIN);
    test.user1.setStatus(APPROVED);

    val randomTokenString = "891044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val randomToken =
        entityGenerator.setupToken(test.regularUser, randomTokenString, 1000, scopes, applications);

    // make sure before revoking, randomToken is not revoked.
    assertFalse(randomToken.isRevoked());

    tokenService.revokeToken(randomTokenString);

    assertTrue(randomToken.isRevoked());
  }

  @Test
  public void adminRevokeOwnToken() {
    // If an admin users tries to revoke her own token, the token should be revoked.
    val tokenString = "791044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val scopes = test.getScopes("song.WRITE", "id.WRITE");
    val applications = new HashSet<Application>();
    applications.add(test.score);

    val adminToken =
        entityGenerator.setupToken(test.user1, tokenString, 1000, scopes, applications);
    test.user1.setType(ADMIN);
    test.user1.setStatus(APPROVED);

    assertFalse(adminToken.isRevoked());

    tokenService.revokeToken(tokenString);

    val revokedToken = tokenService.findByTokenString(tokenString);

    assertTrue(revokedToken.get().isRevoked());
  }

  @Test
  public void userRevokeOwnToken() {
    // If a non-admin user tries to revoke her own token, the token will be revoked.
    val tokenString = "791044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val scopes = test.getScopes("song.WRITE", "id.WRITE");
    val applications = new HashSet<Application>();
    applications.add(test.score);

    val userToken =
        entityGenerator.setupToken(test.regularUser, tokenString, 1000, scopes, applications);

    assertFalse(userToken.isRevoked());

    tokenService.revokeToken(tokenString);

    assertTrue(userToken.isRevoked());
  }

  @Test
  public void userRevokeAnyToken() {
    // If a non-admin user tries to revoke a token that does not belong to her,
    // the token won't be revoked. Expect an InvalidTokenException.
    val tokenString = "791044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val scopes = test.getScopes("song.WRITE", "id.WRITE");
    val applications = new HashSet<Application>();
    applications.add(test.score);

    entityGenerator.setupToken(test.regularUser, tokenString, 1000, scopes, applications);

    val randomTokenString = "891044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val randomToken =
        entityGenerator.setupToken(test.user1, randomTokenString, 1000, scopes, applications);

    assertFalse(randomToken.isRevoked());

    exception.expect(InvalidTokenException.class);
    exception.expectMessage("Users can only revoke tokens that belong to them.");

    tokenService.revokeToken(randomTokenString);
  }
}
