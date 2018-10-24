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

package org.overture.ego.token;

import lombok.val;
import org.overture.ego.service.ApplicationService;
import org.overture.ego.service.UserService;
import org.overture.ego.token.app.AppJWTAccessToken;
import org.overture.ego.token.app.AppTokenClaims;
import org.overture.ego.token.user.UserJWTAccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.TokenEnhancer;


public class CustomTokenEnhancer implements TokenEnhancer {

  @Autowired
  private TokenService tokenService;
  @Autowired
  private UserService userService;
  @Autowired
  private ApplicationService applicationService;

  @Override
  public OAuth2AccessToken enhance(OAuth2AccessToken oAuth2AccessToken, OAuth2Authentication oAuth2Authentication) {

    // get user or application token
    return
        oAuth2Authentication.getAuthorities() != null
        &&
        oAuth2Authentication
            .getAuthorities().stream().anyMatch(authority ->  AppTokenClaims.ROLE.equals(authority.getAuthority())) ?
      getApplicationAccessToken(oAuth2Authentication.getPrincipal().toString()) :
      getUserAccessToken(oAuth2Authentication.getPrincipal().toString());
  }

  private UserJWTAccessToken getUserAccessToken(String userName){
    val user = userService.getByName(userName);
    val token = tokenService.generateUserToken(user);
    return tokenService.getUserAccessToken(token);
  }

  private AppJWTAccessToken getApplicationAccessToken(String clientId){
    val app = applicationService.getByClientId(clientId);
    val token = tokenService.generateAppToken(app);
    return tokenService.getAppAccessToken(token);
  }

}
