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
import org.overture.ego.token.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.stereotype.Component;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;

@Slf4j
public class JWTAuthorizationFilter extends BasicAuthenticationFilter {

  @Value("${security.token.prefix}")
  private String TOKEN_PREFIX;
  @Autowired
  TokenService tokenService;

  public JWTAuthorizationFilter(AuthenticationManager authManager) {
    super(authManager);
  }

  @Override
  @SneakyThrows
  public void doFilterInternal(HttpServletRequest request,
                               HttpServletResponse response,
                               FilterChain chain) {
    String tokenPayload = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (isValidToken(tokenPayload) == false) {
      SecurityContextHolder.clearContext();
      chain.doFilter(request,response);
      return;
    }
   val authentication =
           new UsernamePasswordAuthenticationToken(
                   tokenService.getUserInfo(removeTokenPrefix(tokenPayload)),
                   null, new ArrayList<>());
   SecurityContextHolder.getContext().setAuthentication(authentication);
   chain.doFilter(request,response);
  }

  private boolean isValidToken(String token){
    return  token != null                &&
            token.isEmpty() == false     &&
            token.contains(TOKEN_PREFIX) &&
            tokenService.validateToken(removeTokenPrefix(token));
    }

  private String removeTokenPrefix(String token){
    return token.replace(TOKEN_PREFIX,"").trim();
  }
}
