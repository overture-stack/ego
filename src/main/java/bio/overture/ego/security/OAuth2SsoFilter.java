package bio.overture.ego.security;

import static bio.overture.ego.model.enums.ProviderType.*;
import static java.util.Objects.isNull;

import bio.overture.ego.model.exceptions.SSOAuthenticationFailureHandler;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.GithubService;
import bio.overture.ego.service.LinkedinService;
import bio.overture.ego.service.OrcidService;
import bio.overture.ego.utils.Redirects;
import java.io.IOException;
import java.util.*;
import javax.annotation.PostConstruct;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.CompositeFilter;

@Slf4j
@Component
//@Order()
// @Profile("auth")
public class OAuth2SsoFilter extends CompositeFilter {

  //  private final OAuth2ClientContext oauth2ClientContext;
  private final ApplicationService applicationService;
  private final OAuth2AuthorizedClientService oAuth2AuthorizedClientService;
  private final ClientRegistrationRepository clientRegistrationRepository;

  private final SimpleUrlAuthenticationSuccessHandler simpleUrlAuthenticationSuccessHandler =
      new SimpleUrlAuthenticationSuccessHandler() {
        public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
          // todo fix redirect state
          val application =
              applicationService.getByClientId(
                  (String) request.getSession().getAttribute("ego_client_id"));

          String redirectUri = (String) request.getSession().getAttribute("ego_redirect_uri");

          val redirect = Redirects.getRedirectUri(application, redirectUri);
          if (!redirect.isEmpty()) {
            this.setDefaultTargetUrl(redirect);
            super.onAuthenticationSuccess(request, response, authentication);
          } else {
            throw new RuntimeException("Incorrect redirect uri for ego client.");
          }
        }
      };

  private final OrcidService orcidService;
  private final LinkedinService linkedinService;

  @Autowired
  private AuthenticationManager authenticationManager;
  private SSOAuthenticationFailureHandler ssoAuthenticationFailureHandler;

  @Autowired
  public OAuth2SsoFilter(
      OAuth2AuthorizedClientService oAuth2AuthorizedClientService,
      ClientRegistrationRepository registrationRepository,
      ApplicationService applicationService,
      //      OAuth2ClientResources google,
      //      OAuth2ClientResources facebook,
      //      OAuth2ClientResources github,
      //      OAuth2ClientResources linkedin,
      //      OAuth2ClientResources orcid,
      //      OAuth2ClientResources keycloak,
      OrcidService orcidService,
      LinkedinService linkedinService,
//      @Qualifier("oauth2LoginAuthManager") AuthenticationManager authenticationManager,
      SSOAuthenticationFailureHandler ssoAuthenticationFailureHandler) {

    this.applicationService = applicationService;
    this.orcidService = orcidService;
    this.linkedinService = linkedinService;
    this.ssoAuthenticationFailureHandler = ssoAuthenticationFailureHandler;
    this.oAuth2AuthorizedClientService = oAuth2AuthorizedClientService;
    this.clientRegistrationRepository = registrationRepository;
  }

  @PostConstruct
  public void postProcess() {
    //    RestTemplate restTemplate = new RestTemplate();
    val filters = new ArrayList<Filter>();
    filters.add(new GithubFilter2(clientRegistrationRepository.findByRegistrationId("github")));
    filters.add(new GoogleFilter2(clientRegistrationRepository.findByRegistrationId("google")));
    setFilters(filters);
  }

  class OAuth2SsoChildFilter extends OAuth2LoginAuthenticationFilter {
    protected ClientRegistration clientRegistration;

    // TODO: - check how we can eliminate OAuth2ChildSso Filters and only have main one since now we rely on custom user info service
    public OAuth2SsoChildFilter(String path, ClientRegistration client) {
      super(clientRegistrationRepository, oAuth2AuthorizedClientService);
      super.setFilterProcessesUrl(path);
      super.setAuthenticationManager(authenticationManager);
      super.setAuthenticationSuccessHandler(simpleUrlAuthenticationSuccessHandler);
      super.setAuthenticationFailureHandler(ssoAuthenticationFailureHandler);
      this.clientRegistration = client;
    }
  }

  class GithubFilter2 extends OAuth2SsoChildFilter {
    public GithubFilter2(ClientRegistration client) {
      super("/oauth/code/github", client);
    }
  }

  //
  //  class LinkedInFilter extends OAuth2SsoChildFilter {
  //    public LinkedInFilter(OAuth2ClientResources client) {
  //      super("/oauth/login/linkedin", client);
  //      super.setTokenServices(
  //          new OAuth2UserInfoTokenServices(
  //              client.getResource().getUserInfoUri(),
  //              client.getClient().getClientId(),
  //              super.restTemplate,
  //              LINKEDIN) {
  //            @Override
  //            protected Map<String, Object> transformMap(
  //                Map<String, Object> map, String accessToken) {
  //              val restTemplate = getRestTemplate(accessToken);
  //              return linkedinService.getPrimaryEmail(restTemplate, map);
  //            }
  //          });
  //    }
  //  }
  //

  class GoogleFilter2 extends OAuth2SsoChildFilter {
    public GoogleFilter2(ClientRegistration client) {
      super("/oauth/code/google", client);
    }
  }
  //
  //  class KeycloakFilter extends OAuth2SsoChildFilter {
  //    public KeycloakFilter(OAuth2ClientResources client) {
  //      super("/oauth/login/keycloak", client);
  //      super.setTokenServices(
  //          new OAuth2UserInfoTokenServices(
  //              client.getResource().getUserInfoUri(),
  //              client.getClient().getClientId(),
  //              super.restTemplate,
  //              KEYCLOAK));
  //    }
  //  }
  //
  //  class FacebookFilter extends OAuth2SsoChildFilter {
  //    public FacebookFilter(OAuth2ClientResources client) {
  //      super("/oauth/login/facebook", client);
  //      super.setTokenServices(
  //          new OAuth2UserInfoTokenServices(
  //              client.getResource().getUserInfoUri(),
  //              client.getClient().getClientId(),
  //              super.restTemplate,
  //              FACEBOOK));
  //    }
  //  }
  //
  //  class OrcidFilter extends OAuth2SsoChildFilter {
  //    public OrcidFilter(OAuth2ClientResources client) {
  //      super("/oauth/login/orcid", client);
  //      super.setTokenServices(
  //          new OAuth2UserInfoTokenServices(
  //              client.getResource().getUserInfoUri(),
  //              client.getClient().getClientId(),
  //              super.restTemplate,
  //              ORCID) {
  //            @Override
  //            protected Map<String, Object> transformMap(
  //                Map<String, Object> map, String accessToken) {
  //              val orcid = map.get("sub").toString();
  //              val restTemplate = getRestTemplate(accessToken);
  //              return orcidService.getPrimaryEmail(restTemplate, orcid, map);
  //            }
  //          });
  //    }
  //  }
}
