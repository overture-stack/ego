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

package bio.overture.ego.utils;

import static java.lang.String.format;

import bio.overture.ego.model.entity.Application;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedClientException;

@Slf4j
public class Redirects {

  /**
   * Returns redirect uri based Declares a runtime exception to be explicit
   *
   * @return Returns URI as a String that Ego will redirect to
   */
  public static String getRedirectUri(@NonNull Application app, String redirect) {
    val redirects = Arrays.stream(app.getRedirectUri().split(","));

    // Short return if no redirect URI is provided
    if (redirect == null || redirect.isEmpty()) {
      return redirects
          .findFirst()
          .orElseThrow(() -> new UnauthorizedClientException("Cannot find valid redirect URI"));
    }

    val msg = format("Unauthorized redirect URI: %s", redirect);
    URI redirectUri;
    try {
      redirectUri = new URI(redirect);
    } catch (URISyntaxException e) {
      log.error(msg);
      throw new UnauthorizedClientException(msg);
    }

    val isValid =
        redirects
            .map(
                r -> {
                  try {
                    return new URI(r);
                  } catch (URISyntaxException e) {
                    log.error(
                        format(
                            "Could not parse URI in getRedirectUriOrThrow for clientId: %s %s",
                            app.getClientId(), r),
                        e);
                    return null;
                  }
                })
            .filter(Objects::nonNull)
            .map(
                u ->
                    u.getHost().equals(redirectUri.getHost())
                        && u.getPort() == redirectUri.getPort()) // Map to valid/invalid
            .reduce(Boolean::logicalOr) // Needs at least one valid
            .orElse(false);

    if (isValid) {
      return redirect;
    } else {
      log.error(msg);
      throw new UnauthorizedClientException(msg);
    }
  }
}
