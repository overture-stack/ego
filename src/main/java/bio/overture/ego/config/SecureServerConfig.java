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
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

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

  @Bean
  @SneakyThrows
  public JWTAuthorizationFilter authorizationFilterBean(
      TokenService tokenService, ApplicationService applicationService) {
    return new JWTAuthorizationFilter(PUBLIC_ENDPOINTS, tokenService, applicationService);
  }
}