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

import bio.overture.ego.security.CorsFilter;
import bio.overture.ego.security.OAuth2RequestResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;

@Configuration
public class OAuth2ClientConfig {
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
