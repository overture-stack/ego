/*
 * Copyright (c) 2017. The Ontario Institute for Cancer Research. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bio.overture.ego.config;

import bio.overture.ego.model.exceptions.SSOAuthenticationFailureHandler;
import bio.overture.ego.security.*;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.utils.Redirects;
import java.io.IOException;
import java.util.Arrays;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableWebSecurity
@Import(OAuth2ClientConfig.class)
@Profile("auth")
public class SecureServerConfig {

  /** Constants */
  private final String[] PUBLIC_ENDPOINTS =
      new String[] {
        "/oauth/token",
        "/oauth/google/token",
        "/oauth/facebook/token",
        "/oauth/token/public_key",
        "/oauth/token/verify",
        "/oauth/ego-token",
        "/oauth/update-ego-token",
        "/oauth/refresh"
      };

  // Do not register JWTAuthorizationFilter in global scope
  @Bean
  public FilterRegistrationBean jwtAuthorizationFilterRegistration(JWTAuthorizationFilter filter) {
    FilterRegistrationBean registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  public AuthorizationManager authorizationManager() {
    return new SecureAuthorizationManager();
  }

  @Configuration
  @Order(SecurityProperties.BASIC_AUTH_ORDER - 3)
  public class OAuthConfigurerAdapter extends WebSecurityConfigurerAdapter {

    final OAuth2AuthorizationRequestResolver oAuth2RequestResolver;
    final CustomOAuth2UserInfoService customOAuth2UserInfoService;
    final CustomOidc2UserInfoService customOidc2UserInfoService;
    final ApplicationService applicationService;
    final SSOAuthenticationFailureHandler failureHandler;

    public OAuthConfigurerAdapter(
        OAuth2AuthorizationRequestResolver requestResolver,
        CustomOAuth2UserInfoService customOAuth2UserInfoService,
        CustomOidc2UserInfoService customOidc2UserInfoService,
        ApplicationService applicationService,
        SSOAuthenticationFailureHandler failureHandler) {
      this.oAuth2RequestResolver = requestResolver;
      this.customOAuth2UserInfoService = customOAuth2UserInfoService;
      this.customOidc2UserInfoService = customOidc2UserInfoService;
      this.applicationService = applicationService;
      this.failureHandler = failureHandler;
    }

    @Bean
    public SimpleUrlAuthenticationSuccessHandler successHandler() {
      return new SimpleUrlAuthenticationSuccessHandler() {
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
            throw new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT),
                "Incorrect redirect uri for ego client.");
          }
        }
      };
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http.requestMatchers()
          .antMatchers(
              "/oauth/code/*",
              "/oauth/login/*",
              "/oauth/ego-token",
              "/oauth/update-ego-token",
              "/oauth/refresh")
          .and()
          .csrf()
          .disable()
          .authorizeRequests()
          .anyRequest()
          .permitAll()
          .and()
          .oauth2Login(
              x -> {
                x.redirectionEndpoint().baseUri("/oauth/code/{registrationId}");
                x.authorizationEndpoint(y -> y.authorizationRequestResolver(oAuth2RequestResolver));
                x.tokenEndpoint()
                    .accessTokenResponseClient(this.authorizationCodeTokenResponseClient());
                x.userInfoEndpoint().oidcUserService(this.customOidc2UserInfoService);
                x.userInfoEndpoint().userService(customOAuth2UserInfoService);
                x.successHandler(this.successHandler());
                x.failureHandler(this.failureHandler);
              });
    }

    private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
        authorizationCodeTokenResponseClient() {
      val tokenResponseHttpMessageConverter = new OAuth2AccessTokenResponseHttpMessageConverter();
      tokenResponseHttpMessageConverter.setTokenResponseConverter(
          new OAuth2AccessTokenResponseConverterWithDefaults());

      val restTemplate =
          new RestTemplate(
              Arrays.asList(new FormHttpMessageConverter(), tokenResponseHttpMessageConverter));
      restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());

      val tokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();
      tokenResponseClient.setRestOperations(restTemplate);

      return tokenResponseClient;
    }
  }

  @Bean
  @SneakyThrows
  public JWTAuthorizationFilter authorizationFilter(
      TokenService tokenService, ApplicationService applicationService) {
    return new JWTAuthorizationFilter(PUBLIC_ENDPOINTS, tokenService, applicationService);
  }

  @Configuration
  @Order(SecurityProperties.BASIC_AUTH_ORDER + 3)
  public class AppConfigurerAdapter extends WebSecurityConfigurerAdapter {

    OAuth2AuthorizationServerConfigurer<HttpSecurity> authorizationServerConfigurer =
        new OAuth2AuthorizationServerConfigurer<>();
    @Autowired JWTAuthorizationFilter authorizationFilter;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http.csrf()
          .disable()
          .apply(authorizationServerConfigurer)
          .and()
          .authorizeRequests()
          .antMatchers(
              "/",
              "/favicon.ico",
              "/swagger**",
              "/swagger-resources/**",
              "/configuration/ui",
              "/configuration/**",
              "/v2/api**",
              "/webjars/**",
              "/actuator/**",
              "/oauth/token/verify",
              "/oauth/token/public_key")
          .permitAll()
          .antMatchers(HttpMethod.OPTIONS, "/**")
          .permitAll()
          .anyRequest()
          .authenticated()
          .and()
          .addFilterBefore(authorizationFilter, BasicAuthenticationFilter.class)
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
  }
}
