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

import bio.overture.ego.security.*;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.TokenService;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
// @EnableOAuth2Client
// @Profile("auth")
@Import(OAuth2LoginConfig.class)
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

  // Do not register OAuth2SsoFilter in global scope
  @Bean
  public FilterRegistrationBean oAuth2SsoFilterRegistration(OAuth2SsoFilter filter) {
    FilterRegistrationBean registration = new FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  @Bean
  @SneakyThrows
  public JWTAuthorizationFilter authorizationFilter(TokenService tokenService, ApplicationService applicationService) {
    return new JWTAuthorizationFilter(PUBLIC_ENDPOINTS, tokenService, applicationService);
  }


  @Bean
  public AuthorizationManager authorizationManager() {
    return new SecureAuthorizationManager();
  }

  //  int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

  /**
   * Security rules configuration for the OAuth endpoints, should be checked before basic auth note
   * the @order here
   */
  @Configuration()
  @Order(SecurityProperties.BASIC_AUTH_ORDER - 3)
  public class OAuthConfigurerAdapter extends WebSecurityConfigurerAdapter {
    final OAuth2AuthorizationRequestResolver oAuth2RequestResolver;

    @Autowired
    private OAuth2SsoFilter OAuth2SsoFilter;

    @Autowired
    CustomOAuth2UserInfoService customOAuth2UserInfoService;

    public OAuthConfigurerAdapter(
        OAuth2AuthorizationRequestResolver requestResolver) {
      this.oAuth2RequestResolver = requestResolver;
    }

    @Override
    @Bean()
    public AuthenticationManager authenticationManagerBean() throws Exception {
      return super.authenticationManagerBean();
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
                x.authorizationEndpoint(y -> y.authorizationRequestResolver(oAuth2RequestResolver));
                x.userInfoEndpoint().userService(customOAuth2UserInfoService);
              }
          )
          .addFilterAfter(OAuth2SsoFilter, BasicAuthenticationFilter.class)
      ;
    }
  }

  @Configuration
  @Order(SecurityProperties.BASIC_AUTH_ORDER + 3)
  public class AppConfigurerAdapter extends WebSecurityConfigurerAdapter {

    @Autowired
    JWTAuthorizationFilter authorizationFilter;

    @Override
    protected void configure(HttpSecurity http) throws Exception {

      // add the authorization server endpoints/filters to the security filter chain
      OAuth2AuthorizationServerConfigurer<HttpSecurity> authorizationServerConfigurer =
          new OAuth2AuthorizationServerConfigurer<>();

      http
          .csrf()
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
