package bio.overture.ego.security;

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.service.ApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.filter.CompositeFilter;

import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class OAuth2SsoFilter extends CompositeFilter {

  private OAuth2ClientContext oauth2ClientContext;
  private ApplicationService applicationService;
  private SimpleUrlAuthenticationSuccessHandler simpleUrlAuthenticationSuccessHandler = new SimpleUrlAuthenticationSuccessHandler() {
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {
      Application application = applicationService.getByClientId(request.getParameter("client_id"));
      String redirectUri = application.getRedirectUri();
      this.setDefaultTargetUrl(redirectUri);
      super.onAuthenticationSuccess(request, response, authentication);
    }
  };

  @Autowired
  public OAuth2SsoFilter(
          @Qualifier("oauth2ClientContext") OAuth2ClientContext oauth2ClientContext,
          ApplicationService applicationService,
          OAuth2ClientResources github,
          OAuth2ClientResources linkedin) {
    super();
    this.oauth2ClientContext = oauth2ClientContext;
    this.applicationService = applicationService;
    List<Filter> filters = new ArrayList<>();
    filters.add(childSsoFilter(github, "/oauth/login/github"));
    filters.add(childSsoFilter(linkedin, "/oauth/login/linkedin"));
    setFilters(filters);
  }

  private Filter childSsoFilter(OAuth2ClientResources client, String path) {
    // Currently not checking if client_id is valid
    OAuth2ClientAuthenticationProcessingFilter filter =
            new OAuth2ClientAuthenticationProcessingFilter(path);
    OAuth2RestTemplate template = new OAuth2RestTemplate(client.getClient(), oauth2ClientContext);

    filter.setAuthenticationSuccessHandler(simpleUrlAuthenticationSuccessHandler);
    filter.setRestTemplate(template);

    OAuth2UserInfoTokenServices tokenServices =
      new OAuth2UserInfoTokenServices(
              client.getResource().getUserInfoUri(), client.getClient().getClientId(), template) {
        @Override
        @SuppressWarnings("unchecked")
        protected Map<String, Object> ensureEmail(Map<String, Object> map, String accessToken) {
          if (map.get("email") != null) {
            return map;
          }
          // linkedin
          if (map.get("emailAddress") != null) {
            map.put("email", map.get("emailAddress"));
            return map;
          }
          // github
          try {
            OAuth2RestOperations restTemplate = getRestTemplate(accessToken);
            List<Map<String, Object>> emails = restTemplate.getForEntity("https://api.github.com/user/emails", List.class).getBody();
            Map<String, Object> email = emails.stream()
                    .filter(x -> x.get("verified").equals(true) && x.get("primary").equals(true))
                    .findAny()
                    .orElse(null);
            map.put("email", email.get("email"));
            return map;
          } catch (RestClientException|NullPointerException ex) {
            return Collections.singletonMap("error", "Could not fetch user details");
          }
        }
      };

    filter.setTokenServices(tokenServices);
    return filter;
  }
}
