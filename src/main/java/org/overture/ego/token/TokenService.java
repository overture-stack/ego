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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.val;
import org.overture.ego.model.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
    userInfo.put("email", u.getEmail());
    userInfo.put("first_name", u.getFirstName());
    userInfo.put("last_name", u.getLastName());
    userInfo.put("groups", u.getGroupNames());
    context.put("user", userInfo);
    claims.put("sub", u.getId());
    claims.put("iss", "ego");
    claims.put("iat", (int) (System.currentTimeMillis() / 1000L));
    claims.put("aud", u.getApplicationNames());
    claims.put("jti", UUID.randomUUID());
    claims.put("context", context);


    return Jwts.builder()
        .setClaims(claims)
        .signWith(SignatureAlgorithm.HS512, jwtSecret)
        .compact();
  }

  public boolean validateToken(String token) {

    val decodedToken = Jwts.parser()
        .setSigningKey(jwtSecret)
        .parseClaimsJws(token);
    return (decodedToken != null);
  }

  public User getUserInfo(String token) {
    try {
      Claims body = getTokenClaims(token);

      return User.builder().id((Integer) body.get("id"))
          .name(body.getSubject())
          .email((String) body.get("email"))
          .firstName((String) body.get("firstName"))
          .lastName((String) body.get("lastName"))
          .createdAt((String) body.get("createdAt"))
          .lastLogin((String) body.get("lastLogin"))
          .role((String) body.get("role"))
          .status((String) body.get("status")).build();

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

}
