package bio.overture.ego.controller;

import static bio.overture.ego.model.enums.JavaFields.REFRESH_ID;
import static java.util.Objects.isNull;
import static org.junit.Assert.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.domain.RefreshContext;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.service.RefreshContextService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.web.StringResponseOption;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@ActiveProfiles({"test", "auth"})
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RefreshTokenTest extends AbstractControllerTest {

  @Autowired private EntityGenerator entityGenerator;

  @Autowired private TokenService tokenService;

  @Autowired private RefreshContextService refreshContextService;

  @Rule public ExpectedException exceptionRule = ExpectedException.none();

  private HttpHeaders tokenHeaders = new HttpHeaders();
  private User user;
  private RefreshContext refreshContext;

  @Value("${logging.test.controller.enable}")
  private boolean enableLogging;

  @Override
  protected boolean enableLogging() {
    return enableLogging;
  }

  @Override
  protected void beforeTest() {
    val adminUser = entityGenerator.setupUser("Admin Refreshtoken");
    val bearerToken = tokenService.generateUserToken(adminUser);
    tokenHeaders.add(AUTHORIZATION, "Bearer " + bearerToken);
    tokenHeaders.setContentType(APPLICATION_JSON);

    user = tokenService.getTokenUserInfo(bearerToken);
    refreshContext = refreshContextService.createInitialRefreshContext(bearerToken);
  }

  @Test
  public void refresh_validRefreshToken_refreshCookieAdded() {
    val refreshToken = refreshContext.getRefreshToken();
    val response = createRefreshTokenEndpointAnd(refreshToken.getId().toString(), tokenHeaders);

    val statusCode = response.getResponse().getStatusCode();
    assertEquals(statusCode, OK);
    val newHeaders = response.getResponse().getHeaders();

    val newCookie = newHeaders.get("Set-Cookie");
    assertNotNull(newCookie);
    assertTrue(newCookie.get(0).contains("refreshId="));
    response.assertHasBody();

    exceptionRule.expect(NotFoundException.class);
    exceptionRule.expectMessage(
        String.format("RefreshToken '%s' does not exist", refreshToken.getId()));
    refreshContextService.get(refreshToken.getId(), false);
  }

  @Test
  public void refresh_invalidRefreshToken_NotFound() {
    val refreshToken = refreshContext.getRefreshToken();
    val invalidRefreshToken = UUID.randomUUID().toString();

    assertNotEquals(refreshToken, invalidRefreshToken);
    val response = createRefreshTokenEndpointAnd(invalidRefreshToken, tokenHeaders);
    val statusCode = response.getResponse().getStatusCode();

    assertEquals(statusCode, NOT_FOUND);
    assertNoRefreshIdCookie(response);
  }

  @Test
  public void refresh_missingRefreshToken_Unauthorized() {
    val response = initStringRequest().endpoint("/oauth/refresh").headers(tokenHeaders).postAnd();
    val statusCode = response.getResponse().getStatusCode();

    assertEquals(statusCode, UNAUTHORIZED);
    assertNoRefreshIdCookie(response);
  }

  @Test
  public void refresh_invalidBearerTokenClaims_Forbidden() {
    val invalidClaimsUser = entityGenerator.setupUser("Invalid Bearer");
    val invalidBearerToken = tokenService.generateUserToken(invalidClaimsUser);

    tokenHeaders.clear();
    tokenHeaders.add(AUTHORIZATION, "Bearer " + invalidBearerToken);
    tokenHeaders.setContentType(APPLICATION_JSON);
    val refreshToken = refreshContext.getRefreshToken();
    val response = createRefreshTokenEndpointAnd(refreshToken.getId().toString(), tokenHeaders);

    val statusCode = response.getResponse().getStatusCode();

    assertEquals(statusCode, FORBIDDEN);
    assertNoRefreshIdCookie(response);
  }

  @Test
  public void deleteRefresh_validRefreshToken_Success() {
    val refreshToken = refreshContext.getRefreshToken();
    val response = deleteRefreshTokenEndpointAnd(refreshToken.getId().toString(), tokenHeaders);
    val responseStatus = response.getResponse().getStatusCode();

    assertEquals(responseStatus, OK);

    response.assertHasBody();
    val responseBody = response.getResponse().getBody();
    assertEquals(responseBody, "User is logged out");

    val newCookie = response.getResponse().getHeaders().get("Set-Cookie");
    val refreshCookie = newCookie.get(0);
    assertNotNull(refreshCookie);
    assertTrue(refreshCookie.contains("refreshId=; Max-Age=0;"));

    exceptionRule.expect(NotFoundException.class);
    exceptionRule.expectMessage(
        String.format("RefreshToken '%s' does not exist", refreshToken.getId()));
    refreshContextService.get(refreshToken.getId(), false);
  }

  @Test
  public void deleteRefresh_invalidRefreshToken_NotFound() {
    val invalidRefreshToken = UUID.randomUUID().toString();
    val response = deleteRefreshTokenEndpointAnd(invalidRefreshToken, tokenHeaders);

    val responseStatus = response.getResponse().getStatusCode();
    assertEquals(responseStatus, NOT_FOUND);

    assertNoRefreshIdCookie(response);
  }

  @Test
  public void deleteRefresh_missingRefreshToken_Unauthorized() {
    val response = initStringRequest().endpoint("/oauth/refresh").headers(tokenHeaders).deleteAnd();
    val statusCode = response.getResponse().getStatusCode();

    assertEquals(statusCode, UNAUTHORIZED);
    assertNoRefreshIdCookie(response);
  }

  private void assertNoRefreshIdCookie(StringResponseOption response) {
    val cookies = response.getResponse().getHeaders().get("Set-Cookie");
    if (isNull(cookies)) {
      return;
    }
    Objects.requireNonNull(cookies)
        .forEach(
            c -> {
              assertFalse(c.contains(REFRESH_ID));
            });
  }
}
