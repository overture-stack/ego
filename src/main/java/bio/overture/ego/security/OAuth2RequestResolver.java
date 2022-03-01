package bio.overture.ego.security;

import java.net.URI;
import javax.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Custom request resolver to capture request info before sending it to oauth2 providers and store
 * them in the current request session
 *
 * <p>intended to replace {@see OAuth2ClientResources}
 */
public class OAuth2RequestResolver implements OAuth2AuthorizationRequestResolver {
  private final AntPathRequestMatcher authorizationRequestMatcher;
  private DefaultOAuth2AuthorizationRequestResolver resolver;
  private static final String REGISTRATION_ID_URI_VARIABLE_NAME = "registrationId";
  public OAuth2RequestResolver(
      ClientRegistrationRepository clientRegistrationRepository,
      String authorizationRequestBaseUri) {
    this.resolver =
        new DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository, authorizationRequestBaseUri);
    this.authorizationRequestMatcher = new AntPathRequestMatcher(
        authorizationRequestBaseUri + "/{" + REGISTRATION_ID_URI_VARIABLE_NAME + "}");
  }

  @SneakyThrows
  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
    // check if the request is an oauth2 login request first
    String registrationId = this.resolveRegistrationId(request);
    if (registrationId == null) {
      return this.resolver.resolve(request);
    }
    val uri = new URI(request.getRequestURI() + "?" + request.getQueryString());
    val attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    val session = attr.getRequest().getSession(true);

    val uriComponents = UriComponentsBuilder.fromUri(uri).build();
    val queryParams = uriComponents.getQueryParams();

    val clientId = queryParams.getFirst("client_id");
    if (StringUtils.hasText(clientId)) {
      session.setAttribute("ego_client_id", clientId);
    }
    val redirectUri = queryParams.getFirst("redirect_uri");
    if (StringUtils.hasText(redirectUri)) {
      session.setAttribute("ego_redirect_uri", redirectUri);
    } else {
      session.setAttribute("ego_redirect_uri", "");
    }
    return this.resolver.resolve(request);
  }

  @SneakyThrows
  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
    return this.resolve(request, registrationId);
  }

  private String resolveRegistrationId(HttpServletRequest request) {
    if (this.authorizationRequestMatcher.matches(request)) {
      return this.authorizationRequestMatcher.matcher(request).getVariables()
          .get(REGISTRATION_ID_URI_VARIABLE_NAME);
    }
    return null;
  }
}
