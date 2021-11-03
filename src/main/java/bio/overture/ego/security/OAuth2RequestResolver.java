package bio.overture.ego.security;

import java.net.URI;
import javax.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Custom request resolver to capture request info before sending it to oauth2 providers and store
 * them in the current request session
 *
 * intended to replace {@see OAuth2ClientResources}
 */
public class OAuth2RequestResolver implements OAuth2AuthorizationRequestResolver {
  private DefaultOAuth2AuthorizationRequestResolver resolver;

  public OAuth2RequestResolver(
      ClientRegistrationRepository clientRegistrationRepository,
      String authorizationRequestBaseUri) {
    this.resolver =
        new DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository, authorizationRequestBaseUri);
  }

  @SneakyThrows
  @Override
  public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
    val uri = new URI(request.getRequestURI() + "?" + request.getQueryString());
    val attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    val session = attr.getRequest().getSession(true);

    val uriComponents = UriComponentsBuilder.fromUri(uri).build();
    val queryParams = uriComponents.getQueryParams();

    val clientId = queryParams.getFirst("client_id");
    if (clientId != null && !clientId.isEmpty()) {
      session.setAttribute("ego_client_id", clientId);
    }

    val redirectUri = queryParams.getFirst("redirect_uri");
    if (redirectUri != null && !redirectUri.isEmpty()) {
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
}
