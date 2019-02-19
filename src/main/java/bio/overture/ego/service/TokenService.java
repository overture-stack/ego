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

package bio.overture.ego.service;

import static bio.overture.ego.model.dto.Scope.effectiveScopes;
import static bio.overture.ego.model.dto.Scope.explicitScopes;
import static bio.overture.ego.service.UserService.extractScopes;
import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static bio.overture.ego.utils.TypeUtils.convertToAnotherType;
import static java.lang.String.format;
import static java.util.UUID.fromString;
import static org.springframework.util.DigestUtils.md5Digest;

import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.dto.TokenResponse;
import bio.overture.ego.model.dto.TokenScopeResponse;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Token;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.model.params.ScopeName;
import bio.overture.ego.reactor.events.UserEvents;
import bio.overture.ego.repository.TokenStoreRepository;
import bio.overture.ego.token.IDToken;
import bio.overture.ego.token.TokenClaims;
import bio.overture.ego.token.app.AppJWTAccessToken;
import bio.overture.ego.token.app.AppTokenClaims;
import bio.overture.ego.token.app.AppTokenContext;
import bio.overture.ego.token.signer.TokenSigner;
import bio.overture.ego.token.user.UserJWTAccessToken;
import bio.overture.ego.token.user.UserTokenClaims;
import bio.overture.ego.token.user.UserTokenContext;
import bio.overture.ego.view.Views;
import io.jsonwebtoken.*;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TokenService extends AbstractNamedService<Token, UUID> {

  /*
   * Constant
   */
  private static final String ISSUER_NAME = "ego";

  @Value("${jwt.duration:86400000}")
  private int DURATION;

  /*
   * Dependencies
   */
  private TokenSigner tokenSigner;
  private UserService userService;
  private ApplicationService applicationService;
  private UserEvents userEvents;
  private TokenStoreService tokenStoreService;
  private PolicyService policyService;

  public TokenService(
      @NonNull TokenSigner tokenSigner,
      @NonNull UserService userService,
      @NonNull ApplicationService applicationService,
      @NonNull UserEvents userEvents,
      @NonNull TokenStoreService tokenStoreService,
      @NonNull PolicyService policyService,
      @NonNull TokenStoreRepository tokenStoreRepository) {
    super(Token.class, tokenStoreRepository);
    this.tokenSigner = tokenSigner;
    this.userService = userService;
    this.applicationService = applicationService;
    this.userEvents = userEvents;
    this.tokenStoreService = tokenStoreService;
    this.policyService = policyService;
  }

  public String generateUserToken(IDToken idToken) {
    User user;
    val userName = idToken.getEmail();
    try { // TODO: Replace this with Optional for better control flow.
      user = userService.getByName(userName);
    } catch (NotFoundException e) {
      log.info("User not found, creating.");
      user = userService.createFromIDToken(idToken);
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
    Set<String> permissionNames = mapToSet(extractScopes(u), p -> p.toString());
    return generateUserToken(u, permissionNames);
  }

  public Set<Scope> getScopes(Set<ScopeName> scopeNames) {
    return mapToSet(scopeNames, this::getScope);
  }

  public Scope getScope(ScopeName name) {
    val policy = policyService.getByName(name.getName());

    return new Scope(policy, name.getAccessLevel());
  }

  public Set<Scope> missingScopes(String userName, Set<ScopeName> scopeNames) {
    val user = userService.getByName(userName);
    val userScopes = extractScopes(user);
    val requestedScopes = getScopes(scopeNames);
    return Scope.missingScopes(userScopes, requestedScopes);
  }

  public String str(Object o) {
    if (o == null) {
      return "null";
    } else {
      return "'" + o.toString() + "'";
    }
  }

  public String strList(Collection collection) {
    if (collection == null) {
      return "null";
    }
    val l = new ArrayList(collection);
    return l.toString();
  }

  @SneakyThrows
  public Token issueToken(UUID user_id, List<ScopeName> scopeNames, List<UUID> apps) {
    log.info(format("Looking for user '%s'", str(user_id)));
    log.info(format("Scopes are '%s'", strList(scopeNames)));
    log.info(format("Apps are '%s'", strList(apps)));
    val u =
        userService
            .findById(user_id)
            .orElseThrow(
                () -> new UsernameNotFoundException(format("Can't find user '%s'", str(user_id))));

    log.info(format("Got user with id '%s'", str(u.getId())));
    val userScopes = extractScopes(u);

    log.info(format("User's scopes are '%s'", str(userScopes)));

    val requestedScopes = getScopes(new HashSet<>(scopeNames));

    val missingScopes = Scope.missingScopes(userScopes, requestedScopes);
    if (!missingScopes.isEmpty()) {
      val msg = format("User %s has no access to scopes [%s]", str(user_id), str(missingScopes));
      log.info(msg);
      throw new InvalidScopeException(msg);
    }

    val tokenString = generateTokenString();
    log.info(format("Generated token string '%s'", str(tokenString)));

    val token = new Token();
    token.setExpires(DURATION);
    token.setRevoked(false);
    token.setName(tokenString);
    token.setOwner(u);

    for (Scope requestedScope : requestedScopes) {
      token.addScope(requestedScope);
    }

    if (apps != null) {
      log.info("Generating apps list");
      for (val appId : apps) {
        val app = applicationService.get(appId.toString());
        token.addApplication(app);
      }
    }

    log.info("Creating token in token store");
    tokenStoreService.create(token);

    log.info(format("Returning '%s'", str(token)));

    return token;
  }

  public Optional<Token> findByTokenString(String token) {
    return tokenStoreService.findByTokenName(token);
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

  public boolean isValidToken(String token) {
    Jws<Claims> decodedToken = null;
    try {
      decodedToken = Jwts.parser().setSigningKey(tokenSigner.getKey().get()).parseClaimsJws(token);
    } catch (JwtException e) {
      log.error("JWT token is invalid", e);
    }
    return (decodedToken != null);
  }

  public User getTokenUserInfo(String token) {
    try {
      val body = getTokenClaims(token);
      val tokenClaims =
          convertToAnotherType(body, UserTokenClaims.class, Views.JWTAccessToken.class);
      return userService.get(tokenClaims.getSub());
    } catch (JwtException | ClassCastException e) {
      log.error("Issue handling user token: {}", new String(md5Digest(token.getBytes())));
      return null;
    }
  }

  public Application getTokenAppInfo(String token) {
    try {
      val body = getTokenClaims(token);
      val tokenClaims =
          convertToAnotherType(body, AppTokenClaims.class, Views.JWTAccessToken.class);
      return applicationService.getById(fromString(tokenClaims.getSub()));
    } catch (JwtException | ClassCastException e) {
      log.error("Issue handling application token: {}", new String(md5Digest(token.getBytes())));
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
          .setClaims(convertToAnotherType(claims, Map.class, Views.JWTAccessToken.class))
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

    log.error(format("token='%s'", token));
    val application = applicationService.findByBasicToken(authToken);

    val t =
        findByTokenString(token).orElseThrow(() -> new InvalidTokenException("Token not found"));

    val clientId = application.getClientId();
    val apps = t.getApplications();
    log.info(format("Applications are %s", apps.toString()));
    if (apps != null && !apps.isEmpty()) {
      if (!(apps.stream().anyMatch(app -> app.getClientId().equals(clientId)))) {
        throw new InvalidTokenException("Token not authorized for this client");
      }
    }

    // We want to limit the scopes listed in the token to those scopes that the user
    // is allowed to access at the time the token is checked -- we don't assume that
    // they have not changed since the token was issued.
    val owner = t.getOwner();
    val scopes = explicitScopes(effectiveScopes(extractScopes(owner), t.scopes()));
    val names = mapToSet(scopes, Scope::toString);

    return new TokenScopeResponse(owner.getName(), clientId, t.getSecondsUntilExpiry(), names);
  }

  public void revokeToken(UUID userId, @NonNull String tokenName) {
    validateTokenName(tokenName);

    log.info(format("Looking for user: '%s'. ", str(userId)));
    val user =
        userService
            .findById(userId)
            .orElseThrow(
                () -> new UsernameNotFoundException(format("Can't find user '%s'", str(userId))));

    log.info(format("validating if user '%s' has permission to revoke token.", str(userId)));
    if (userService.isAdmin(user) && userService.isActiveUser(user)) {
      revokeToken(tokenName);
    } else {
      // if it's a regular user, check if the token belongs to the user
      verifyToken(tokenName, userId);
      revokeToken(tokenName);
    }
  }

  private void verifyToken(String token, UUID userId) {
    val currentToken =
        findByTokenString(token).orElseThrow(() -> new InvalidTokenException("Token not found."));

    if (!currentToken.getOwner().getId().equals(userId)) {
      throw new InvalidTokenException("Users can only revoke tokens that belong to them.");
    }
  }

  private void validateTokenName(@NonNull String tokenName) {
    log.info(format("Validating token: '%s'.", tokenName));

    if (tokenName.isEmpty()) {
      throw new InvalidTokenException("Token cannot be empty.");
    }

    if (tokenName.length() > 2048) {
      throw new InvalidRequestException("Invalid token, the maximum length for a token is 2048.");
    }
  }

  private void revokeToken(String token) {
    val currentToken =
        findByTokenString(token).orElseThrow(() -> new InvalidTokenException("Token not found."));
    if (currentToken.isRevoked()) {
      throw new InvalidTokenException(format("Token '%s' is already revoked.", token));
    }
    currentToken.setRevoked(true);
    getRepository().save(currentToken);
  }

  public List<TokenResponse> listToken(@NonNull UUID userId) {
    val user =
        userService
            .findById(userId)
            .orElseThrow(
                () -> new UsernameNotFoundException(format("Can't find user '%s'", str(userId))));

    val tokens = user.getTokens();

    if (tokens.isEmpty()) {
      throw new NotFoundException("User is not associated with any token.");
    }

    List<TokenResponse> response = new ArrayList<>();
    tokens.forEach(
        token -> {
          createTokenResponse(token, response);
        });

    return response;
  }

  private void createTokenResponse(@NonNull Token token, @NonNull List<TokenResponse> responses) {
    Set<String> scopes = mapToSet(token.scopes(), scope -> scope.toString());
    responses.add(new TokenResponse(token.getName(), scopes, token.getSecondsUntilExpiry()));
  }
}
