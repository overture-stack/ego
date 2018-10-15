/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.overture.ego.provider.oauth;

import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.overture.ego.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Sets.difference;
import static java.lang.String.format;
import static org.springframework.security.oauth2.common.util.OAuth2Utils.SCOPE;

@Slf4j
public class ScopeAwareOAuth2RequestFactory extends DefaultOAuth2RequestFactory {

  private static final String USERNAME_REQUEST_PARAM = "username";
  private final UserService userService;

  public ScopeAwareOAuth2RequestFactory(@NonNull ClientDetailsService clientDetailsService,
    @NonNull UserService userService) {
    super(clientDetailsService);
    this.userService = userService;
  }

  private static Set<String> resolveRequestedScopes(Map<String, String> requestParameters) {
    val scope = requestParameters.get(SCOPE);
    checkState(!isNullOrEmpty(scope), "Failed to resolve scope from request: %s", requestParameters);
    return Sets.newHashSet(scope.split("/s+"));
  }

  private static String resolveUserName(Map<String, String> requestParameters) {
    val userName = requestParameters.get(USERNAME_REQUEST_PARAM);
    checkState(!isNullOrEmpty(userName), "Failed to resolve user from request: %s", requestParameters);

    return userName;
  }

  @Override
  public TokenRequest createTokenRequest(Map<String, String> requestParameters, ClientDetails authenticatedClient) {
    validateScope(requestParameters);

    return super.createTokenRequest(requestParameters, authenticatedClient);
  }

  void validateScope(Map<String, String> requestParameters) {
    val user = resolveUserName(requestParameters);
    val requestScope = resolveRequestedScopes(requestParameters);
    val userScopes = userService.get(user).getScopes();
    log.debug("Verifying allowed scopes for user '{}'...", user);
    log.debug("User scopes: {}. RequestScopes: {}", userScopes, requestScope);

    val scopeDiff = difference(requestScope, Sets.newHashSet(userScopes));
    if (!scopeDiff.isEmpty()) {
      val extraScope = scopeDiff.stream().collect(Collectors.joining(" "));
      throw new AccessDeniedException(format("Invalid token scope '%s' requested for user '%s'. Valid scopes: %s",
        extraScope, user, userScopes));
    }
  }

}
