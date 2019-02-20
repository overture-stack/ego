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

import bio.overture.ego.security.AuthorizationManager;
import bio.overture.ego.security.JWTAuthorizationFilter;
import bio.overture.ego.security.OAuth2SsoFilter;
import bio.overture.ego.security.SecureAuthorizationManager;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableOAuth2Client
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
        "/oauth/ego-token"
      };

  /** Dependencies */
  private AuthenticationManager authenticationManager;

  private OAuth2SsoFilter oAuth2SsoFilter;

  @SneakyThrows
  @Autowired
  public SecureServerConfig(
      AuthenticationManager authenticationManager, OAuth2SsoFilter oAuth2SsoFilter) {
    this.authenticationManager = authenticationManager;
    this.oAuth2SsoFilter = oAuth2SsoFilter;
  }

  @Bean
  @SneakyThrows
  public JWTAuthorizationFilter authorizationFilter() {
    return new JWTAuthorizationFilter(authenticationManager, PUBLIC_ENDPOINTS);
  }

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
  public AuthorizationManager authorizationManager() {
    return new SecureAuthorizationManager();
  }

  // Register oauth2 filter earlier so it can handle redirects signaled by exceptions in
  // authentication requests.
  @Bean
  public FilterRegistrationBean<OAuth2ClientContextFilter> oauth2ClientFilterRegistration(
      OAuth2ClientContextFilter filter) {
    FilterRegistrationBean<OAuth2ClientContextFilter> registration = new FilterRegistrationBean<>();
    registration.setFilter(filter);
    registration.setOrder(-100);
    return registration;
  }

  //  int LOWEST_PRECEDENCE = Integer.MAX_VALUE;
  @Configuration
  @Order(SecurityProperties.BASIC_AUTH_ORDER - 3)
  public class OAuthConfigurerAdapter extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http.requestMatchers()
          .antMatchers(
  "/oauth/login/*"
          )
          .and()
          .csrf()
          .disable()
          .authorizeRequests()
          .anyRequest()
          .permitAll()
          .and()
          .addFilterAfter(oAuth2SsoFilter, BasicAuthenticationFilter.class);
    }
  }

  @Configuration
  @Order(SecurityProperties.BASIC_AUTH_ORDER + 3)
  public class AppConfigurerAdapter extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http.csrf()
          .disable()
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
              "/oauth/token/public_key",
              "/oauth/ego-token")
          .permitAll()
          .antMatchers(HttpMethod.OPTIONS, "/**")
          .permitAll()
          .anyRequest()
          .authenticated()
          .and()
          .addFilterBefore(authorizationFilter(), BasicAuthenticationFilter.class)
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
  }
}
