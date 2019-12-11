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

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import bio.overture.ego.model.entity.RefreshToken;
import bio.overture.ego.provider.facebook.FacebookTokenService;
import bio.overture.ego.provider.google.GoogleTokenService;
import bio.overture.ego.service.RefreshContextService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.token.IDToken;
import bio.overture.ego.token.signer.TokenSigner;
import bio.overture.ego.utils.Tokens;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/oauth")
public class AuthController {

  @Value("${auth.token.prefix}")
  private String TOKEN_PREFIX;

  private final TokenService tokenService;
  private final GoogleTokenService googleTokenService;
  private final FacebookTokenService facebookTokenService;
  private final TokenSigner tokenSigner;
  private final RefreshContextService refreshContextService;

  @Autowired
  public AuthController(
      @NonNull TokenService tokenService,
      @NonNull GoogleTokenService googleTokenService,
      @NonNull FacebookTokenService facebookTokenService,
      @NonNull TokenSigner tokenSigner,
      RefreshContextService refreshContextService) {
    this.tokenService = tokenService;
    this.googleTokenService = googleTokenService;
    this.facebookTokenService = facebookTokenService;
    this.tokenSigner = tokenSigner;
    this.refreshContextService = refreshContextService;
  }

  @RequestMapping(method = GET, value = "/google/token")
  @ResponseStatus(value = OK)
  @SneakyThrows
  public @ResponseBody String exchangeGoogleTokenForAuth(
      @RequestHeader(value = "token") final String idToken) {
    if (!googleTokenService.validToken(idToken))
      throw new InvalidTokenException("Invalid user token:" + idToken);
    val authInfo = googleTokenService.decode(idToken);
    return tokenService.generateUserToken(authInfo);
  }

  @RequestMapping(method = GET, value = "/facebook/token")
  @ResponseStatus(value = OK)
  @SneakyThrows
  public @ResponseBody String exchangeFacebookTokenForAuth(
      @RequestHeader(value = "token") final String idToken) {
    if (!facebookTokenService.validToken(idToken))
      throw new InvalidTokenException("Invalid user token:" + idToken);
    val authInfo = facebookTokenService.getAuthInfo(idToken);
    if (authInfo.isPresent()) {
      return tokenService.generateUserToken(authInfo.get());
    } else {
      throw new InvalidTokenException("Unable to generate auth token for this user");
    }
  }

  @RequestMapping(method = GET, value = "/token/verify")
  @ResponseStatus(value = OK)
  @SneakyThrows
  public @ResponseBody boolean verifyJWToken(@RequestHeader(value = "token") final String token) {
    if (StringUtils.isEmpty(token)) {
      throw new InvalidTokenException("ScopedAccessToken is empty");
    }

    if (!tokenService.isValidToken(token)) {
      throw new InvalidTokenException("ScopedAccessToken failed validation");
    }
    return true;
  }

  @RequestMapping(method = GET, value = "/token/public_key")
  @ResponseStatus(value = OK)
  public @ResponseBody String getPublicKey() {
    val pubKey = tokenSigner.getEncodedPublicKey();
    return pubKey.orElse("");
  }

  @RequestMapping(
      method = {GET, POST},
      value = "/ego-token")
  @SneakyThrows
  public ResponseEntity<String> user(
      OAuth2Authentication authentication, HttpServletResponse response) {
    if (authentication == null) return new ResponseEntity<>("Please login", UNAUTHORIZED);
    String token = tokenService.generateUserToken((IDToken) authentication.getPrincipal());

    refreshContextService.disassociateUserAndDelete(token);
    RefreshToken refreshToken = refreshContextService.createRefreshToken(token);

    val newRefreshContext =
        refreshContextService.createRefreshContext(refreshToken.getId().toString(), token);

    if (newRefreshContext.hasApprovedUser()) {
      val cookie =
          createCookie(
              "refreshId",
              refreshToken.getId().toString(),
              refreshToken.getSecondsUntilExpiry().intValue());
      response.addCookie(cookie);
    }

    SecurityContextHolder.getContext().setAuthentication(null);
    return new ResponseEntity<>(token, OK);
  }

  @RequestMapping(
      method = {GET, POST},
      value = "/update-ego-token")
  public ResponseEntity<String> updateEgoToken(
      @RequestHeader(value = "Authorization") final String authorization) {
    val currentToken = Tokens.removeTokenPrefix(authorization, TOKEN_PREFIX);
    return new ResponseEntity<>(tokenService.updateUserToken(currentToken), OK);
  }

  @RequestMapping(method = DELETE, value = "/refresh")
  public ResponseEntity<String> deleteRefreshToken(
      @RequestHeader(value = "Authorization") final String authorization,
      @CookieValue(value = "refreshId", defaultValue = "missing") String refreshId,
      HttpServletResponse response,
      HttpServletRequest request) {
    if (authorization == null || refreshId.equals("missing"))
      return new ResponseEntity<>("Please login", UNAUTHORIZED);
    val currentToken = Tokens.removeTokenPrefix(authorization, TOKEN_PREFIX);
    val cookieToRemove = createCookie("refreshId", "", 0);
    response.addCookie(cookieToRemove);

    refreshContextService.disassociateUserAndDelete(currentToken);
    val session = request.getSession(false);
    session.invalidate();
    return new ResponseEntity<>("User is logged out", OK);
  }

  // TODO: [anncatton] add controller tests
  @RequestMapping(method = POST, value = "/refresh")
  public ResponseEntity<String> refreshEgoToken(
      @RequestHeader(value = "Authorization") final String authorization,
      @CookieValue(value = "refreshId", defaultValue = "missing") String refreshId,
      HttpServletResponse response) {
    if (authorization == null || refreshId.equals("missing"))
      return new ResponseEntity<>("Please login", UNAUTHORIZED);
    val currentToken = Tokens.removeTokenPrefix(authorization, TOKEN_PREFIX);
    // TODO: [anncatton] validate jwt before proceeding to service call.

    val incomingRefreshContext =
        refreshContextService.createRefreshContext(refreshId, currentToken);
    val isValid = incomingRefreshContext.validate();

    refreshContextService.disassociateUserAndDelete(currentToken);
    if (isValid) {
      // generate new token + refresh token
      val currentUser = tokenService.getTokenUserInfo(currentToken);
      val newUserToken = tokenService.generateUserToken(currentUser);
      val newRefreshToken = refreshContextService.createRefreshToken(newUserToken);
      val newRefreshId = newRefreshToken.getId().toString();

      val newCookie =
          createCookie(
              "refreshId", newRefreshId, newRefreshToken.getSecondsUntilExpiry().intValue());
      response.addCookie(newCookie);

      return new ResponseEntity<>(newUserToken, OK);
    } else {
      return new ResponseEntity<>(
          format("Unable to refresh authorization, please login"), UNAUTHORIZED);
    }
  }

  private static Cookie createCookie(String cookieName, String cookieValue, Integer maxAge) {
    Cookie cookie = new Cookie(cookieName, cookieValue);

    cookie.setDomain("localhost");
    // disable setSecure while testing locally in browser, or will not set in cookies
    //    cookie.setSecure(true);
    cookie.setHttpOnly(true);
    cookie.setMaxAge(maxAge);
    cookie.setPath("/");

    return cookie;
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
