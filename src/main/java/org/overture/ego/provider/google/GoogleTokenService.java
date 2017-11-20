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

package org.overture.ego.provider.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GoogleTokenService {

  HttpTransport transport;
  JsonFactory jsonFactory;
  GoogleIdTokenVerifier verifier;
  @Value("${google.client.Ids}")
  private String clientIDs;

  public GoogleTokenService() {
    transport = new NetHttpTransport();
    jsonFactory = new JacksonFactory();
  }

  public boolean validToken(String token) {
    if (verifier == null)
      initVerifier();
    GoogleIdToken idToken = null;
    try {
      idToken = verifier.verify(token);
    } catch (GeneralSecurityException gEX) {
      log.error("Error while verifying google token: {}", gEX);
    } catch (IOException ioEX) {
      log.error("Error while verifying google token: {}", ioEX);
    } catch (Exception ex) {
      log.error("Error while verifying google token: {}", ex);
    }

    return (idToken != null);
  }

  @Synchronized
  private void initVerifier() {
    List<String> targetAudience;
    if (clientIDs.contains(","))
      targetAudience = Arrays.asList(clientIDs.split(","));
    else {
      targetAudience = new ArrayList<String>();
      targetAudience.add(clientIDs);
    }
    verifier =
        new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
            .setAudience(targetAudience)
            .build();
  }

  @SneakyThrows
  public Map decode(String token){
    val tokenDecoded = JwtHelper.decode(token);
    val authInfo = new ObjectMapper().readValue(tokenDecoded.getClaims(), Map.class);
    return authInfo;
  }
}
