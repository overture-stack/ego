package bio.overture.ego.security;

import static java.util.Objects.isNull;

import bio.overture.ego.model.enums.ProviderType;
import bio.overture.ego.service.GithubService;
import bio.overture.ego.service.LinkedinService;
import bio.overture.ego.service.OrcidService;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import static org.springframework.security.oauth2.core.oidc.StandardClaimNames.*;

@Component
@Slf4j
public class CustomOAuth2UserInfoService extends DefaultOAuth2UserService {
  final GithubService githubService;
  private final LinkedinService linkedinService;

  @Autowired
  public CustomOAuth2UserInfoService(
      GithubService githubService, LinkedinService linkedinService) {
    this.githubService = githubService;
    this.linkedinService = linkedinService;
  }

  @Override
  public CustomOAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest)
      throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
    try {
      String provider = oAuth2UserRequest.getClientRegistration().getRegistrationId();
      val idName = ProviderType.getIdAccessor(ProviderType.resolveProviderType(provider));
      if (provider.equalsIgnoreCase(ProviderType.GITHUB.toString())) {
        val info = getGithubUserEmail(oAuth2User, oAuth2UserRequest);
        return CustomOAuth2User.builder()
            .oauth2User(new DefaultOAuth2User(oAuth2User.getAuthorities(), info, idName))
            .subjectId(info.get(idName).toString())
            .email(info.getOrDefault(EMAIL, "").toString())
            .familyName(info.getOrDefault(FAMILY_NAME, "").toString())
            .givenName(info.getOrDefault(GIVEN_NAME, "").toString())
            .build();
      } else if (provider.equalsIgnoreCase(ProviderType.LINKEDIN.toString())) {
        val info = getLinkedInUserInfo(oAuth2User, oAuth2UserRequest);
        return CustomOAuth2User.builder()
            .oauth2User(new DefaultOAuth2User(oAuth2User.getAuthorities(), info, idName))
            .subjectId(info.get(idName).toString())
            .email(info.getOrDefault(EMAIL, "").toString())
            .familyName(info.getOrDefault(FAMILY_NAME, "").toString())
            .givenName(info.getOrDefault(GIVEN_NAME, "").toString())
            .build();
      } else {
        throw new RuntimeException("unhandled provider type " + provider);
      }

    } catch (AuthenticationException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("failed to load oauth user info", ex);
      // Throwing an instance of AuthenticationException will trigger the
      // OAuth2AuthenticationFailureHandler
      throw new RuntimeException(ex.getMessage(), ex.getCause());
    }
  }

  public Map<String, Object> getGithubUserEmail(
      OAuth2User user, OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
    val attributes = new HashMap<>(user.getAttributes());
    RestTemplate restTemplate = getTemplate(oAuth2UserRequest);
    String email;
    try {
      email = githubService.getVerifiedEmail(restTemplate);
    } catch (RestClientException | ClassCastException ex) {
      throw new RuntimeException("cannot fetch email");
    }
    if (!isNull(email)) {
      attributes.put(EMAIL, email);
      String name = (String) attributes.get("name");
      // github allows the name field to be null
      if (!isNull(name)) {
        githubService.parseName(name, attributes);
      }
    }
    return attributes;
  }

  public Map<String, Object> getLinkedInUserInfo(
      OAuth2User user, OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
    val attributes = new HashMap<>(user.getAttributes());
    RestTemplate restTemplate = getTemplate(oAuth2UserRequest);
    return linkedinService.getPrimaryEmail(restTemplate, attributes);
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
