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
import org.overture.ego.service.UserService;
import org.overture.ego.token.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;


import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;

@Slf4j
public class JWTAuthorizationFilter extends BasicAuthenticationFilter {

  @Value("${auth.token.prefix}")
  private String TOKEN_PREFIX;

  @Autowired
  TokenService tokenService;

  @Autowired
  UserService userService;

  public JWTAuthorizationFilter(AuthenticationManager authManager) {
    super(authManager);
  }

  @Override
  @SneakyThrows
  public void doFilterInternal(HttpServletRequest request,
                               HttpServletResponse response,
                               FilterChain chain) {
    String tokenPayload = "";
    if("/oauth/token".equals(request.getServletPath())){
      chain.doFilter(request,response);
      return;
    } else{
      tokenPayload = request.getHeader(HttpHeaders.AUTHORIZATION);
    }
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
