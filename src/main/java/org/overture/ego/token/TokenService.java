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

import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.overture.ego.model.entity.User;
import org.overture.ego.service.UserService;
import org.overture.ego.utils.Types;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class TokenService {

  @Value("${jwt.secret}")
  private String jwtSecret;
  @Autowired
  UserService userService;

  public String generateUserToken(IDToken idToken){
    val userName = idToken.getEmail();
    User user = userService.getByName(userName);
    if (user == null) {
      userService.createFromIDToken(idToken);
    }
    return generateUserToken(new TokenUserInfo(user));
  }

  public String generateUserToken(TokenUserInfo u) {
    val tokenContext = new TokenContext(u);
    val tokenClaims = new TokenClaims();
    tokenClaims.setIss("ego");
    tokenClaims.setValidDuration(1000000);
    tokenClaims.setContext(tokenContext);

    return Jwts.builder()
            .setClaims(Types.convertToAnotherType(tokenClaims, Map.class))
            .signWith(SignatureAlgorithm.HS512, jwtSecret)
            .compact();
  }

  public boolean validateToken(String token) {

    Jws decodedToken = null;
    try{
        decodedToken  = Jwts.parser()
        .setSigningKey(jwtSecret)
        .parseClaimsJws(token);
    } catch (Exception ex){
      log.error("Error parsing JWT: {}", ex);
    }
    return (decodedToken != null);
  }

  public User getTokenUserInfo(String token) {
    try {
      Claims body = getTokenClaims(token);
      val tokenClaims = Types.convertToAnotherType(body, TokenClaims.class);
      return userService.get(tokenClaims.getSub());
    } catch (JwtException | ClassCastException e) {
      return null;
    }
  }

  public Claims getTokenClaims(String token) {

    return Jwts.parser()
        .setSigningKey(jwtSecret)
        .parseClaimsJws(token)
        .getBody();
  }

  public JWTAccessToken getJWTAccessToken(String token){
    return new JWTAccessToken(token, this);
  }

}
