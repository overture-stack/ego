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

package org.overture.ego.config;

import lombok.SneakyThrows;
import org.overture.ego.security.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;



@Configuration
@EnableWebSecurity
@Profile("auth")
public class SecureServerConfig extends WebSecurityConfigurerAdapter {

  /*
    Constants
   */
  private final String[] PUBLIC_ENDPOINTS =
      new String[] {"/oauth/token","/oauth/google/token", "/oauth/facebook/token", "/oauth/token/public_key",
          "/oauth/token/verify"};

  @Autowired
  private AuthenticationManager authenticationManager;

  @Bean
  @SneakyThrows
  public JWTAuthorizationFilter authorizationFilter() {
    return new JWTAuthorizationFilter(authenticationManager,PUBLIC_ENDPOINTS);
  }

  @Bean
  public AuthorizationManager authorizationManager() {
    return new SecureAuthorizationManager();
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf().disable()
        .authorizeRequests()
        .antMatchers("/", "/oauth/**","/swagger**","/swagger-resources/**","/configuration/ui","/configuration/**","/v2/api**","/webjars/**").permitAll()
        .anyRequest().authenticated().and().authorizeRequests()
        .and()
        .addFilterAfter(authorizationFilter(), BasicAuthenticationFilter.class)
        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
  }

}
