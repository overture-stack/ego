/*
 * Copyright (c) 2019. The Ontario Institute for Cancer Research. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package bio.overture.ego.config;

import bio.overture.ego.security.OAuth2ClientResources;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OAuth2ClientConfig {

  @Bean
  @ConfigurationProperties("google")
  public OAuth2ClientResources google() {
    return new OAuth2ClientResources();
  }

  @Bean
  @ConfigurationProperties("facebook")
  public OAuth2ClientResources facebook() {
    return new OAuth2ClientResources();
  }

  @Bean
  @ConfigurationProperties("github")
  public OAuth2ClientResources github() {
    return new OAuth2ClientResources();
  }

  @Bean
  @ConfigurationProperties("linkedin")
  public OAuth2ClientResources linkedin() {
    return new OAuth2ClientResources();
  }

  @Bean
  @ConfigurationProperties("orcid")
  public OAuth2ClientResources orcid() { return new OAuth2ClientResources(); }
}
