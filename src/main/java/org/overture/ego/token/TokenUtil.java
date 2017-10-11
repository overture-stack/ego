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
import org.springframework.stereotype.Component;

@Component
public class TokenUtil {

  @Value("${jwt.secret}")
  private String jwtSecret;

  public String generateToken(User u) {
    Claims claims = Jwts.claims().setSubject(u.getUserName());
    claims.put("userId", u.getId());
    claims.put("username", u.getUserName());
    claims.put("role", u.getRole());
    claims.put("email", u.getEmail());
    claims.put("firstName", u.getFirstName());
    claims.put("lastName", u.getLastName());
    claims.put("createdAt", u.getCreatedAt());
    claims.put("lastLogin", u.getLastLogin());
    claims.put("role", u.getRole());
    claims.put("status", u.getStatus());

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

      return User.builder().id((String) body.get("id"))
          .userName(body.getSubject())
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
