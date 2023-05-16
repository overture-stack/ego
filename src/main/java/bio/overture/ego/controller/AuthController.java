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

package bio.overture.ego.controller;

import static bio.overture.ego.model.enums.JavaFields.REFRESH_ID;
import static bio.overture.ego.utils.SwaggerConstants.AUTH_CONTROLLER;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import bio.overture.ego.model.enums.ProviderType;
import bio.overture.ego.model.exceptions.InvalidScopeException;
import bio.overture.ego.model.exceptions.InvalidTokenException;
import bio.overture.ego.provider.google.GoogleTokenService;
import bio.overture.ego.security.CustomOAuth2User;
import bio.overture.ego.service.PassportService;
import bio.overture.ego.service.RefreshContextService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.token.IDToken;
import bio.overture.ego.token.signer.TokenSigner;
import bio.overture.ego.utils.Tokens;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Objects;

import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/oauth")
@Tag(name = "Auth", description = AUTH_CONTROLLER)
public class AuthController {

  @Value("${auth.token.prefix}")
  private String TOKEN_PREFIX;

  private final TokenService tokenService;
  private final GoogleTokenService googleTokenService;
  private final TokenSigner tokenSigner;
  private final RefreshContextService refreshContextService;
  private final PassportService passportService;

  @Autowired
  public AuthController(
          @NonNull TokenService tokenService,
          @NonNull GoogleTokenService googleTokenService,
          @NonNull TokenSigner tokenSigner,
          @NonNull RefreshContextService refreshContextService,
          @NotNull PassportService passportService) {
    this.tokenService = tokenService;
    this.googleTokenService = googleTokenService;
    this.tokenSigner = tokenSigner;
    this.refreshContextService = refreshContextService;
    this.passportService = passportService;
  }

  @RequestMapping(method = GET, value = "/google/token")
  @ResponseStatus(value = OK)
  @SneakyThrows
  public @ResponseBody String exchangeGoogleTokenForAuth(
      @RequestHeader(value = "token") final String idToken) {
    if (!googleTokenService.validToken(idToken))
      throw new InvalidTokenException("Invalid user token:" + idToken);
    val authInfo = googleTokenService.decode(idToken);
    passportService.validatePassportPermissions(idToken);
    return tokenService.generateUserToken(authInfo);
  }

  @RequestMapping(method = GET, value = "/token/verify")
  @ResponseStatus(value = OK)
  @SneakyThrows
  public @ResponseBody boolean verifyJWToken(@RequestHeader(value = "token") final String token) {
    if (!StringUtils.hasText(token)) {
      throw new InvalidTokenException("ScopedAccessToken is empty");
    }

    if (!tokenService.isValidToken(token)) {
      throw new InvalidTokenException("ScopedAccessToken failed validation");
    }
    return true;
  }

  @ResponseStatus(value = OK)
  @RequestMapping(method = GET, value = "/token/public_key", produces = TEXT_PLAIN_VALUE)
  public @ResponseBody String getPublicKey() {
    val pubKey = tokenSigner.getEncodedPublicKey();
    return pubKey.orElse("");
  }

  @RequestMapping(
      method = {GET, POST},
      value = "/ego-token")
  @SneakyThrows
  public ResponseEntity<String> user(
      OAuth2AuthenticationToken authentication, HttpServletResponse response) {
    if (authentication == null) {
      return new ResponseEntity<>("Please login", UNAUTHORIZED);
    }
    if (Objects.isNull(authentication.getPrincipal())) {
      throw new RuntimeException("no user");
    }

    val user = (CustomOAuth2User) authentication.getPrincipal();
    String token =
        tokenService.generateUserToken(
            IDToken.builder()
                .providerSubjectId(user.getSubjectId())
                .email(user.getEmail())
                .familyName(user.getFamilyName())
                .givenName(user.getGivenName())
                .providerType(
                    ProviderType.resolveProviderType(
                        authentication.getAuthorizedClientRegistrationId()))
                .build());

    val outgoingRefreshContext = refreshContextService.createInitialRefreshContext(token);
    val cookie =
        refreshContextService.createRefreshCookie(outgoingRefreshContext.getRefreshToken());
    response.addCookie(cookie);

    SecurityContextHolder.getContext().setAuthentication(null);
    return new ResponseEntity<>(token, OK);
  }

  @RequestMapping(
      method = {GET, POST},
      value = "/update-ego-token")
  public ResponseEntity<String> updateEgoToken(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization) {
    val currentToken = Tokens.removeTokenPrefix(authorization, TOKEN_PREFIX);
    return new ResponseEntity<>(tokenService.updateUserToken(currentToken), OK);
  }

  @RequestMapping(method = DELETE, value = "/refresh")
  public ResponseEntity<String> deleteRefreshToken(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @CookieValue(value = REFRESH_ID, defaultValue = "missing") String refreshId,
      HttpServletResponse response) {

    if (authorization == null || refreshId.equals("missing")) {
      return new ResponseEntity<>("Please login", UNAUTHORIZED);
    }
    val cookieToRemove = refreshContextService.deleteRefreshTokenAndCookie(refreshId);
    response.addCookie(cookieToRemove);

    return new ResponseEntity<>("User is logged out", OK);
  }

  @RequestMapping(method = POST, value = "/refresh")
  public ResponseEntity<String> refreshEgoToken(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @CookieValue(value = REFRESH_ID, defaultValue = "missing") String refreshId,
      HttpServletResponse response) {
    if (authorization == null || refreshId.equals("missing")) {
      return new ResponseEntity<>("Please login", UNAUTHORIZED);
    }
    val currentToken = Tokens.removeTokenPrefix(authorization, TOKEN_PREFIX);
    // TODO: [anncatton] validate jwt before proceeding to service call.

    val outboundUserToken =
        refreshContextService.validateAndReturnNewUserToken(refreshId, currentToken);
    val newRefreshToken = tokenService.getTokenUserInfo(outboundUserToken).getRefreshToken();
    val newCookie = refreshContextService.createRefreshCookie(newRefreshToken);
    response.addCookie(newCookie);

    return new ResponseEntity<>(outboundUserToken, OK);
  }

  @ExceptionHandler({InvalidTokenException.class})
  public ResponseEntity<Object> handleInvalidTokenException(InvalidTokenException ex) {
    log.error(String.format("InvalidTokenException: %s", ex.getMessage()));
    log.error("ID ScopedAccessToken not found.");
    return new ResponseEntity<>(
        "Invalid ID ScopedAccessToken provided.", new HttpHeaders(), BAD_REQUEST);
  }

  @ExceptionHandler({InvalidScopeException.class})
  public ResponseEntity<Object> handleInvalidScopeException(InvalidTokenException ex) {
    log.error(String.format("Invalid ScopeName: %s", ex.getMessage()));
    return new ResponseEntity<>(String.format("{\"error\": \"%s\"}", ex.getMessage()), BAD_REQUEST);
  }
}
