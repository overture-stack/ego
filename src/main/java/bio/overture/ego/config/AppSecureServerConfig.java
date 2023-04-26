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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity
@Import(OAuth2ClientConfig.class)
@Profile("auth")
public class AppSecureServerConfig {

  OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
      new OAuth2AuthorizationServerConfigurer();
  @Autowired JWTAuthorizationFilter authorizationFilter;

  @Bean
  @Order(SecurityProperties.BASIC_AUTH_ORDER - 6)
  public SecurityFilterChain appFilterChain(HttpSecurity http) throws Exception {
    return http.csrf()
        .disable()
        .apply(authorizationServerConfigurer)
        .and()
        .securityMatcher(
            "/",
            "/favicon.ico",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/**",
            "/v2/api**",
            "/webjars/**",
            "/actuator/**",
            "/o/**",
            "/oauth/token",
            "/oauth/token/verify",
            "/oauth/token/public_key")
        .authorizeRequests()
        .requestMatchers(
            "/",
            "/favicon.ico",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/**",
            "/v2/api**",
            "/webjars/**",
            "/actuator/**",
            "/oauth/token/verify",
            "/oauth/token/public_key")
        .permitAll()
        .requestMatchers(HttpMethod.OPTIONS, "/**")
        .permitAll()
        .anyRequest()
        .authenticated()
        .and()
        .addFilterBefore(authorizationFilter, BasicAuthenticationFilter.class)
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .build();
  }
}
