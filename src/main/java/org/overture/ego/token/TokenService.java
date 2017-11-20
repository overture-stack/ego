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
import org.overture.ego.security.JWTAccessToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class TokenService {

  @Value("${jwt.secret}")
  private String jwtSecret;

  public String generateToken(User u) {
    Claims claims = Jwts.claims().setSubject(u.getName());
    Map<String, Object> context = new HashMap<String, Object>();
    Map<String, Object> userInfo = new HashMap<String, Object>();
    userInfo.put("name", u.getName());
    userInfo.put("roles", new String[] {u.getRole()});
    userInfo.put("status", u.getStatus());
    userInfo.put("email", u.getEmail());
    userInfo.put("first_name", u.getFirstName());
    userInfo.put("last_name", u.getLastName());
    userInfo.put("groups", u.getGroupNames());
    context.put("user", userInfo);
    claims.put("sub", u.getId());
    claims.put("iss", "ego");
    claims.put("iat", (int) (System.currentTimeMillis() / 1000L));
    claims.put("exp", (int) ((System.currentTimeMillis() + 1000000) / 1000L));
    claims.put("aud", u.getApplicationNames());
    claims.put("jti", UUID.randomUUID());
    claims.put("context", context);

    return Jwts.builder()
        .setClaims(claims)
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

  public User getUserInfo(String token) {
    try {
      Claims body = getTokenClaims(token);
      val userInfo = (Map)((Map)body.get("context")).get("user");

      User u = new User();
      u.setId((Integer) body.get("sub"));
      u.setName(userInfo.get("name").toString());
      u.setEmail(userInfo.get("email").toString());
      u.setFirstName(userInfo.get("first_name").toString());
      u.setLastName(userInfo.get("last_name").toString());
      u.setRole(((ArrayList<String>)userInfo.get("roles")).get(0));
      u.setStatus(userInfo.get("status").toString());
      u.setApplicationNames((ArrayList<String>) body.get("aud"));
      u.setGroupNames((ArrayList<String>) userInfo.get("groups"));
      return u;
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
