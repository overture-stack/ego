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
import static bio.overture.ego.model.enums.ApplicationType.ADMIN;
import static bio.overture.ego.service.ApplicationService.extractScopes;
import static bio.overture.ego.service.UserService.extractScopes;
import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static bio.overture.ego.utils.EntityServices.checkEntityExistence;
import static bio.overture.ego.utils.TypeUtils.convertToAnotherType;
import static java.lang.String.format;
import static java.util.UUID.fromString;
import static org.springframework.data.jpa.domain.Specification.where;
import static org.springframework.util.DigestUtils.md5Digest;

import bio.overture.ego.model.dto.*;
import bio.overture.ego.model.entity.ApiKey;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.exceptions.ForbiddenException;
import bio.overture.ego.model.params.ScopeName;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.TokenStoreRepository;
import bio.overture.ego.repository.UserRepository;
import bio.overture.ego.repository.queryspecification.TokenStoreSpecification;
import bio.overture.ego.security.BasicAuthToken;
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
import com.google.common.collect.*;
import io.jsonwebtoken.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

// TODO: rename to ApiKeyService [anncatton]
@Slf4j
@Service
public class TokenService extends AbstractNamedService<ApiKey, UUID> {

  /*
   * Constant
   */
  private static final String ISSUER_NAME = "ego";

  /*
   * Dependencies
   */
  private TokenSigner tokenSigner;
  private UserService userService;
  private ApplicationService applicationService;
  private ApiKeyStoreService apiKeyStoreService;
  private PolicyService policyService;
  private final UserRepository userRepository;

  /** Configuration */
  private int JWT_DURATION;

  private int API_TOKEN_DURATION;

  public TokenService(
      @NonNull TokenSigner tokenSigner,
      @NonNull UserService userService,
      @NonNull ApplicationService applicationService,
      @NonNull ApiKeyStoreService apiKeyStoreService,
      @NonNull PolicyService policyService,
      @NonNull TokenStoreRepository tokenStoreRepository,
      @NonNull UserRepository userRepository,
      @Value("${jwt.durationMs:10800000}") int JWT_DURATION,
      @Value("${apitoken.durationDays:365}") int API_TOKEN_DURATION) {
    super(ApiKey.class, tokenStoreRepository);
    this.tokenSigner = tokenSigner;
    this.userService = userService;
    this.applicationService = applicationService;
    this.apiKeyStoreService = apiKeyStoreService;
    this.policyService = policyService;
    this.userRepository = userRepository;
    this.JWT_DURATION = JWT_DURATION;
    this.API_TOKEN_DURATION = API_TOKEN_DURATION;
  }

  @Override
  public ApiKey getWithRelationships(@NonNull UUID id) {
    return apiKeyStoreService.getWithRelationships(id);
  }

  public String generateUserToken(IDToken idToken) {
    val user = userService.getUserByToken(idToken);
    return generateUserToken(user);
  }

  public String updateUserToken(String accessToken) {
    Jws<Claims> decodedToken = validateAndReturn(accessToken);

    val expiration = decodedToken.getBody().getExpiration().getTime();
    val currentTime = Instant.now().toEpochMilli();

    val userId = decodedToken.getBody().getSubject();
    val user = userService.getById(UUID.fromString(userId));

    Set<String> scope = extractExplicitScopes(user);
    val tokenClaims = generateUserTokenClaims(user, scope);
    tokenClaims.setValidDuration((int) (expiration - currentTime));

    return getSignedToken(tokenClaims);
  }

  @SneakyThrows
  public String generateUserToken(User u) {
    return generateUserToken(u, extractExplicitScopes(u));
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
  public ApiKey issueApiKey(UUID user_id, List<ScopeName> scopeNames, String description) {
    log.info(format("Looking for user '%s'", str(user_id)));
    log.info(format("Scopes are '%s'", strList(scopeNames)));
    log.info(format("ApiKey description is '%s'", description));

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

    val apiKeyString = generateApiKeyString();
    log.info(format("Generated apiKey string '%s'", str(apiKeyString)));

    val cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_YEAR, API_TOKEN_DURATION);
    val expiryDate = cal.getTime();

    val today = Calendar.getInstance();

    val apiKey = new ApiKey();
    apiKey.setExpiryDate(expiryDate);
    apiKey.setIssueDate(today.getTime());
    apiKey.setRevoked(false);
    apiKey.setName(apiKeyString);
    apiKey.setOwner(u);
    apiKey.setDescription(description);

    for (Scope requestedScope : requestedScopes) {
      apiKey.addScope(requestedScope);
    }

    log.info("Creating apiKey in apiKey store");
    apiKeyStoreService.create(apiKey);

    log.info(format("Returning '%s'", str(apiKey)));

    return apiKey;
  }

  public Optional<ApiKey> findByApiKeyString(String apiKey) {
    return apiKeyStoreService.findByApiKeyName(apiKey);
  }

  public String generateApiKeyString() {
    return UUID.randomUUID().toString();
  }

  public String generateUserToken(@NonNull User u, @NonNull Set<String> scope) {
    val tokenClaims = generateUserTokenClaims(u, scope);
    return getSignedToken(tokenClaims);
  }

  public UserTokenClaims generateUserTokenClaims(@NonNull User u, @NonNull Set<String> scope) {
    val tokenContext = new UserTokenContext(u);
    tokenContext.setScope(scope);
    val tokenClaims = new UserTokenClaims();
    tokenClaims.setIss(ISSUER_NAME);
    tokenClaims.setValidDuration(JWT_DURATION);
    tokenClaims.setContext(tokenContext);

    return tokenClaims;
  }

  @SneakyThrows
  public String generateAppToken(Application application) {
    val permissionNames = extractExplicitScopes(application);
    val tokenContext = new AppTokenContext(application);
    val tokenClaims = new AppTokenClaims();
    tokenContext.setScope(permissionNames);
    tokenClaims.setIss(ISSUER_NAME);
    tokenClaims.setValidDuration(JWT_DURATION);
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

  public Jws<Claims> validateAndReturn(String token) {
    Jws<Claims> decodedToken = null;
    try {
      decodedToken = Jwts.parser().setSigningKey(tokenSigner.getKey().get()).parseClaimsJws(token);
    } catch (JwtException e) {
      log.error("JWT token is invalid", e);
      throw new ForbiddenException("Authorization is required for this action.");
    }
    if (decodedToken == null) {
      log.error("JWT token was null when trying to validate and return.");
      throw new ForbiddenException("Authorization is required for this action.");
    }

    return decodedToken;
  }

  public User getTokenUserInfo(String token) {
    try {
      val body = getTokenClaims(token);
      val tokenClaims =
          convertToAnotherType(body, UserTokenClaims.class, Views.JWTAccessToken.class);
      return userService.getById(fromString(tokenClaims.getSub()));
    } catch (JwtException | ClassCastException | IOException e) {
      log.error("Issue handling user token (MD5sum) {}", new String(md5Digest(token.getBytes())));
      return null;
    }
  }

  public Application getTokenAppInfo(String token) {
    try {
      val body = getTokenClaims(token);
      val tokenClaims =
          convertToAnotherType(body, AppTokenClaims.class, Views.JWTAccessToken.class);
      return applicationService.getById(fromString(tokenClaims.getSub()));
    } catch (JwtException | ClassCastException | IOException e) {
      log.error(
          "Issue handling application token (MD5sum) {}", new String(md5Digest(token.getBytes())));
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

  public Claims getTokenClaimsIgnoreExpiry(String token) {
    try {
      return getTokenClaims(token);
    } catch (ExpiredJwtException exception) {
      val claims = exception.getClaims();
      if (StringUtils.isEmpty(claims.getId())) {
        throw new ForbiddenException("Invalid token claims, cannot refresh expired token.");
      }
      log.info("Refreshing expired token: {}", claims.getId());
      log.debug("Refreshing expired token! ", exception);
      return claims;
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
  public ApiKeyScopeResponse checkApiKey(String authToken, String apiKey) {
    if (apiKey == null) {
      log.debug("Null apiKey");
      throw new InvalidTokenException("No apiKey field found in POST request");
    }

    log.debug(format("apiKey ='%s'", apiKey));
    val contents = BasicAuthToken.decode(authToken);

    val clientId = contents.get().getClientId();
    val application = applicationService.findByClientId(clientId);

    val aK =
        findByApiKeyString(apiKey).orElseThrow(() -> new InvalidTokenException("ApiKey not found"));

    if (aK.isRevoked())
      throw new InvalidTokenException(
          format("ApiKey \"%s\" has expired or is no longer valid. ", apiKey));

    // We want to limit the scopes listed in the apiKey to those scopes that the user
    // is allowed to access at the time the apiKey is checked -- we don't assume that
    // they have not changed since the apiKey was issued.

    val owner = aK.getOwner();
    val scopes = explicitScopes(effectiveScopes(extractScopes(owner), aK.scopes()));
    val names = mapToSet(scopes, Scope::toString);

    return new ApiKeyScopeResponse(owner.getName(), clientId, aK.getSecondsUntilExpiry(), names);
  }

  public UserScopesResponse userScopes(@NonNull String userName) {
    val user = userService.getByName(userName);
    val scopes = extractScopes(user);
    val names = mapToSet(scopes, Scope::toString);

    return new UserScopesResponse(names);
  }

  public void revokeApiKey(@NonNull String apiKeyName) {
    validateApiKeyName(apiKeyName);
    val principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    if (principal instanceof User) {
      revokeApiKeyAsUser(apiKeyName, (User) principal);
    } else if (principal instanceof Application) {
      revokeApiKeyAsApplication(apiKeyName, (Application) principal);
    } else {
      log.info("Unknown type of authentication, apiKey is not allowed to be revoked.");
      throw new InvalidRequestException("Unknown type of authentication.");
    }
  }

  private void revokeApiKeyAsUser(String apiKeyName, User user) {
    if (userService.isAdmin(user) && userService.isActiveUser(user)) {
      revoke(apiKeyName);
    } else {
      // if it's a regular user, check if the api key belongs to the user
      verifyApiKey(apiKeyName, user.getId());
      revoke(apiKeyName);
    }
  }

  private void revokeApiKeyAsApplication(String apiKeyName, Application application) {
    if (application.getType() == ADMIN) {
      revoke(apiKeyName);
    } else {
      throw new InvalidRequestException(
          format("The application does not have permission to revoke apiKey '%s'", apiKeyName));
    }
  }

  private void verifyApiKey(String apiKey, UUID userId) {
    val currentApiKey =
        findByApiKeyString(apiKey)
            .orElseThrow(() -> new InvalidTokenException("ApiKey not found."));

    if (!currentApiKey.getOwner().getId().equals(userId)) {
      throw new InvalidTokenException("Users can only revoke apiKeys that belong to them.");
    }
  }

  private void validateApiKeyName(@NonNull String apiKeyName) {
    log.info(format("Validating apiKey: '%s'.", apiKeyName));

    if (apiKeyName.isEmpty()) {
      throw new InvalidTokenException("ApiKey cannot be empty.");
    }

    if (apiKeyName.length() > 2048) {
      throw new InvalidRequestException(
          "Invalid apiKey, the maximum length for an apiKey is 2048.");
    }
  }

  public void revoke(String apiKey) {
    val currentApiKey =
        findByApiKeyString(apiKey)
            .orElseThrow(() -> new InvalidTokenException("ApiKey not found."));
    if (currentApiKey.isRevoked()) {
      throw new InvalidTokenException(format("ApiKey '%s' is already revoked.", apiKey));
    }
    currentApiKey.setRevoked(true);
    getRepository().save(currentApiKey);
  }

  public List<ApiKeyResponse> listApiKey(@NonNull UUID userId) {
    return getApiKeysForUser(userId).stream()
        .filter((token -> !token.isRevoked()))
        .map(this::createApiKeyResponse)
        .collect(Collectors.toList());
  }

  public Page<ApiKeyResponse> listApiKeysForUser(
      @NonNull UUID userId, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {

    checkEntityExistence(User.class, userRepository, userId);

    val apiKeys =
        (Page<ApiKey>)
            getRepository()
                .findAll(
                    where(TokenStoreSpecification.containsUser(userId))
                        .and(TokenStoreSpecification.filterBy(filters)),
                    pageable);

    val apiKeyResponses =
        ImmutableList.copyOf(apiKeys).stream()
            .map(this::createApiKeyResponse)
            .collect(Collectors.toList());

    return new PageImpl<>(apiKeyResponses, pageable, apiKeys.getTotalElements());
  }

  public Page<ApiKeyResponse> findApiKeysForUser(
      @NonNull UUID userId, String query, List<SearchFilter> filters, @NonNull Pageable pageable) {
    checkEntityExistence(User.class, userRepository, userId);
    val apiKeys =
        (Page<ApiKey>)
            getRepository()
                .findAll(
                    where(TokenStoreSpecification.containsUser(userId))
                        .and(TokenStoreSpecification.containsText(query))
                        .and(TokenStoreSpecification.filterBy(filters)),
                    pageable);

    val apiKeyResponses =
        ImmutableList.copyOf(apiKeys).stream()
            .map(this::createApiKeyResponse)
            .collect(Collectors.toList());

    return new PageImpl<>(apiKeyResponses, pageable, apiKeys.getTotalElements());
  }
  /** DEPRECATED: To be removed in next major release */
  @Deprecated
  public List<TokenResponse> listTokens(@NonNull UUID userId) {
    return getApiKeysForUser(userId).stream()
        .filter((token -> !token.isRevoked()))
        .map(this::createTokenResponse)
        .collect(Collectors.toList());
  }

  private Set<ApiKey> getApiKeysForUser(@NonNull UUID userId) {
    val user =
        userService
            .findById(userId)
            .orElseThrow(
                () -> new UsernameNotFoundException(format("Can't find user '%s'", str(userId))));
    return user.getTokens();
  }

  private ApiKeyResponse createApiKeyResponse(@NonNull ApiKey apiKey) {
    val scopes = mapToSet(apiKey.scopes(), Scope::toString);
    return ApiKeyResponse.builder()
        .name(apiKey.getName())
        .scope(scopes)
        .description(apiKey.getDescription())
        .issueDate(apiKey.getIssueDate())
        .expiryDate(apiKey.getExpiryDate())
        .isRevoked(apiKey.isRevoked())
        .build();
  }

  private static Set<String> extractExplicitScopes(User u){
    return mapToSet(explicitScopes(extractScopes(u)), Scope::toString);
  }

  private static Set<String> extractExplicitScopes(Application a){
    return mapToSet(explicitScopes(extractScopes(a)), Scope::toString);
  }

  /** DEPRECATED: To be removed in next major release */
  @Deprecated
  private TokenResponse createTokenResponse(@NonNull ApiKey token) {
    val scopes = mapToSet(token.scopes(), Scope::toString);
    return TokenResponse.builder()
        .accessToken(token.getName())
        .scope(scopes)
        .exp(token.getSecondsUntilExpiry())
        .description(token.getDescription())
        .build();
  }
}
