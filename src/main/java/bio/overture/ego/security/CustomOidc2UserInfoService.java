package bio.overture.ego.security;

import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.*;

import bio.overture.ego.model.enums.ProviderType;
import bio.overture.ego.service.OrcidService;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class CustomOidc2UserInfoService extends OidcUserService {
  private final OrcidService orcidService;

  @Autowired
  public CustomOidc2UserInfoService(OrcidService orcidService) {
    this.orcidService = orcidService;
  }

  @Override
  public OidcUser loadUser(OidcUserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
    OidcUser oidcUser = super.loadUser(oAuth2UserRequest);
    try {
      String provider = oAuth2UserRequest.getClientRegistration().getRegistrationId();
      if (provider.equalsIgnoreCase(ProviderType.ORCID.toString())) {
        val info = getOrcidUserInfo(oidcUser, oAuth2UserRequest);
        val idName = ProviderType.getIdAccessor(ProviderType.resolveProviderType(provider));
        return CustomOAuth2User.builder()
            .oauth2User(new DefaultOAuth2User(oidcUser.getAuthorities(), info, idName))
            .subjectId(info.get(idName).toString())
            .email(info.getOrDefault(EMAIL, "").toString())
            .familyName(info.getOrDefault(FAMILY_NAME, "").toString())
            .givenName(info.getOrDefault(GIVEN_NAME, "").toString())
            .build();
      }
      return CustomOAuth2User.builder()
          .oauth2User(oidcUser)
          .subjectId(oidcUser.getSubject())
          .email(oidcUser.getEmail())
          .familyName(oidcUser.getFamilyName())
          .givenName(oidcUser.getGivenName())
          .accessToken(oAuth2UserRequest.getAccessToken().getTokenValue())
          .build();
    } catch (AuthenticationException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("failed to load OIDC user info", ex);
      // Throwing an instance of AuthenticationException will trigger the
      // OAuth2AuthenticationFailureHandler
      throw new RuntimeException(ex.getMessage(), ex.getCause());
    }
  }

  public Map<String, Object> getOrcidUserInfo(
      OAuth2User user, OAuth2UserRequest oAuth2UserRequest) {
    val attributes = new HashMap<>(user.getAttributes());
    RestTemplate restTemplate = getTemplate(oAuth2UserRequest);
    val orcid = attributes.get(SUB).toString();
    return orcidService.getPrimaryEmail(restTemplate, orcid, attributes);
  }

  private RestTemplate getTemplate(OAuth2UserRequest oAuth2UserRequest) {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate
        .getInterceptors()
        .add(
            (x, y, z) -> {
              x.getHeaders()
                  .set(
                      HttpHeaders.AUTHORIZATION,
                      "Bearer " + oAuth2UserRequest.getAccessToken().getTokenValue());
              return z.execute(x, y);
            });
    return restTemplate;
  }
}
