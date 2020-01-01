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

import static bio.overture.ego.controller.resolver.PageableResolver.*;
import static bio.overture.ego.controller.resolver.PageableResolver.SORTORDER;
import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.MULTI_STATUS;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

import bio.overture.ego.model.dto.*;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.Fields;
import bio.overture.ego.model.exceptions.ForbiddenException;
import bio.overture.ego.model.params.ScopeName;
import bio.overture.ego.model.search.Filters;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.security.ApplicationScoped;
import bio.overture.ego.security.AuthorizationManager;
import bio.overture.ego.service.TokenService;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Slf4j
@RestController
@RequestMapping("/o")
public class ApiKeyController {

  /** Dependencies */
  private final TokenService tokenService;

  private final AuthorizationManager
      authorizationManager; // Need this here due to context sensitive checks

  @Autowired
  public ApiKeyController(
      @NonNull TokenService tokenService, @NonNull AuthorizationManager authorizationManager) {
    this.tokenService = tokenService;
    this.authorizationManager = authorizationManager;
  }

  @ApplicationScoped()
  @RequestMapping(method = POST, value = "/check_api_key")
  @ResponseStatus(value = MULTI_STATUS)
  @SneakyThrows
  public @ResponseBody ApiKeyScopeResponse checkApiKey(
      @RequestHeader(value = "Authorization") final String authToken,
      @RequestParam(value = "apiKey") final String apiKey) {

    return tokenService.checkApiKey(authToken, apiKey);
  }

  /** DEPRECATED: GET /check_token to be removed in next major release */
  @Deprecated
  @ApplicationScoped()
  @RequestMapping(method = POST, value = "/check_token")
  @ResponseStatus(value = MULTI_STATUS)
  @SneakyThrows
  public @ResponseBody ApiKeyScopeResponse checkToken(
      @RequestHeader(value = "Authorization") final String authToken,
      @RequestParam(value = "token") final String token) {

    return tokenService.checkApiKey(authToken, token);
  }

  @RequestMapping(method = GET, value = "/scopes")
  @ResponseStatus(value = OK)
  @SneakyThrows
  public @ResponseBody UserScopesResponse userScope(
      @RequestHeader(value = "Authorization") final String auth,
      @RequestParam(value = "userName") final String userName) {
    return tokenService.userScopes(userName);
  }

  @RequestMapping(method = POST, value = "/api_key")
  @ResponseStatus(value = OK)
  public @ResponseBody ApiKeyResponse issueApiKey(
      @RequestHeader(value = "Authorization") final String authorization,
      @RequestParam(value = "user_id") UUID userId,
      @RequestParam(value = "scopes") ArrayList<String> scopes,
      @RequestParam(value = "description", required = false) String description) {

    checkAdminOrOwner(userId); // side effect check and exception throw

    val scopeNames = mapToList(scopes, ScopeName::new);
    val aK = tokenService.issueApiKey(userId, scopeNames, description);
    Set<String> issuedScopes = mapToSet(aK.scopes(), Scope::toString);
    return ApiKeyResponse.builder()
        .apiKey(aK.getName())
        .scope(issuedScopes)
        .exp(aK.getSecondsUntilExpiry())
        .description(aK.getDescription())
        .build();
  }

  /** DEPRECATED: POST /token to be removed in next major release */
  @Deprecated
  @RequestMapping(method = POST, value = "/token")
  @ResponseStatus(value = OK)
  public @ResponseBody TokenResponse issueToken(
      @RequestHeader(value = "Authorization") final String authorization,
      @RequestParam(value = "user_id") UUID userId,
      @RequestParam(value = "scopes") ArrayList<String> scopes,
      @RequestParam(value = "description", required = false) String description) {

    checkAdminOrOwner(userId); // side effect check and exception throw

    val scopeNames = mapToList(scopes, ScopeName::new);
    val t = tokenService.issueApiKey(userId, scopeNames, description);
    Set<String> issuedScopes = mapToSet(t.scopes(), Scope::toString);
    return TokenResponse.builder()
        .accessToken(t.getName())
        .scope(issuedScopes)
        .exp(t.getSecondsUntilExpiry())
        .description(t.getDescription())
        .build();
  }

  @RequestMapping(method = DELETE, value = "/api_key")
  @ResponseStatus(value = OK)
  public @ResponseBody String revokeApiKey(
      @RequestHeader(value = "Authorization") final String authorization,
      @RequestParam(value = "apiKey") final String apiKey) {
    tokenService.revokeApiKey(apiKey);
    return format("ApiKey '%s' is successfully revoked!", apiKey);
  }

  /** DEPRECATED: DELETE /token to be removed in next major release */
  @Deprecated
  @RequestMapping(method = DELETE, value = "/token")
  @ResponseStatus(value = OK)
  public @ResponseBody String revokeToken(
      @RequestHeader(value = "Authorization") final String authorization,
      @RequestParam(value = "token") final String token) {
    tokenService.revokeApiKey(token);
    return format("Token '%s' is successfully revoked!", token);
  }

  // TODO: paginate response
  @RequestMapping(method = GET, value = "/api_key")
  @ApiImplicitParams({
    @ApiImplicitParam(
        name = Fields.NAME, // TODO: what should this field be?
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Search for ids containing this text"),
    @ApiImplicitParam(
        name = LIMIT,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Number of results to retrieve"),
    @ApiImplicitParam(
        name = OFFSET,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Index of first result to retrieve"),
    @ApiImplicitParam(
        name = SORT,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Field to sort on"),
    @ApiImplicitParam(
        name = SORTORDER,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Sorting order: ASC|DESC. Default order: DESC"),
  })
  @ResponseStatus(value = OK)
  public @ResponseBody PageDTO<ApiKeyResponse> listApiKey(
      @RequestHeader(value = "Authorization") final String authorization,
      @RequestParam(value = "user_id") UUID userId,
      @RequestParam(value = "query", required = false) String query,
      @ApiIgnore @Filters List<SearchFilter> filters,
      Pageable pageable) {
    checkAdminOrOwner(userId);
    //    if (isEmpty(query)) {
    return new PageDTO<>(tokenService.findApiKeysForUser(userId, filters, pageable));
    //    } else {
    //      return new PageDTO<>(tokenService.findApiKeysForUser(userId, query, filters, pageable));
    //    }
    //    return tokenService.listApiKey(userId);
  }

  /** DEPRECATED: GET /token to be removed in next major release */
  @Deprecated
  @RequestMapping(method = GET, value = "/token")
  @ResponseStatus(value = OK)
  public @ResponseBody List<TokenResponse> listToken(
      @RequestHeader(value = "Authorization") final String authorization,
      @RequestParam(value = "user_id") UUID userId) {
    checkAdminOrOwner(userId);
    return tokenService.listTokens(userId);
  }

  @ExceptionHandler({InvalidTokenException.class})
  public ResponseEntity<Object> handleInvalidApiKeyException(
      HttpServletRequest req, InvalidTokenException ex) {
    log.error(format("ID ScopedAccessApiKey not found.:%s", ex.toString()));
    return errorResponse(UNAUTHORIZED, "Invalid apiKey: %s", ex);
  }

  @ExceptionHandler({InvalidScopeException.class})
  public ResponseEntity<Object> handleInvalidScopeException(
      HttpServletRequest req, InvalidTokenException ex) {
    log.error(format("Invalid PolicyIdStringWithMaskName: %s", ex.getMessage()));
    return new ResponseEntity<>("{\"error\": \"Invalid Scope\"}", new HttpHeaders(), UNAUTHORIZED);
  }

  @ExceptionHandler({InvalidRequestException.class})
  public ResponseEntity<Object> handleInvalidRequestException(
      HttpServletRequest req, InvalidRequestException ex) {
    log.error(format("Invalid request: %s", ex.getMessage()));
    return new ResponseEntity<>("{\"error\": \"%s\"}".format(ex.getMessage()), BAD_REQUEST);
  }

  @ExceptionHandler({UsernameNotFoundException.class})
  public ResponseEntity<Object> handleUserNotFoundException(
      HttpServletRequest req, InvalidTokenException ex) {
    log.error(format("User not found: %s", ex.getMessage()));
    return new ResponseEntity<>("{\"error\": \"User not found\"}", UNAUTHORIZED);
  }

  private String jsonEscape(String text) {
    return text.replace("\"", "\\\"");
  }

  private ResponseEntity<Object> errorResponse(HttpStatus status, String fmt, Exception ex) {
    log.error(format(fmt, ex.getMessage()));
    val headers = new HttpHeaders();
    headers.setContentType(APPLICATION_JSON);
    val msg = format("{\"error\": \"%s\"}", jsonEscape(ex.getMessage()));
    return new ResponseEntity<>(msg, status);
  }

  private void checkAdminOrOwner(@NonNull UUID userId) {
    // Check if admin, if not, then check if owner
    val authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!authorizationManager.authorizeWithAdminRole(authentication)) {
      val principal = authentication.getPrincipal();
      if (principal instanceof User) {
        val user = (User) principal;
        if (!user.getId().equals(userId)) {
          log.error(
              "User: {} is illegally trying to generate access tokens for user: {}",
              user.getId().toString(),
              userId.toString());
          throw new ForbiddenException("Action is forbidden for this user.");
        }
      } else {
        val app = (Application) principal;
        log.warn(
            "Application {} tried to create an access token for user {} but is not an ADMIN application.",
            app.getId().toString(),
            userId.toString());
        throw new ForbiddenException("Action is forbidden for this application.");
      }
    }
  }
}
