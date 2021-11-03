package bio.overture.ego.security;

import bio.overture.ego.model.enums.ProviderType;
import bio.overture.ego.service.GithubService;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;

@Component
public class CustomOAuth2UserInfoService extends DefaultOAuth2UserService {

  @Autowired
  GithubService githubService;

  @Override
  public CustomOAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
    try {
      String provider = oAuth2UserRequest.getClientRegistration().getRegistrationId();
      if (provider.equalsIgnoreCase(ProviderType.GITHUB.toString())) {
        val info = getMoreUserInfo(oAuth2User, oAuth2UserRequest);
        return CustomOAuth2User.builder()
            .oauth2User(new DefaultOAuth2User(oAuth2User.getAuthorities(), info, "id"))
            .subjectId(info.getOrDefault("id", "").toString())
            .email(info.getOrDefault("email", "").toString())
            .familyName(info.getOrDefault("family_name", "").toString())
            .givenName(info.getOrDefault("given_name", "").toString())
            .build();
      }
      return CustomOAuth2User.builder()
          .oauth2User(oAuth2User)
          .build();
    } catch (AuthenticationException ex) {
      throw ex;
    } catch (Exception ex) {
      ex.printStackTrace();
      // Throwing an instance of AuthenticationException will trigger the
      // OAuth2AuthenticationFailureHandler
      throw new RuntimeException(ex.getMessage(), ex.getCause());
    }
  }

  public Map<String, Object> getMoreUserInfo(OAuth2User user, OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    val attributes = new HashMap<>(user.getAttributes());
    restTemplate.getInterceptors().add((x,y,z) -> {
      x.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + oAuth2UserRequest.getAccessToken().getTokenValue());
      return z.execute(x, y);
    });
    String email;
    try {
      email = githubService.getVerifiedEmail(restTemplate);
    } catch (RestClientException | ClassCastException ex) {
      throw new RuntimeException("cannot fetch email");
    }
    if (!isNull(email)) {
      attributes.put("email", email);
      String name = (String) attributes.get("name");
      // github allows the name field to be null
      if (!isNull(name)) {
        githubService.parseName(name, attributes);
      }
    }
    return attributes;
  }


}
