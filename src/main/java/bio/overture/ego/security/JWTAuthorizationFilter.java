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

package bio.overture.ego.security;

import static bio.overture.ego.utils.TypeUtils.convertToAnotherType;
import static org.springframework.util.DigestUtils.md5Digest;

import bio.overture.ego.model.exceptions.ForbiddenException;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.token.app.AppTokenClaims;
import bio.overture.ego.token.user.UserTokenClaims;
import bio.overture.ego.view.Views;
import java.util.ArrayList;
import java.util.Arrays;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
public class JWTAuthorizationFilter extends OncePerRequestFilter {

  private String[] publicEndpoints = null;

  @Value("${auth.token.prefix}")
  private String TOKEN_PREFIX = "Bearer";

  @Autowired private TokenService tokenService;
  @Autowired private ApplicationService applicationService;

  public JWTAuthorizationFilter(String[] publicEndpoints) {
    this.publicEndpoints = publicEndpoints;
  }

  public JWTAuthorizationFilter(
      String[] publicEndpoints, TokenService tokenService, ApplicationService applicationService) {
    this.publicEndpoints = publicEndpoints;
    this.tokenService = tokenService;
    this.applicationService = applicationService;
  }

  @Override
  @SneakyThrows
  public void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain) {

    if (isPublicEndpoint(request.getServletPath())) {
      chain.doFilter(request, response);
      return;
    }
    val tokenPayload = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (tokenPayload != null && tokenPayload.startsWith(BasicAuthToken.TOKEN_PREFIX)) {
      authenticateApplication(tokenPayload);
    } else {
      authenticateUserOrApplication(tokenPayload);
    }
    chain.doFilter(request, response);
  }

  /**
   * Responsible for authenticating a Bearer JWT into a user or application.
   *
   * @param tokenPayload The string representation of the Authorization Header with the token prefix
   *     included
   */
  protected void authenticateUserOrApplication(String tokenPayload) {
    if (!isValidToken(tokenPayload)) {
      log.warn(
          "Invalid token (MD5sum): {}",
          tokenPayload == null ? "null token" : new String(md5Digest(tokenPayload.getBytes())));
      SecurityContextHolder.clearContext();
      return;
    }

    UsernamePasswordAuthenticationToken authentication = null;
    val body = tokenService.getTokenClaims(removeTokenPrefix(tokenPayload));
    try {
      // Test Conversion
      convertToAnotherType(body, UserTokenClaims.class, Views.JWTAccessToken.class);
      authentication =
          new UsernamePasswordAuthenticationToken(
              tokenService.getTokenUserInfo(removeTokenPrefix(tokenPayload)),
              null,
              new ArrayList<>());
      SecurityContextHolder.getContext().setAuthentication(authentication);
      return; // Escape
    } catch (Exception e) {
      log.debug(e.getMessage());
      log.warn("Token is valid but not a User JWT");
    }

    try {
      // Test Conversion
      convertToAnotherType(body, AppTokenClaims.class, Views.JWTAccessToken.class);
      authentication =
          new UsernamePasswordAuthenticationToken(
              tokenService.getTokenAppInfo(removeTokenPrefix(tokenPayload)),
              null,
              new ArrayList<>());
      SecurityContextHolder.getContext().setAuthentication(authentication);
      return; // Escape
    } catch (Exception e) {
      log.debug(e.getMessage());
      log.warn("Token is valid but not an Application JWT");
    }

    throw new ForbiddenException("Bad Token");
  }

  protected void authenticateApplication(String tokenString) {
    // An application just sends us Basic authToken (an id+a secret) to authenticate itself.
    val token = BasicAuthToken.decode(tokenString);

    if (token.isEmpty()) {
      log.warn("AuthenticateApplication: Invalid token for application authentication request");
      SecurityContextHolder.clearContext();
      return;
    }

    val clientId = token.get().getClientId();
    val clientSecret = token.get().getClientSecret();

    // Deny access if they don't have a valid clientId from one of our applications
    val applicationOpt = applicationService.getClientApplication(clientId);

    if (!applicationOpt.isPresent() || applicationOpt.isEmpty()) {
      SecurityContextHolder.clearContext();
      log.warn("AuthenticateApplication: No application found for clientId " + clientId);
      return;
    }
    val application = applicationOpt.get();
    // Deny access if the clientSecret in the token is wrong
    if (!application.getClientSecret().equals(clientSecret)) {
      SecurityContextHolder.clearContext();
      log.warn(
          "AuthenticateApplication: Wrong client secret for clientId '"
              + clientId
              + "' from token '"
              + tokenString
              + "'");
      return;
    }

    val authentication =
        new UsernamePasswordAuthenticationToken(application, null, new ArrayList<>());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private boolean isValidToken(String token) {
    return StringUtils.hasText(token)
        && token.contains(TOKEN_PREFIX)
        && tokenService.isValidToken(removeTokenPrefix(token));
  }

  private String removeTokenPrefix(String token) {
    return token.replace(TOKEN_PREFIX, "").trim();
  }

  private boolean isPublicEndpoint(String endpointPath) {
    if (this.publicEndpoints != null) {
      return Arrays.stream(this.publicEndpoints).anyMatch(item -> item.equals(endpointPath));
    } else return false;
  }
}
