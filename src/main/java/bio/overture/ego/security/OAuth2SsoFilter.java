package bio.overture.ego.security;

import bio.overture.ego.service.ApplicationService;
import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.CompositeFilter;

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
          this.setDefaultTargetUrl(application.getRedirectUri());
          super.onAuthenticationSuccess(request, response, authentication);
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
          new GithubUserInfoTokenServices(
              client.getResource().getUserInfoUri(),
              client.getClient().getClientId(),
              super.restTemplate));
    }
  }

  class LinkedInFilter extends OAuth2SsoChildFilter {
    public LinkedInFilter(OAuth2ClientResources client) {
      super("/oauth/login/linkedin", client);
      super.setTokenServices(
          new LinkedInUserInfoTokenServices(
              client.getResource().getUserInfoUri(),
              client.getClient().getClientId(),
              super.restTemplate));
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
