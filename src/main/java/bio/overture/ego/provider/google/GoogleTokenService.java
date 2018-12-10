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

package bio.overture.ego.provider.google;

import static bio.overture.ego.utils.TypeUtils.convertToAnotherType;
import static java.util.Arrays.asList;

import bio.overture.ego.token.IDToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@NoArgsConstructor
public class GoogleTokenService {

  /*
   * Dependencies
   */
  @Value("${google.client.Ids}")
  private String clientIDs;

  /*
   * State
   */
  @Getter(lazy = true)
  private final GoogleIdTokenVerifier verifier = initVerifier();

  public boolean validToken(String token) {
    val verifier = this.getVerifier();
    GoogleIdToken idToken = null;
    try {
      idToken = verifier.verify(token);
    } catch (GeneralSecurityException | IOException gEX) {
      log.error("Error while verifying google token: {}", gEX);
    }
    return (idToken != null);
  }

  private GoogleIdTokenVerifier initVerifier() {
    checkState();
    val targetAudience = asList(clientIDs.split(","));
    return new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new JacksonFactory())
        .setAudience(targetAudience)
        .build();
  }

  @SneakyThrows
  public IDToken decode(String token) {
    val claims = JwtHelper.decode(token).getClaims();
    val authInfo = new ObjectMapper().readValue(claims, Map.class);
    return convertToAnotherType(authInfo, IDToken.class);
  }

  private void checkState() {
    if (clientIDs == null) {
      throw new IllegalStateException("No client Ids are configured for google. ");
    }
  }
}
