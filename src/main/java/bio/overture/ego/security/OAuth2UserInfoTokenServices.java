package bio.overture.ego.security;

import bio.overture.ego.token.IDToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.oauth2.resource.AuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.FixedAuthoritiesExtractor;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

// This class make sure email is in the user info. User info endpoint of Github does not contain
// private email.
@Slf4j
public class OAuth2UserInfoTokenServices implements ResourceServerTokenServices {

  private final String userInfoEndpointUrl;

  private final String clientId;

  private OAuth2RestOperations restTemplate;

  private String tokenType = DefaultOAuth2AccessToken.BEARER_TYPE;

  private AuthoritiesExtractor authoritiesExtractor = new FixedAuthoritiesExtractor();

  private PrincipalExtractor principalExtractor = OAuth2UserInfoTokenServices::extractPrincipalFromMap;

  static public IDToken extractPrincipalFromMap(Map<String, Object> map) {
    String email;
    String givenName;
    String familyName;

    if (map.get("email") instanceof String) {
      email = (String) map.get("email");
    } else {
      return null;
    }

    givenName = (String) map.getOrDefault("given_name", "");
    familyName = (String) map.getOrDefault("family_name", "");
    return new IDToken(email, givenName , familyName);
  }

  public OAuth2UserInfoTokenServices(
      String userInfoEndpointUrl, String clientId, OAuth2RestOperations restTemplate) {
    this.userInfoEndpointUrl = userInfoEndpointUrl;
    this.clientId = clientId;
    this.restTemplate = restTemplate;
  }

  @Override
  public OAuth2Authentication loadAuthentication(String accessToken)
      throws AuthenticationException, InvalidTokenException {
    Map<String, Object> map = getMap(this.userInfoEndpointUrl, accessToken);
    map = ensureEmail(map, accessToken);
    if (map.containsKey("error")) {
      if (log.isDebugEnabled()) {
        log.debug("userinfo returned error: " + map.get("error"));
      }
      throw new InvalidTokenException(accessToken);
    }
    return extractAuthentication(map);
  }

  // Guarantee that email will be fetched
  protected Map<String, Object> ensureEmail(Map<String, Object> map, String accessToken) throws NoSuchElementException {
    if (map.get("email")==null) {
        return Collections.singletonMap("error", "Could not fetch user details");
    }
    return map;
  }

  private OAuth2Authentication extractAuthentication(Map<String, Object> map) {
    Object principal = getPrincipal(map);
    List<GrantedAuthority> authorities = this.authoritiesExtractor.extractAuthorities(map);
    OAuth2Request request =
        new OAuth2Request(null, this.clientId, null, true, null, null, null, null, null);
    UsernamePasswordAuthenticationToken token =
        new UsernamePasswordAuthenticationToken(principal, "N/A", authorities);
    token.setDetails(map);
    return new OAuth2Authentication(request, token);
  }

  /**
   * Return the principal that should be used for the token. The default implementation delegates to
   * the {@link PrincipalExtractor}.
   *
   * @param map the source map
   * @return the principal or {@literal "unknown"}
   */
  protected Object getPrincipal(Map<String, Object> map) {
    Object principal = this.principalExtractor.extractPrincipal(map);
    return (principal == null ? "unknown" : principal);
  }

  @Override
  public OAuth2AccessToken readAccessToken(String accessToken) {
    throw new UnsupportedOperationException("Not supported: read access token");
  }

  protected OAuth2RestOperations getRestTemplate(String accessToken) {
    OAuth2AccessToken existingToken = restTemplate.getOAuth2ClientContext().getAccessToken();
    if (existingToken == null || !accessToken.equals(existingToken.getValue())) {
      DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(accessToken);
      token.setTokenType(this.tokenType);
      restTemplate.getOAuth2ClientContext().setAccessToken(token);
    }
    return restTemplate;
  }

  @SuppressWarnings("unchecked")
  protected Map<String, Object> getMap(String path, String accessToken) {
    try {
      OAuth2RestOperations restTemplate = getRestTemplate(accessToken);
      return restTemplate.getForEntity(path, Map.class).getBody();
    } catch (Exception ex) {
      log.warn("Could not fetch user details: " + ex.getClass() + ", " + ex.getMessage());
      return Collections.singletonMap("error", "Could not fetch user details");
    }
  }
}
