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
import org.overture.ego.model.entity.Scope;
import org.overture.ego.model.dto.TokenScopeResponse;
import org.overture.ego.model.entity.Application;
import org.overture.ego.model.entity.ScopedAccessToken;
import org.overture.ego.model.entity.User;
import org.overture.ego.model.params.ScopeName;
import org.overture.ego.reactor.events.UserEvents;
import org.overture.ego.service.ApplicationService;
import org.overture.ego.service.PolicyService;
import org.overture.ego.service.TokenStoreService;
import org.overture.ego.service.UserService;
import org.overture.ego.token.app.AppJWTAccessToken;
import org.overture.ego.token.app.AppTokenClaims;
import org.overture.ego.token.app.AppTokenContext;
import org.overture.ego.token.signer.TokenSigner;
import org.overture.ego.token.user.UserJWTAccessToken;
import org.overture.ego.token.user.UserTokenClaims;
import org.overture.ego.token.user.UserTokenContext;
import org.overture.ego.utils.TypeUtils;
import org.overture.ego.view.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.stereotype.Service;

import javax.management.InvalidApplicationException;
import java.security.InvalidKeyException;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Slf4j
@Service
public class TokenService {
  /*
    Constant
  */
  private static final String ISSUER_NAME = "ego";
  @Autowired
  TokenSigner tokenSigner;
  @Value("${demo:false}")
  private boolean demo;
  @Value("${jwt.duration:86400000}")
  private int DURATION;
  @Autowired
  private UserService userService;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private UserEvents userEvents;
  @Autowired
  private TokenStoreService tokenStoreService;
  @Autowired
  private PolicyService policyService;

  public String generateUserToken(IDToken idToken) {
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
    //    the DB call won't block returning the ScopedAccessToken
    user.setLastLogin(new Date());
    userEvents.update(user);

    return generateUserToken(user);
  }

  @SneakyThrows
  public String generateUserToken(User u) {
    val scope=u.getPermissionsList().stream().map(p->p.toString()).collect(Collectors.toSet());
    return generateUserToken(u, scope);
  }


  public Set<Scope> getScopes(Set<ScopeName> scopeNames) {
    return scopeNames.stream().map(name -> getScope(name)).collect(Collectors.toSet());
  }

  public Scope getScope(ScopeName name) {

    val policy = policyService.getByName(name.getName());

    return new Scope(policy, name.getMask());
  }

  public Set<Scope> missingScopes(String userName, Set<ScopeName> scopeNames) {
    val user= userService.get(userName);
    log.debug("Verifying allowed scopes for user '{}'...", user);
    log.debug("Requested Scopes: {}", scopeNames);

    val missing = user.missingScopes(getScopes(scopeNames));

    return missing;
  }

  @SneakyThrows
  public ScopedAccessToken issueToken(String name, List<ScopeName> scopeNames, List<String> apps) {
    log.info(format("Looking for user '%s'",name));
    log.info(format("Scopes are '%s'", new ArrayList(scopeNames).toString()));
    log.info(format("Apps are '%s'",new ArrayList(apps).toString()));
    User u = userService.getByName(name);
    if (u == null) {
      throw new UsernameNotFoundException(format("Can't find user '%s'",name));
    }

    log.info(format("Got user with id '%s'",u.getId().toString()));
    val scopes = getScopes(new HashSet<>(scopeNames));
    val missingScopes = u.missingScopes(scopes);
    if (!missingScopes.isEmpty()) {
      val msg = format("User %s has no access to scopes [%s]", name, missingScopes);
      log.info(msg);
      throw new InvalidScopeException(msg);
    }

    val tokenString = generateTokenString();
    log.info(format("Generated token string '%s'",tokenString));

    val token = new ScopedAccessToken();
    token.setExpires(DURATION);
    token.setRevoked(false);
    token.setToken(tokenString);
    token.setOwner(u);
    scopes.stream().forEach(scope -> token.addScope(scope));

    log.info("Generating apps list");
    for (val appName : apps) {
      val app = applicationService.getByName(appName);
      if (app == null) {
        log.info(format("Can't issue token for non-existant application '%s'", appName));
        throw new InvalidApplicationException(format("No such application %s",appName));
      }
      token.addApplication(app);
    }

    log.info("Creating token in token store");
    tokenStoreService.create(token);

    log.info("Returning");

    return token;
  }

  public ScopedAccessToken findByTokenString(String token) {
    ScopedAccessToken t = tokenStoreService.findByTokenString(token);

    return t;
  }

  public String generateTokenString() {
    return UUID.randomUUID().toString();
  }

  public String generateUserToken(User u, Set<String> scope) {
    val tokenContext = new UserTokenContext(u);
    tokenContext.setScope(scope);
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
    try {
      decodedToken = Jwts.parser()
        .setSigningKey(tokenSigner.getKey().get())
        .parseClaimsJws(token);
    } catch (Exception ex) {
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
    if (tokenSigner.getKey().isPresent()) {
      return Jwts.parser()
        .setSigningKey(tokenSigner.getKey().get())
        .parseClaimsJws(token)
        .getBody();
    } else {
      throw new InvalidKeyException("Invalid signing key for the token.");
    }
  }

  public UserJWTAccessToken getUserAccessToken(String token) {
    return new UserJWTAccessToken(token, this);
  }

  public AppJWTAccessToken getAppAccessToken(String token) {
    return new AppJWTAccessToken(token, this);
  }

  @SneakyThrows
  private String getSignedToken(TokenClaims claims) {
    if (tokenSigner.getKey().isPresent()) {
      return Jwts.builder()
        .setClaims(TypeUtils.convertToAnotherType(claims, Map.class, Views.JWTAccessToken.class))
        .signWith(SignatureAlgorithm.RS256, tokenSigner.getKey().get())
        .compact();
    } else {
      throw new InvalidKeyException("Invalid signing key for the token.");
    }
  }

  @SneakyThrows
  public TokenScopeResponse checkToken(String authToken, String token) {
    if (token == null) {
      throw new InvalidTokenException("No token field found in POST request");
    }

    log.error(format("token='%s'",token));
    val application = applicationService.findByBasicToken(authToken);

    ScopedAccessToken t = findByTokenString(token);
    if (t == null) {
      throw new InvalidTokenException("Token not found");
    }

    val clientId = application.getClientId();
    val apps = t.getApplications();
    log.info(format("Applications are %s",apps.toString()));
    if (apps != null && !apps.isEmpty() ) {
      if (!(apps.stream().anyMatch(app -> app.getClientId().equals(clientId)))) {
        throw new InvalidTokenException("Token not authorized for this client");
      }
    }
    /// We want to limit the scopes listed in the token to those scopes that the sid
    // is allowed to access at the time the token is checked -- we don't assume that they
    // have not changed since the token was issued.
    val owner = t.getOwner();
    val allowed = owner.allowedScopes(t.scopes());
    val names = allowed.stream().map(s->s.toString()).collect(Collectors.toSet());
    return new TokenScopeResponse(owner.getName(), clientId,
      t.getSecondsUntilExpiry(), names);
  }
}
