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
import static bio.overture.ego.utils.SwaggerConstants.POST_ACCESS_TOKEN;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import bio.overture.ego.model.enums.ProviderType;
import bio.overture.ego.provider.facebook.FacebookTokenService;
import bio.overture.ego.provider.google.GoogleTokenService;
import bio.overture.ego.security.CustomOAuth2User;
import bio.overture.ego.service.InvalidScopeException;
import bio.overture.ego.service.InvalidTokenException;
import bio.overture.ego.service.RefreshContextService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.token.IDToken;
import bio.overture.ego.token.signer.TokenSigner;
import bio.overture.ego.utils.Tokens;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.security.Principal;
import java.util.Map;
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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@Slf4j
@RestController
@RequestMapping("/oauth")
@Api(tags = "Auth", value = AUTH_CONTROLLER)
public class AuthController {

  @Value("${auth.token.prefix}")
  private String TOKEN_PREFIX;

  private final TokenService tokenService;
  private final GoogleTokenService googleTokenService;
  private final FacebookTokenService facebookTokenService;
  private final TokenSigner tokenSigner;
  private final RefreshContextService refreshContextService;
  //  private final TokenEndpoint tokenEndpoint;

  @Autowired
  public AuthController(
      //      @NonNull TokenEndpoint tokenEndpoint,
      @NonNull TokenService tokenService,
      @NonNull GoogleTokenService googleTokenService,
      @NonNull FacebookTokenService facebookTokenService,
      @NonNull TokenSigner tokenSigner,
      @NonNull RefreshContextService refreshContextService) {
    this.tokenService = tokenService;
    this.googleTokenService = googleTokenService;
    this.facebookTokenService = facebookTokenService;
    this.tokenSigner = tokenSigner;
    this.refreshContextService = refreshContextService;
    //    this.tokenEndpoint = tokenEndpoint;
  }

  // This spring tokenEndpoint controller is proxied so that Springfox can include this in the
  // swagger-ui under the Auth controller
  @ApiOperation(value = POST_ACCESS_TOKEN)
  @RequestMapping(value = "/token", method = RequestMethod.POST)
  public ResponseEntity<OAuth2AccessToken> postAccessToken(
      Principal principal, @RequestParam Map<String, String> parameters)
      throws HttpRequestMethodNotSupportedException {
    //    return this.tokenEndpoint.postAccessToken(principal, parameters);
    throw new RuntimeException("Not supported");
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
    if (authentication.getPrincipal() == null) {
      throw new RuntimeException("no user ");
    }
    CustomOAuth2User user;
    if (authentication.getPrincipal() instanceof OidcUser) {
      val p = (OidcUser) authentication.getPrincipal();
      user = CustomOAuth2User.builder()
          .subjectId(p.getSubject())
          .givenName(p.getGivenName())
          .familyName(p.getFamilyName())
          .email(p.getEmail())
          .oauth2User(p)
          .build();
    } else if (authentication.getPrincipal() instanceof CustomOAuth2User) {
      user = (CustomOAuth2User) authentication.getPrincipal();
    } else {
      throw new RuntimeException();
    }

    String token = tokenService.generateUserToken(IDToken.builder()
      .providerSubjectId(user.getSubjectId())
      .email(user.getEmail())
      .familyName(user.getFamilyName())
      .givenName(user.getGivenName())
      .providerType(ProviderType.resolveProviderType(authentication.getAuthorizedClientRegistrationId()))
      .build()
    );

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
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization) {
    val currentToken = Tokens.removeTokenPrefix(authorization, TOKEN_PREFIX);
    return new ResponseEntity<>(tokenService.updateUserToken(currentToken), OK);
  }

  @RequestMapping(method = DELETE, value = "/refresh")
  public ResponseEntity<String> deleteRefreshToken(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
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
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
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
