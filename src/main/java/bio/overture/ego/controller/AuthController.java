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

import bio.overture.ego.provider.facebook.FacebookTokenService;
import bio.overture.ego.provider.github.GithubOAuthService;
import bio.overture.ego.provider.google.GoogleTokenService;
import bio.overture.ego.provider.linkedin.LinkedInOAuthService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.token.signer.TokenSigner;
import javax.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@Slf4j
@RestController
@RequestMapping("/oauth")
@AllArgsConstructor(onConstructor = @__({@Autowired}))
public class AuthController {
  private TokenService tokenService;
  private GoogleTokenService googleTokenService;
  private FacebookTokenService facebookTokenService;
  private TokenSigner tokenSigner;
  private LinkedInOAuthService linkedInOAuthService;
  private GithubOAuthService githubOAuthService;

  @RequestMapping(method = RequestMethod.GET, value = "/google/token")
  @ResponseStatus(value = HttpStatus.OK)
  @SneakyThrows
  public @ResponseBody String exchangeGoogleTokenForAuth(
      @RequestHeader(value = "token") final String idToken) {
    if (!googleTokenService.validToken(idToken))
      throw new InvalidTokenException("Invalid user token:" + idToken);
    val authInfo = googleTokenService.decode(idToken);
    return tokenService.generateUserToken(authInfo);
  }

  @RequestMapping(method = RequestMethod.GET, value = "/facebook/token")
  @ResponseStatus(value = HttpStatus.OK)
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

  @RequestMapping(method = RequestMethod.GET, value = "/linkedin-cb")
  @SneakyThrows
  public RedirectView linkedinCallback(
      @RequestParam("code") String code,
      RedirectAttributes attributes,
      @Value("${oauth.redirectFrontendUri}") final String redirectFrontendUri) {
    val redirectView = new RedirectView();

    redirectView.setUrl(redirectFrontendUri);
    val authInfo = linkedInOAuthService.getAuthInfo(code);
    if (authInfo.isPresent()) {
      attributes.addAttribute("token", tokenService.generateUserToken(authInfo.get()));
      return redirectView;
    } else {
      throw new InvalidTokenException("Unable to generate auth token for this user");
    }
  }

  @RequestMapping(method = RequestMethod.GET, value = "/github-cb")
  @SneakyThrows
  public RedirectView githubCallback(
      @RequestParam("code") String code,
      RedirectAttributes attributes,
      @Value("${oauth.redirectFrontendUri}") final String redirectFrontendUri) {
    val redirectView = new RedirectView();

    redirectView.setUrl(redirectFrontendUri);
    val authInfo = githubOAuthService.getAuthInfo(code);
    if (authInfo.isPresent()) {
      attributes.addAttribute("token", tokenService.generateUserToken(authInfo.get()));
      return redirectView;
    } else {
      throw new InvalidTokenException("Unable to generate auth token for this user");
    }
  }

  @RequestMapping(method = RequestMethod.GET, value = "/token/verify")
  @ResponseStatus(value = HttpStatus.OK)
  @SneakyThrows
  public @ResponseBody boolean verifyJWToken(@RequestHeader(value = "token") final String token) {
    if (StringUtils.isEmpty(token)) {
      throw new InvalidTokenException("ScopedAccessToken is empty");
    }

    if (!tokenService.validateToken(token)) {
      throw new InvalidTokenException("ScopedAccessToken failed validation");
    }
    return true;
  }

  @RequestMapping(method = RequestMethod.GET, value = "/token/public_key")
  @ResponseStatus(value = HttpStatus.OK)
  public @ResponseBody String getPublicKey() {
    val pubKey = tokenSigner.getEncodedPublicKey();
    if (pubKey.isPresent()) {
      return pubKey.get();
    } else {
      return "";
    }
  }

  @ExceptionHandler({InvalidTokenException.class})
  public ResponseEntity<Object> handleInvalidTokenException(
      HttpServletRequest req, InvalidTokenException ex) {
    log.error("InvalidTokenException: %s".format(ex.getMessage()));
    log.error("ID ScopedAccessToken not found.");
    return new ResponseEntity<Object>(
        "Invalid ID ScopedAccessToken provided.", new HttpHeaders(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({InvalidScopeException.class})
  public ResponseEntity<Object> handleInvalidScopeException(
      HttpServletRequest req, InvalidTokenException ex) {
    log.error("Invalid ScopeName: %s".format(ex.getMessage()));
    return new ResponseEntity<Object>(
        "{\"error\": \"%s\"}".format(ex.getMessage()), HttpStatus.BAD_REQUEST);
  }
}
