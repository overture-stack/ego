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
import bio.overture.ego.utils.Redirects;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import lombok.val;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableWebSecurity
@Import(OAuth2ClientConfig.class)
@Profile("auth")
public class OauthSecureServerConfig {

  @Bean
  public AuthorizationManager authorizationManager() {
    return new SecureAuthorizationManager();
  }

  final OAuth2AuthorizationRequestResolver oAuth2RequestResolver;
  final CustomOAuth2UserInfoService customOAuth2UserInfoService;
  final CustomOidc2UserInfoService customOidc2UserInfoService;
  final ApplicationService applicationService;
  final SSOAuthenticationFailureHandler failureHandler;

  public OauthSecureServerConfig(
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

  @Bean
  @Order(SecurityProperties.BASIC_AUTH_ORDER + 3000)
  public SecurityFilterChain oathFilterChain(HttpSecurity http) throws Exception {
    return http.csrf()
        .disable()
        .securityMatcher(
            "/oauth/code/*",
            "/oauth/login/*",
            "/oauth/ego-token",
            "/oauth/update-ego-token",
            "/oauth/refresh")
        .authorizeHttpRequests()
        .requestMatchers(
            "/oauth/code/*",
            "/oauth/login/*",
            "/oauth/ego-token",
            "/oauth/update-ego-token",
            "/oauth/refresh")
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
            })
        .build();
  }

  private OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest>
      authorizationCodeTokenResponseClient() {
    val tokenResponseHttpMessageConverter = new OAuth2AccessTokenResponseHttpMessageConverter();
    tokenResponseHttpMessageConverter.setAccessTokenResponseConverter(
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
