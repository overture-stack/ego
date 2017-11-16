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
import org.overture.ego.provider.facebook.FacebookTokenService;
import org.overture.ego.provider.google.GoogleTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

@Slf4j
@Component
@Primary
public class UserAuthenticationManager implements AuthenticationManager {

  @Autowired
  GoogleTokenService googleTokenService;
  @Autowired
  SimpleDateFormat formatter;
  @Autowired
  FacebookTokenService facebookTokenService;

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

    val provider = request.getParameter("provider");
    val idToken = request.getParameter("id_token");
    String username = "";
    if("google".equals(provider.toLowerCase())){
      username = exchangeGoogleTokenForAuth(idToken);
    } else if ("facebook".equals(provider.toLowerCase())){
      username = exchangeFacebookTokenForAuth(idToken);
    } else return null;

    return new UsernamePasswordAuthenticationToken(
            username,
            null, new ArrayList<>());
  }

  @SneakyThrows
  private String exchangeGoogleTokenForAuth(final String idToken) {
    if (!googleTokenService.validToken(idToken))
      throw new Exception("Invalid user token:" + idToken);
    val authInfo = googleTokenService.decode(idToken);
    return  authInfo.get("email").toString();

  }


  @SneakyThrows
  private String exchangeFacebookTokenForAuth(final String idToken) {
    if (!facebookTokenService.validToken(idToken))
      throw new Exception("Invalid user token:" + idToken);
    val authInfo = facebookTokenService.getAuthInfo(idToken);
    if(authInfo.isPresent()) {
      return  authInfo.get().get("email").toString();
    } else {
      throw new Exception("Unable to generate auth token for this user");
    }

  }
}
