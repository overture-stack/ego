/*
 * Copyright (c) 2019. The Ontario Institute for Cancer Research. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package bio.overture.ego.event.token;

import static bio.overture.ego.utils.Collectors.toImmutableSet;

import bio.overture.ego.model.dto.ApiKeyResponse;
import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.params.ScopeName;
import bio.overture.ego.service.TokenService;
import java.util.Set;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CleanupTokenListener implements ApplicationListener<CleanupUserTokensEvent> {

  /** Dependencies */
  private final TokenService tokenService;

  @Autowired
  public CleanupTokenListener(@NonNull TokenService tokenService) {
    this.tokenService = tokenService;
  }

  @Override
  public void onApplicationEvent(@NonNull CleanupUserTokensEvent event) {
    log.debug("Number of users to be checked for token cleanup: {}", event.getUsers().size());
    cleanupTokens(event.getUsers());
  }

  private void cleanupTokens(@NonNull Set<User> users) {
    users.forEach(this::cleanupTokensForUser);
  }

  private void cleanupTokensForUser(@NonNull User user) {
    val scopes = tokenService.userScopes(user.getName()).getScopes();
    val tokens = tokenService.listToken(user.getId());
    tokens.forEach(t -> verifyToken(t, scopes));
  }

  private void verifyToken(@NonNull ApiKeyResponse token, @NonNull Set<String> scopes) {
    // Expand effective scopes to include READ if WRITE is present and convert to Scope type.
    val expandedUserScopes =
        Scope.explicitScopes(
            scopes.stream().map(this::convertStringToScope).collect(toImmutableSet()));

    // Convert token scopes from String to Scope
    val tokenScopes =
        token.getScope().stream().map(this::convertStringToScope).collect(toImmutableSet());

    // Compare
    if (!expandedUserScopes.containsAll(tokenScopes)) {
      log.info(
          "Token scopes not contained in user scopes, revoking. {} not in {}",
          tokenScopes.toString(),
          expandedUserScopes.toString());
      tokenService.revoke(token.getApiKey());
    }
  }

  private Scope convertStringToScope(@NonNull String stringScope) {
    val s = new ScopeName(stringScope);

    val policy = new Policy();
    policy.setName(s.getName());
    return new Scope(policy, s.getAccessLevel());
  }
}
