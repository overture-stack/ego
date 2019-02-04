package bio.overture.ego.security;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpSession;
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
            URI uri = new URI(request.getCurrentUri());
            ServletRequestAttributes attr =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpSession session = attr.getRequest().getSession(true);
            Pattern pattern = Pattern.compile("client_id=([^&]+)?");
            Matcher matcher = pattern.matcher(uri.getQuery());
            if (matcher.find()) {
              session.setAttribute("ego_client_id", matcher.group(1));
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
