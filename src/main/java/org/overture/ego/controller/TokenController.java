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
import org.overture.ego.model.dto.TokenResponse;
import org.overture.ego.model.dto.TokenScope;
import org.overture.ego.model.entity.ScopedAccessToken;
import org.overture.ego.security.ApplicationScoped;
import org.overture.ego.service.ApplicationService;
import org.overture.ego.token.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.lang.String.format;

@Slf4j
@RestController
@RequestMapping("/o")
@AllArgsConstructor(onConstructor = @__({ @Autowired }))
public class TokenController {
  private TokenService tokenService;
  private ApplicationService applicationService;

  String getValue(String content, String key) {
    val lines = content.split("\n");
    for (val l : lines) {
      if (l.startsWith(key)) {
        return l.replaceFirst(key, "");
      }
    }
    return "";
  }

  @ApplicationScoped()
  @RequestMapping(method = RequestMethod.POST, value = "/check_token")
  @ResponseStatus(value = HttpStatus.MULTI_STATUS)
  @SneakyThrows
  public @ResponseBody
  TokenScope checkToken(
    @RequestHeader(value = "Authorization") final String authToken,
    @RequestParam(value = "token") final String token) {
    if (token == null) {
      throw new InvalidTokenException("No token field found in POST request");
    }
    log.error(format("token='%s'",token));
    val application = applicationService.findByBasicToken(authToken);

    ScopedAccessToken t = tokenService.findByTokenString(token);
    if (t == null) {
      throw new InvalidTokenException("Token not found");
    }

    val clientId = application.getClientId();
    val apps = t.getApplications();
    if (apps != null && !apps.isEmpty() && !apps.stream().anyMatch(app -> app.getClientId() == clientId)) {
      throw new InvalidTokenException("Token not authorized for this client");
    }

    return new TokenScope(t.getOwner().getName(), clientId,
      t.getSecondsUntilExpiry(), t.getScope());
  }

  @RequestMapping(method = RequestMethod.POST, value = "/token")
  @ResponseStatus(value = HttpStatus.OK)
  public @ResponseBody
  TokenResponse issueToken(
    @RequestHeader(value = "Authorization") final String authorization,
    @RequestParam(value = "name")String name,
    @RequestParam(value = "scopes") ArrayList<String> scopes,
    @RequestParam(value = "applications", required = false) ArrayList<String> applications) {
    val t = tokenService.issueToken(name, toSet(scopes), toSet(applications));
    TokenResponse response = new TokenResponse(t.getToken(), t.getScope(), t.getSecondsUntilExpiry());
    return response;
  }

  private Set<String> toSet(Collection<String> collection) {
    if (collection == null) {
      return new HashSet<>();
    } else {
      return new HashSet<>(collection);
    }
  }

  @ExceptionHandler({ InvalidTokenException.class })
  public ResponseEntity<Object> handleInvalidTokenException(HttpServletRequest req, InvalidTokenException ex) {
    log.error(format("ID ScopedAccessToken not found.:%s",ex.toString()));
    return new ResponseEntity<>(format("{\"error\": \"Invalid ID ScopedAccessToken provided:'%s'\"}",
      ex.toString()), new HttpHeaders(),
      HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({ InvalidScopeException.class })
  public ResponseEntity<Object> handleInvalidScopeException(HttpServletRequest req, InvalidTokenException ex) {
    log.error(format("Invalid Scope: %s",ex.getMessage()));
    return new ResponseEntity<>("{\"error\": \"%s\"}".format(ex.getMessage()),
      HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler({ UsernameNotFoundException.class })
  public ResponseEntity<Object> handleUserNotFoundException(HttpServletRequest req, InvalidTokenException ex) {
    log.error(format("User not found: %s",ex.getMessage()));
    return new ResponseEntity<>("{\"error\": \"%s\"}".format(ex.getMessage()),
      HttpStatus.BAD_REQUEST);
  }

}
