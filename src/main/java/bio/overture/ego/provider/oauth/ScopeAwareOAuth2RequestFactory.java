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
package bio.overture.ego.provider.oauth;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static org.springframework.security.oauth2.common.util.OAuth2Utils.SCOPE;

import bio.overture.ego.model.params.ScopeName;
import bio.overture.ego.service.TokenService;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.TokenRequest;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;

@Slf4j
public class ScopeAwareOAuth2RequestFactory extends DefaultOAuth2RequestFactory {

  private static final String USERNAME_REQUEST_PARAM = "username";
  private final TokenService tokenService;

  public ScopeAwareOAuth2RequestFactory(
      @NonNull ClientDetailsService clientDetailsService, @NonNull TokenService tokenService) {
    super(clientDetailsService);
    this.tokenService = tokenService;
  }

  private static Set<ScopeName> resolveRequestedScopes(Map<String, String> requestParameters) {
    val scope = requestParameters.get(SCOPE);
    checkState(
        !isNullOrEmpty(scope), "Failed to resolve scope from request: %s", requestParameters);
    return Sets.newHashSet(scope.split("/s+"))
        .stream()
        .map(s -> new ScopeName(s))
        .collect(Collectors.toSet());
  }

  private static String resolveUserName(Map<String, String> requestParameters) {
    val userName = requestParameters.get(USERNAME_REQUEST_PARAM);
    checkState(
        !isNullOrEmpty(userName), "Failed to resolve user from request: %s", requestParameters);

    return userName;
  }

  @Override
  public TokenRequest createTokenRequest(
      Map<String, String> requestParameters, ClientDetails authenticatedClient) {
    validateScope(requestParameters);

    return super.createTokenRequest(requestParameters, authenticatedClient);
  }

  void validateScope(Map<String, String> requestParameters) {
    val userName = resolveUserName(requestParameters);
    val requestScope = resolveRequestedScopes(requestParameters);

    val missing = tokenService.missingScopes(userName, requestScope);
    if (!missing.isEmpty()) {
      throw new AccessDeniedException(
          format("Invalid token scopes '%s' requested for user '%s'", missing, userName));
    }
  }
}
