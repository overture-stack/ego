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
import bio.overture.ego.model.exceptions.UnauthorizedClientException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class Redirects {

  /**
   * Returns redirect uri based on combination between provided URI and those registered in the
   * passed application. Checks validity and throws 403 if unauthorized URI is provided.
   *
   * @return Returns URI as a String that Ego will redirect to
   */
  public static String getRedirectUri(@NonNull Application app, String redirect) {
    val redirects = Arrays.stream(app.getRedirectUri().split(","));
    return verifyRedirectUri(redirect, redirects);
  }

  private static Optional<URI> toUri(String uri) {
    try {
      return Optional.of(new URI(uri));
    } catch (URISyntaxException e) {
      log.error(format("Could not parse URI %s", uri), e);
      return Optional.empty();
    }
  }

  private static Predicate<URI> getMatcher(URI target) {
    return (URI u) -> u.getHost().equals(target.getHost()) && u.getPort() == target.getPort();
  }

  public static String getErrorRedirectUri(@NonNull Application app, String redirect) {
    val redirects = Arrays.stream(app.getErrorRedirectUri().split(","));
    return verifyRedirectUri(redirect, redirects);
  }

  public static String verifyRedirectUri(String redirect, Stream<String> redirects) {
    // Short return if no redirect URI is provided
    if (redirect == null || redirect.isBlank()) {
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
            .map(Redirects::toUri)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .anyMatch(getMatcher(redirectUri)); // Needs at least one valid

    if (isValid) {
      return redirect;
    } else {
      log.error(msg);
      throw new UnauthorizedClientException(msg);
    }
  }
}
