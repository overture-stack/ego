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

package org.overture.ego.security;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.overture.ego.service.ApplicationService;
import org.overture.ego.token.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.StringUtils;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

@Slf4j
public class JWTAuthorizationFilter extends BasicAuthenticationFilter {

  private String[] publicEndpoints = null;

  @Value("${auth.token.prefix}")
  private String TOKEN_PREFIX;

  @Autowired
  private TokenService tokenService;
  @Autowired
  private ApplicationService applicationService;


  public JWTAuthorizationFilter(AuthenticationManager authManager, String[] publicEndpoints) {
    super(authManager);
    this.publicEndpoints = publicEndpoints;
  }

  @Override
  @SneakyThrows
  public void doFilterInternal(HttpServletRequest request,
                               HttpServletResponse response,
                               FilterChain chain) {

    if (isPublicEndpoint(request.getServletPath())) {
      log.error("This is a public Endpoint.");
      chain.doFilter(request, response);
      return;
    }
    val tokenPayload = request.getHeader(HttpHeaders.AUTHORIZATION);
    log.error("Found token payload '%s'".format(tokenPayload));

    if (tokenPayload.startsWith(applicationService.APP_TOKEN_PREFIX)) {
      authenticateApplication(tokenPayload);
    } else {
      authenticateUser(tokenPayload);
    }
    chain.doFilter(request,response);
  }

  private void authenticateUser(String tokenPayload) {
    if (!isValidToken(tokenPayload)) {
      SecurityContextHolder.clearContext();
      return;
    }

   val authentication = new UsernamePasswordAuthenticationToken(
     tokenService.getTokenUserInfo(removeTokenPrefix(tokenPayload)),
     null, new ArrayList<>());

   SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private void authenticateApplication(String token) {
    val application = applicationService.findByBasicToken(token);

    // Deny access if they don't have a valid app token for
    // one of our applications
    if (application == null ) {
      SecurityContextHolder.clearContext();
      return;
    }

    val authentication =
        new UsernamePasswordAuthenticationToken(application,null, new ArrayList<>());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private boolean isValidToken(String token){
    return  !StringUtils.isEmpty(token)  &&
            token.contains(TOKEN_PREFIX) &&
            tokenService.validateToken(removeTokenPrefix(token));
    }

  private String removeTokenPrefix(String token){
    return token.replace(TOKEN_PREFIX,"").trim();
  }

  private boolean isPublicEndpoint(String endpointPath){
    if(this.publicEndpoints != null){
      return Arrays.stream(this.publicEndpoints).anyMatch(item -> item.equals(endpointPath));
    } else return false;
  }

}
