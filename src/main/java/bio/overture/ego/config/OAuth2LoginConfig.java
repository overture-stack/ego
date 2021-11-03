package bio.overture.ego.config;

import bio.overture.ego.security.CorsFilter;
import bio.overture.ego.security.OAuth2RequestResolver;
import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationProvider;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Configuration
public class OAuth2LoginConfig {
//  @Bean
//  public OAuth2UserService<OAuth2UserRequest, OAuth2User> defaultUserService() {
//    return new DefaultOAuth2UserService();
//  }
//
//  @Bean
//  public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
//      defaultResponseClient() {
//    return new DefaultAuthorizationCodeTokenResponseClient();
//  }
//
//  @Bean
//  OAuth2LoginAuthenticationProvider oauth2LoginAuthenticationProvider() {
//    return new OAuth2LoginAuthenticationProvider(defaultResponseClient(), defaultUserService());
//  }
//
//  @Bean
//  public AuthenticationManager oauth2LoginAuthManager(OidcAuthorizationCodeAuthenticationProvider provider) {
//    return new ProviderManager(Collections.singletonList(provider));
//  }

  @Bean
  public OAuth2AuthorizationRequestResolver oAuth2AuthorizationRequestResolver(
      ClientRegistrationRepository clientRegistrationRepository) {
    return new OAuth2RequestResolver(clientRegistrationRepository, "/oauth/login/");
  }

  @Bean
  @Primary
  public CorsFilter corsFilter() {
    return new CorsFilter();
  }
}
