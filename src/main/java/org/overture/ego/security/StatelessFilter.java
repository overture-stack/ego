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
import org.overture.ego.token.JWTAuthenticationToken;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Order(Ordered.HIGHEST_PRECEDENCE)
public class StatelessFilter extends AbstractAuthenticationProcessingFilter {

  public StatelessFilter(String defaultFilterProcessesUrl) {
    super(defaultFilterProcessesUrl);
    setAuthenticationManager(new NoopAuthenticationManager());
  }

  @Override
  @SneakyThrows
  public Authentication attemptAuthentication(HttpServletRequest request,
                                              HttpServletResponse response) {
    String tokenPayload = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (tokenPayload == null || tokenPayload.isEmpty()) return null;
    return new JWTAuthenticationToken(tokenPayload);
  }

  private static class NoopAuthenticationManager implements AuthenticationManager {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
      throw new UnsupportedOperationException("No authentication should be done with this AuthenticationManager");
    }

  }

}
