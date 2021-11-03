package bio.overture.ego.security;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.TokenService;
import io.jsonwebtoken.Claims;
import java.util.*;
import lombok.val;
import org.junit.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

public class SecurityTest {
  @Test
  public void testDecodeBasicToken() {
    val token = "Basic aWQ6c2VjcmV0";
    val contents = BasicAuthToken.decode(token);
    assertTrue(contents.isPresent());
    assertEquals("id", contents.get().getClientId());
    assertEquals("secret", contents.get().getClientSecret());
  }

  private JWTAuthorizationFilter getAuthorizationFilter(
      ApplicationService applicationService, TokenService tokenService) {
    val authManager =  mock(AuthenticationManager.class);

    val app = new Application();
    app.setClientId("id");
    app.setClientSecret("secret");
    when(applicationService.findByClientId2("id")).thenReturn(Optional.of(app));

    val authorizationFilter = new JWTAuthorizationFilter(new String[0], tokenService, applicationService);
    ReflectionTestUtils.setField(authorizationFilter, "applicationService", applicationService);
    ReflectionTestUtils.setField(authorizationFilter, "tokenService", tokenService);
    return authorizationFilter;
  }

  @Test
  public void testAuthenticateApplication() {
    val tokenService = mock(TokenService.class);
    val applicationService = mock(ApplicationService.class);

    val app = new Application();
    app.setClientId("id");
    app.setClientSecret("secret");
    when(applicationService.findByClientId2("id")).thenReturn(Optional.of(app));

    val authorizationFilter = getAuthorizationFilter(applicationService, tokenService);
    ReflectionTestUtils.setField(authorizationFilter, "applicationService", applicationService);
    ReflectionTestUtils.setField(authorizationFilter, "tokenService", tokenService);

    val token = "Basic aWQ6c2VjcmV0"; // client id="id", client secret="secret"
    authorizationFilter.authenticateApplication(token);
    val result = SecurityContextHolder.getContext().getAuthentication();
    assertTrue("right id & password", result.isAuthenticated());

    val app2 = new Application();
    app2.setClientId("id");
    app2.setClientSecret("wrong");
    when(applicationService.findByClientId2("id")).thenReturn(Optional.of(app2));
    authorizationFilter.authenticateApplication(token);
    val result2 = SecurityContextHolder.getContext().getAuthentication();
    assertNull("wrong password", result2);

    when(applicationService.findByClientId2("id")).thenReturn(Optional.empty());

    app.setClientSecret("secret");
    authorizationFilter.authenticateApplication(token);
    val result3 = SecurityContextHolder.getContext().getAuthentication();
    assertNull("Bad application id", result3);
  }

  @Test
  public void testAuthenticateUserOrApplication() {
    val applicationService = mock(ApplicationService.class);
    val tokenService = mock(TokenService.class);
    val claims = mock(Claims.class);

    String token = "Bearer xxxx";

    when(tokenService.isValidToken("xxxx")).thenReturn(true);
    when(tokenService.getTokenClaims("xxxx")).thenReturn(claims);

    val filter = getAuthorizationFilter(applicationService, tokenService);

    filter.authenticateUserOrApplication(token);
    val result = SecurityContextHolder.getContext().getAuthentication();
    assertTrue("Passed authentication", result.isAuthenticated());

    when(tokenService.isValidToken("xxxx")).thenReturn(false);
    filter.authenticateUserOrApplication(token);
    val result2 = SecurityContextHolder.getContext().getAuthentication();
    assertNull("Invalid token means no access", result2);
  }
}
