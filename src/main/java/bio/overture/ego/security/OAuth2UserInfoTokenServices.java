package bio.overture.ego.security;

import static bio.overture.ego.model.enums.ProviderType.getIdAccessor;
import static java.lang.String.format;
import static java.util.Objects.isNull;

import bio.overture.ego.model.enums.ProviderType;
import bio.overture.ego.model.exceptions.InternalServerException;
import bio.overture.ego.model.exceptions.NoPrimaryEmailException;
import bio.overture.ego.token.IDToken;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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

// This class make sure email is in the user info. User info endpoint of Github does not contain
// private email.
@Slf4j
public class OAuth2UserInfoTokenServices
    implements ResourceServerTokenServices, PrincipalExtractor {

  /** Dependencies */
  private final String userInfoEndpointUrl;

  private final String clientId;
  private final OAuth2RestOperations restTemplate;

  private final ProviderType providerType;

  private AuthoritiesExtractor authoritiesExtractor = new FixedAuthoritiesExtractor();

  public OAuth2UserInfoTokenServices(
      @NonNull String userInfoEndpointUrl,
      @NonNull String clientId,
      @NonNull OAuth2RestOperations restTemplate,
      @NonNull ProviderType providerType) {
    this.userInfoEndpointUrl = userInfoEndpointUrl;
    this.clientId = clientId;
    this.restTemplate = restTemplate;
    this.providerType = providerType;
  }

  public IDToken extractPrincipal(Map<String, Object> map) {
    String email;

    if (map.get("email") instanceof String) {
      email = (String) map.get("email");
    } else {
      throw new NoPrimaryEmailException(
          format("No primary email found for this %s account.", this.providerType));
    }

    val givenName = (String) map.getOrDefault("given_name", map.getOrDefault("first_name", ""));
    val familyName = (String) map.getOrDefault("family_name", map.getOrDefault("last_name", ""));

    val providerSubjectIdAccessor = getIdAccessor(providerType);

    if (isNull(map.get(providerSubjectIdAccessor))) {
      throw new InternalServerException("Invalid providerSubjectId accessor.");
    }
    // call toString because Github returns an integer id
    val providerSubjectId = map.get(providerSubjectIdAccessor).toString();

    return new IDToken(email, givenName, familyName, providerType, providerSubjectId);
  }

  @Override
  public OAuth2Authentication loadAuthentication(String accessToken)
      throws AuthenticationException, InvalidTokenException {
    Map<String, Object> map = getMap(this.userInfoEndpointUrl, accessToken);
    map = transformMap(map, accessToken);
    if (map.containsKey("error")) {
      if (log.isDebugEnabled()) {
        log.debug("userinfo returned error: " + map.get("error"));
      }
      throw new InvalidTokenException(accessToken);
    }
    return extractAuthentication(map);
  }

  // Guarantee that email will be fetched
  protected Map<String, Object> transformMap(Map<String, Object> map, String accessToken)
      throws NoSuchElementException {
    if (map.get("email") == null) {
      return Collections.singletonMap("error", "Could not fetch user details");
    }
    return map;
  }

  private OAuth2Authentication extractAuthentication(Map<String, Object> map) {
    Object principal = getPrincipal(map);
    List<GrantedAuthority> authorities = this.authoritiesExtractor.extractAuthorities(map);
    final OAuth2Request request =
        new OAuth2Request(null, this.clientId, null, true, null, null, null, null, null);
    final UsernamePasswordAuthenticationToken token =
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
    Object principal = this.extractPrincipal(map);
    return (principal == null ? "unknown" : principal);
  }

  @Override
  public OAuth2AccessToken readAccessToken(String accessToken) {
    throw new UnsupportedOperationException("Not supported: read access token");
  }

  protected OAuth2RestOperations getRestTemplate(String accessToken) {
    val existingToken = restTemplate.getOAuth2ClientContext().getAccessToken();
    if (existingToken == null || !accessToken.equals(existingToken.getValue())) {
      val token = new DefaultOAuth2AccessToken(accessToken);
      val tokenType = DefaultOAuth2AccessToken.BEARER_TYPE;
      token.setTokenType(tokenType);
      restTemplate.getOAuth2ClientContext().setAccessToken(token);
    }
    return restTemplate;
  }

  @SuppressWarnings("unchecked")
  protected Map<String, Object> getMap(String path, String accessToken) {
    try {
      val restTemplate = getRestTemplate(accessToken);
      return restTemplate.getForEntity(path, Map.class).getBody();
    } catch (Exception ex) {
      log.warn("Could not fetch user details: " + ex.getClass() + ", " + ex.getMessage());
      return Collections.singletonMap("error", "Could not fetch user details");
    }
  }
}
