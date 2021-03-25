package bio.overture.ego.utils;

import static java.lang.String.format;
import static org.junit.Assert.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.enums.ProviderType;
import bio.overture.ego.model.exceptions.SSOAuthenticationFailureHandler;
import java.net.URISyntaxException;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration
public class SSOAuthenticationFailureHandlerTest {

  @Autowired private SSOAuthenticationFailureHandler ssoAuthenticationFailureHandler;
  @Autowired private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  // constants
  private String ERROR_TYPE_PARAM = "error_type";
  private String ERROR_CODE_PARAM = "error_code";
  private String PROVIDER_TYPE_PARAM = "provider_type";

  @Rule public ExpectedException exceptionRule = ExpectedException.none();

  @Before
  public void initTest() {
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .alwaysDo(print())
            .build();
  }

  @SneakyThrows
  @Test
  public void noPrimaryEmail_validProviderParam_createRedirectWithParams() {
    val app = appWithUrls("https://example-ego.com/redirect");
    val validProvider = ProviderType.GITHUB;
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setScheme("https");
    request.setServerName("www.example-ego.com");
    request.setRequestURI(String.format("/oauth/login/%s", validProvider.toString().toLowerCase()));
    URIBuilder uri = null;
    try {
      uri = new URIBuilder(app.getErrorRedirectUri());
      uri.addParameter(ERROR_CODE_PARAM, "403");
    } catch (URISyntaxException e) {
      assertEquals(
          format("Invalid redirect uri from application: '%s", app.getName()),
          URISyntaxException.class,
          e.getClass());
    }

    assertNotNull(uri);
    val errorUri = ssoAuthenticationFailureHandler.buildNoPrimaryExceptionResponse(request, uri);

    Map<String, String> errorParams =
        errorUri.getQueryParams().stream()
            .collect(Collectors.toImmutableMap(NameValuePair::getName, NameValuePair::getValue));

    assertTrue(errorParams.containsKey(ERROR_TYPE_PARAM));
    assertTrue(errorParams.containsKey(ERROR_CODE_PARAM));
    assertTrue(errorParams.containsKey(PROVIDER_TYPE_PARAM));
    assertEquals(errorParams.get(ERROR_TYPE_PARAM), "no_primary_email");
    assertEquals(errorParams.get(ERROR_CODE_PARAM), "403");
    assertEquals(errorParams.get(PROVIDER_TYPE_PARAM), validProvider.toString());
  }

  @SneakyThrows
  @Test
  public void noPrimaryEmail_invalidProvider_IllegalArgumentException() {
    val app = appWithUrls("https://example-ego.com/redirect");
    val invalidProvider = "foo";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setScheme("https");
    request.setServerName("www.example-ego.com");
    request.setRequestURI(String.format("/oauth/login/%s", invalidProvider));
    URIBuilder uri = null;
    try {
      uri = new URIBuilder(app.getErrorRedirectUri());
      uri.addParameter(ERROR_CODE_PARAM, "403");
    } catch (URISyntaxException e) {
      assertEquals(
          format("Invalid redirect uri from application: '%s", app.getName()),
          URISyntaxException.class,
          e.getClass());
    }

    assertNotNull(uri);
    exceptionRule.expect(IllegalArgumentException.class);
    ssoAuthenticationFailureHandler.buildNoPrimaryExceptionResponse(request, uri);
  }

  @SneakyThrows
  @Test
  public void oAuth2Exception_validErrorRedirect_createResponseWithParams() {
    val app = appWithUrls("https://example-ego.com/redirect");
    URIBuilder uri = null;

    try {
      uri = new URIBuilder(app.getErrorRedirectUri());
      uri.addParameter(ERROR_CODE_PARAM, "403");
    } catch (URISyntaxException e) {
      assertEquals(
          format("Invalid redirect uri from application: '%s", app.getName()),
          URISyntaxException.class,
          e.getClass());
    }
    assertNotNull(uri);

    val errorUri = ssoAuthenticationFailureHandler.buildOAuth2ExceptionResponse(uri);

    Map<String, String> errorParams =
        errorUri.getQueryParams().stream()
            .collect(Collectors.toImmutableMap(NameValuePair::getName, NameValuePair::getValue));

    assertTrue(errorParams.containsKey(ERROR_TYPE_PARAM));
    assertTrue(errorParams.containsKey(ERROR_CODE_PARAM));
    assertEquals(errorParams.get(ERROR_TYPE_PARAM), "access_denied");
    assertEquals(errorParams.get(ERROR_CODE_PARAM), "403");
  }

  @SneakyThrows
  @Test
  public void invalidErrorUri_URISyntaxException() {
    val app = appWithUrls("https://example-ego.com/error");
    URIBuilder uri = null;

    try {
      uri = new URIBuilder("=^URGH");
      uri.addParameter(ERROR_CODE_PARAM, "403");
    } catch (URISyntaxException e) {
      assertEquals(
          format("Invalid redirect uri from application: '%s", app.getName()),
          URISyntaxException.class,
          e.getClass());
    }
  }

  private static Application appWithUrls(String urls) {
    val app = new Application();
    app.setRedirectUri(urls);
    app.setErrorRedirectUri(urls);
    return app;
  }
}
