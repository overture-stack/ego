package bio.overture.ego.security;

import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.utils.Redirects;
import java.io.IOException;
import java.util.*;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedClientException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.filter.CompositeFilter;

@Slf4j
@Component
@Profile("auth")
public class OAuth2SsoFilter extends CompositeFilter {

  private OAuth2ClientContext oauth2ClientContext;
  private ApplicationService applicationService;
  private SimpleUrlAuthenticationSuccessHandler simpleUrlAuthenticationSuccessHandler =
      new SimpleUrlAuthenticationSuccessHandler() {
        public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
          val application =
              applicationService.getByClientId(
                  (String) request.getSession().getAttribute("ego_client_id"));

          String redirectUri = (String) request.getSession().getAttribute("ego_redirect_uri");

          val redirect = Redirects.getRedirectUri(application, redirectUri);
          if (!redirect.isEmpty()) {
            this.setDefaultTargetUrl(redirect);
            super.onAuthenticationSuccess(request, response, authentication);
          } else {
            throw new UnauthorizedClientException("Incorrect redirect uri for ego client.");
          }
        }
      };

  @Autowired
  public OAuth2SsoFilter(
      @Qualifier("oauth2ClientContext") OAuth2ClientContext oauth2ClientContext,
      ApplicationService applicationService,
      OAuth2ClientResources google,
      OAuth2ClientResources facebook,
      OAuth2ClientResources github,
      OAuth2ClientResources linkedin) {
    this.oauth2ClientContext = oauth2ClientContext;
    this.applicationService = applicationService;
    val filters = new ArrayList<Filter>();

    filters.add(new GoogleFilter(google));
    filters.add(new FacebookFilter(facebook));
    filters.add(new GithubFilter(github));
    filters.add(new LinkedInFilter(linkedin));
    setFilters(filters);
  }

  class OAuth2SsoChildFilter extends OAuth2ClientAuthenticationProcessingFilter {
    public OAuth2SsoChildFilter(String path, OAuth2ClientResources client) {
      super(path);
      OAuth2RestTemplate template = new OAuth2RestTemplate(client.getClient(), oauth2ClientContext);
      super.setRestTemplate(template);
      super.setAuthenticationSuccessHandler(simpleUrlAuthenticationSuccessHandler);
    }

    @Override
    public Authentication attemptAuthentication(
        HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
      // Don't use the existing access token, otherwise, it would attempt to get github user info
      // with linkedin access token
      this.restTemplate.getOAuth2ClientContext().setAccessToken(null);
      return super.attemptAuthentication(request, response);
    }
  }

  class GithubFilter extends OAuth2SsoChildFilter {
    public GithubFilter(OAuth2ClientResources client) {
      super("/oauth/login/github", client);
      super.setTokenServices(
          new OAuth2UserInfoTokenServices(
              client.getResource().getUserInfoUri(),
              client.getClient().getClientId(),
              super.restTemplate) {
            @Override
            protected Map<String, Object> transformMap(Map<String, Object> map, String accessToken)
                throws NoSuchElementException {
              OAuth2RestOperations restTemplate = getRestTemplate(accessToken);
              String email;

              try {
                // [{email, primary, verified}]
                email =
                    (String)
                        restTemplate
                            .exchange(
                                "https://api.github.com/user/emails",
                                HttpMethod.GET,
                                null,
                                new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                            .getBody().stream()
                            .filter(
                                x ->
                                    x.get("verified").equals(true) && x.get("primary").equals(true))
                            .findAny()
                            .orElse(Collections.emptyMap())
                            .get("email");
              } catch (RestClientException | ClassCastException ex) {
                return Collections.singletonMap("error", "Could not fetch user details");
              }

              if (email != null) {
                map.put("email", email);

                String name = (String) map.get("name");
                String[] names = name.split(" ");
                if (names.length == 2) {
                  map.put("given_name", names[0]);
                  map.put("family_name", names[1]);
                }
                return map;
              } else {
                return Collections.singletonMap("error", "Could not fetch user details");
              }
            }
          });
    }
  }

  class LinkedInFilter extends OAuth2SsoChildFilter {
    public LinkedInFilter(OAuth2ClientResources client) {
      super("/oauth/login/linkedin", client);
      super.setTokenServices(
          new OAuth2UserInfoTokenServices(
              client.getResource().getUserInfoUri(),
              client.getClient().getClientId(),
              super.restTemplate) {
            @Override
            protected Map<String, Object> transformMap(
                Map<String, Object> map, String accessToken) {
              String email = (String) map.get("emailAddress");

              if (email != null) {
                map.put("email", email);
                map.put("given_name", map.get("firstName"));
                map.put("family_name", map.get("lastName"));
                return map;
              } else {
                return Collections.singletonMap("error", "Could not fetch user details");
              }
            }
          });
    }
  }

  class GoogleFilter extends OAuth2SsoChildFilter {
    public GoogleFilter(OAuth2ClientResources client) {
      super("/oauth/login/google", client);
      super.setTokenServices(
          new OAuth2UserInfoTokenServices(
              client.getResource().getUserInfoUri(),
              client.getClient().getClientId(),
              super.restTemplate));
    }
  }

  class FacebookFilter extends OAuth2SsoChildFilter {
    public FacebookFilter(OAuth2ClientResources client) {
      super("/oauth/login/facebook", client);
      super.setTokenServices(
          new OAuth2UserInfoTokenServices(
              client.getResource().getUserInfoUri(),
              client.getClient().getClientId(),
              super.restTemplate));
    }
  }
}
