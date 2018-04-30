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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.overture.ego.model.entity.Application;
import org.overture.ego.model.entity.User;
import org.overture.ego.reactor.events.UserEvents;
import org.overture.ego.service.UserService;
import org.overture.ego.token.app.AppJWTAccessToken;
import org.overture.ego.token.app.AppTokenClaims;
import org.overture.ego.token.app.AppTokenContext;
import org.overture.ego.token.signer.TokenSigner;
import org.overture.ego.token.user.UserTokenClaims;
import org.overture.ego.token.user.UserTokenContext;
import org.overture.ego.token.user.UserJWTAccessToken;
import org.overture.ego.utils.TypeUtils;
import org.overture.ego.view.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.InvalidKeyException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

@Slf4j
@Service
public class TokenService {

  @Value("${demo:false}")
  private boolean demo;

  @Value("${jwt.duration:86400000}")
  private int DURATION;
  @Autowired
  private UserService userService;
  @Autowired
  private UserEvents userEvents;
  @Autowired
  TokenSigner tokenSigner;
  @Autowired
  private SimpleDateFormat dateFormatter;
  /*
    Constant
  */
  private static final String ISSUER_NAME="ego";


  public String generateUserToken(IDToken idToken){
    // If the demo flag is set, all tokens will be generated as the Demo User,
    // otherwise, get the user associated with their idToken
    User user;
    if (demo) {
      user = userService.getOrCreateDemoUser();
    } else {
      val userName = idToken.getEmail();
      user = userService.getByName(userName);
      if (user == null) {
        user = userService.createFromIDToken(idToken);
      }
    }

    // Update user.lastLogin in the DB
    // Use events as these are async:
    //    the DB call won't block returning the Token
    user.setLastLogin(dateFormatter.format(new Date()));
    userEvents.update(user);

    return generateUserToken(user);
  }

  @SneakyThrows
  public String generateUserToken(User u) {
    val tokenContext = new UserTokenContext(u);
    val tokenClaims = new UserTokenClaims();
    tokenClaims.setIss(ISSUER_NAME);
    tokenClaims.setValidDuration(DURATION);
    tokenClaims.setContext(tokenContext);
    return getSignedToken(tokenClaims);
  }

  @SneakyThrows
  public String generateAppToken(Application application) {
    val tokenContext = new AppTokenContext(application);
    val tokenClaims = new AppTokenClaims();
    tokenClaims.setIss(ISSUER_NAME);
    tokenClaims.setValidDuration(DURATION);
    tokenClaims.setContext(tokenContext);
    return getSignedToken(tokenClaims);
  }

  public boolean validateToken(String token) {

    Jws decodedToken = null;
    try{
        decodedToken  = Jwts.parser()
        .setSigningKey(tokenSigner.getKey().get())
        .parseClaimsJws(token);
    } catch (Exception ex){
      log.error("Error parsing JWT: {}", ex);
    }
    return (decodedToken != null);
  }

  public User getTokenUserInfo(String token) {
    try {
      Claims body = getTokenClaims(token);
      val tokenClaims = TypeUtils.convertToAnotherType(body, UserTokenClaims.class, Views.JWTAccessToken.class);
      return userService.get(tokenClaims.getSub());
    } catch (JwtException | ClassCastException e) {
      return null;
    }
  }

  @SneakyThrows
  public Claims getTokenClaims(String token) {

    if(tokenSigner.getKey().isPresent()) {
    return Jwts.parser()
        .setSigningKey(tokenSigner.getKey().get())
        .parseClaimsJws(token)
        .getBody();
  } else {
      throw new InvalidKeyException("Invalid signing key for the token.");
    }
  }

  public UserJWTAccessToken getUserAccessToken(String token){
    return new UserJWTAccessToken(token, this);
  }

  public AppJWTAccessToken getAppAccessToken(String token){
    return new AppJWTAccessToken(token, this);
  }

  @SneakyThrows
  private String getSignedToken(TokenClaims claims){
    if(tokenSigner.getKey().isPresent()) {
      return Jwts.builder()
          .setClaims(TypeUtils.convertToAnotherType(claims, Map.class, Views.JWTAccessToken.class))
          .signWith(SignatureAlgorithm.RS256, tokenSigner.getKey().get())
          .compact();
    } else {
      throw new InvalidKeyException("Invalid signing key for the token.");
    }
  }
}
