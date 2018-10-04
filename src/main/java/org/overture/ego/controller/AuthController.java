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

package org.overture.ego.controller;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.overture.ego.model.entity.User;
import org.overture.ego.provider.facebook.FacebookTokenService;
import org.overture.ego.provider.google.GoogleTokenService;
import org.overture.ego.service.UserService;
import org.overture.ego.token.TokenService;
import org.overture.ego.token.signer.TokenSigner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/oauth")
@AllArgsConstructor(onConstructor = @__({@Autowired}))
public class AuthController {
  private TokenService tokenService;
  private GoogleTokenService googleTokenService;
  private FacebookTokenService facebookTokenService;
  private TokenSigner tokenSigner;
  private UserService userService;

  @RequestMapping(method = RequestMethod.GET, value = "/google/token")
  @ResponseStatus(value = HttpStatus.OK)
  @SneakyThrows
  public @ResponseBody
  String exchangeGoogleTokenForAuth(
      @RequestHeader(value = "token") final String idToken) {
    if (!googleTokenService.validToken(idToken))
      throw new InvalidTokenException("Invalid user token:" + idToken);
    val authInfo = googleTokenService.decode(idToken);
    return tokenService.generateUserToken(authInfo);
  }

  @RequestMapping(method = RequestMethod.GET, value = "/facebook/token")
  @ResponseStatus(value = HttpStatus.OK)
  @SneakyThrows
  public @ResponseBody
  String exchangeFacebookTokenForAuth(
          @RequestHeader(value = "token") final String idToken) {
    if (!facebookTokenService.validToken(idToken))
      throw new InvalidTokenException("Invalid user token:" + idToken);
    val authInfo = facebookTokenService.getAuthInfo(idToken);
    if(authInfo.isPresent()) {
      return tokenService.generateUserToken(authInfo.get());
    } else {
      throw new InvalidTokenException("Unable to generate auth token for this user");
    }
  }

  @RequestMapping(method = RequestMethod.POST, value = "/user/{id}/authToken")
  @ResponseStatus(value = HttpStatus.OK)
  @SneakyThrows
  public @ResponseBody
  String issueToken(
    @RequestHeader(value = HttpHeaders.AUTHORIZATION) final String accessToken,
    @PathVariable(value = "id") UUID id,
    @RequestBody() Set<String> scopes
    ) {
    User u = userService.get(id.toString());
    val userScopes = u.getScopes();
    if (!userScopes.containsAll(scopes)) {
      scopes.removeAll(userScopes);
      throw new InvalidScopeException(
        "User %s does not have permission to access scope(s) %s".
          format(u.getId().toString(), scopes));
    }

    return tokenService.generateUserToken(u, scopes);
  }

  @RequestMapping(method = RequestMethod.GET, value = "/user/{id}/scopes")
  @ResponseStatus(value = HttpStatus.OK)
  @SneakyThrows
  public @ResponseBody
  String getScopes(
    @RequestHeader(value = HttpHeaders.AUTHORIZATION) final String accessToken,
    @PathVariable(value = "id") UUID id
  ) {
    User u = userService.get(id.toString());
    val userScopes = u.getScopes();
    return userScopes.toString();
  }

  @RequestMapping(method = RequestMethod.GET, value = "/token/verify")
  @ResponseStatus(value = HttpStatus.OK)
  @SneakyThrows
  public @ResponseBody
  boolean verifyJWToken(
      @RequestHeader(value = "token") final String token) {
    if (StringUtils.isEmpty(token))  {
      throw new InvalidTokenException("Token is empty");
    }

    if ( ! tokenService.validateToken(token) ) {
      throw new InvalidTokenException("Token failed validation");
    }
    return true;
  }

  @RequestMapping(method = RequestMethod.GET, value = "/token/public_key")
  @ResponseStatus(value = HttpStatus.OK)
  public @ResponseBody
  String getPublicKey() {
    val pubKey = tokenSigner.getEncodedPublicKey();
    if(pubKey.isPresent()){
      return pubKey.get();
    } else {
      return "";
    }
  }

  @ExceptionHandler({ InvalidTokenException.class })
  public ResponseEntity<Object> handleInvalidTokenException(HttpServletRequest req, InvalidTokenException ex) {
    log.error("ID Token not found.");
    return new ResponseEntity<Object>("Invalid ID Token provided.", new HttpHeaders(),
        HttpStatus.BAD_REQUEST);
  }

}
