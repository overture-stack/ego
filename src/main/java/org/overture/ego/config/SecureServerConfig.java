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
import org.overture.ego.security.AuthorizationManager;
import org.overture.ego.security.CorsFilter;
import org.overture.ego.security.JWTAuthorizationFilter;
import org.overture.ego.security.SecureAuthorizationManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

import java.text.SimpleDateFormat;
import java.util.TimeZone;


@Configuration
@EnableWebSecurity
@Profile("auth")
public class SecureServerConfig extends WebSecurityConfigurerAdapter {


  @Bean
  @SneakyThrows
  JWTAuthorizationFilter authorizationFilter() {
    return new JWTAuthorizationFilter(authenticationManager());
  }

  @Bean
  public AuthorizationManager authorizationManager() {
    return new SecureAuthorizationManager();
  }

  @Bean
  CorsFilter corsFilter() {
    return new CorsFilter();
  }

  @Bean
  SimpleDateFormat formatter(){
    SimpleDateFormat formatter =
            new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    return formatter;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf().disable()
        .authorizeRequests()
        .antMatchers("/", "/oauth/**","/swagger**","/swagger-resources/**","/configuration/ui","/configuration/**","/v2/api**","/webjars/**").permitAll()
        .anyRequest().authenticated().and().authorizeRequests()
        .and()
        .addFilter(authorizationFilter())
        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
  }

}
