package bio.overture.ego.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;
import lombok.val;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class OAuth2ClientResources {

  @NestedConfigurationProperty
  private AuthorizationCodeResourceDetails client =
      new AuthorizationCodeResourceDetails() {
        // Do not send url parameter (including the application id of ego) to authorization server
        // because some authorization server like google does not support parameter in redirect url
        @Override
        public String getRedirectUri(AccessTokenRequest request) {
          try {
            val uri = new URI(request.getCurrentUri());
            val attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            val session = attr.getRequest().getSession(true);

            val clientIdPattern = Pattern.compile("client_id=([^&]+)?");
            val clientIdMatcher = clientIdPattern.matcher(uri.getQuery());
            if (clientIdMatcher.find()) {
              session.setAttribute("ego_client_id", clientIdMatcher.group(1));
            }

            val statePattern = Pattern.compile("front_state=([^&]+)?");
            val stateMatcher = statePattern.matcher(uri.getQuery());
            if (stateMatcher.find()) {
              session.setAttribute("front_state", stateMatcher.group(1));
            }

            if (getPreEstablishedRedirectUri() != null) {
              return getPreEstablishedRedirectUri();
            }

            return new URI(
                    uri.getScheme(), uri.getAuthority(), uri.getPath(), null, uri.getFragment())
                .toString();
          } catch (URISyntaxException e) {
            return request.getCurrentUri();
          }
        }
      };

  @NestedConfigurationProperty
  private ResourceServerProperties resource = new ResourceServerProperties();

  public AuthorizationCodeResourceDetails getClient() {
    return client;
  }

  public ResourceServerProperties getResource() {
    return resource;
  }
}
