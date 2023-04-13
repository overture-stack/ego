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

package bio.overture.ego.token;

import bio.overture.ego.model.entity.User;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.token.app.AppJWTAccessToken;
import java.time.Instant;
import java.util.List;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

// This class is responsible to modify and customize the jwt claims
@Component
public class CustomTokenEnhancer implements OAuth2TokenCustomizer<JwtEncodingContext> {

  private final TokenService tokenService;
  private final UserService userService;
  private final ApplicationService applicationService;

  @Autowired
  public CustomTokenEnhancer(
      TokenService tokenService, UserService userService, ApplicationService applicationService) {
    this.tokenService = tokenService;
    this.userService = userService;
    this.applicationService = applicationService;
  }

  @Override
  public void customize(JwtEncodingContext context) {
    val client = context.getRegisteredClient();
    val isAppToken =
        context.getAuthorizationGrantType().equals(AuthorizationGrantType.CLIENT_CREDENTIALS);
    if (isAppToken) {
      val appToken = getApplicationAccessToken(client.getClientId());
      context.getClaims().claim("context", appToken.getTokenClaims().get("context"));
      context.getClaims().expiresAt(appToken.getTokenClaims().getExpiration().toInstant());
      context.getClaims().issuer(appToken.getTokenClaims().getIssuer());
      context.getClaims().issuedAt(appToken.getTokenClaims().getIssuedAt().toInstant());
      context.getClaims().subject(appToken.getTokenClaims().getSubject());
      context.getClaims().id(appToken.getTokenClaims().getId());
      context.getClaims().audience(List.of());
    } else {
      val user = (User) context.getPrincipal();
      val claims = tokenService.getUserTokenClaims(userService.getById(user.getId()));
      context.getClaims().issuedAt(Instant.ofEpochSecond(claims.iat));
      context.getClaims().expiresAt(Instant.ofEpochSecond(claims.exp));
      context.getClaims().issuer(claims.iss);
      context.getClaims().subject(claims.sub);
      context.getClaims().id(claims.getJti());
      context.getClaims().audience(List.of());
      context.getClaims().claim("context", claims.getContext());
    }
  }

  private AppJWTAccessToken getApplicationAccessToken(String clientId) {
    val app = applicationService.getByClientId(clientId);
    val token = tokenService.generateAppToken(app);
    return tokenService.getAppAccessToken(token);
  }
}
