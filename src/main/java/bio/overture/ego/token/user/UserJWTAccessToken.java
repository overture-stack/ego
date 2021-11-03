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

package bio.overture.ego.token.user;

import bio.overture.ego.service.TokenService;
import io.jsonwebtoken.Claims;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;

@Slf4j
public class UserJWTAccessToken extends OAuth2AccessToken {

  private Claims tokenClaims = null;
  private String token = null;

  public UserJWTAccessToken(String token, TokenService tokenService) {
    super(
        TokenType.BEARER,
        token,
        tokenService.getTokenClaims(token).getIssuedAt().toInstant(),
        tokenService.getTokenClaims(token).getExpiration().toInstant());
    this.token = token;
    this.tokenClaims = tokenService.getTokenClaims(token);
  }

  public Map<String, Object> getAdditionalInformation() {
    val output = new HashMap<String, Object>();
    output.put("groups", getUser().get("groups"));
    return output;
  }

  public Set<String> getScope() {
    val scope = ((UserTokenContext) tokenClaims.get("context")).getScope();
    return new HashSet<>(scope);
  }

  public OAuth2RefreshToken getRefreshToken() {
    return null;
  }

  public boolean isExpired() {
    return getExpiresIn() <= 0;
  }

  public Date getExpiration() {
    return tokenClaims.getExpiration();
  }

  public int getExpiresIn() {
    return (int) ((System.currentTimeMillis() - tokenClaims.getExpiration().getTime()) / 1000L);
  }

  public String getValue() {
    return token;
  }

  private Map getUser() {
    return (Map) ((Map) tokenClaims.get("context")).get("user");
  }
}
