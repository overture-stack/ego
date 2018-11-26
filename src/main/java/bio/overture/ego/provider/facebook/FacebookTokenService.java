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

package bio.overture.ego.provider.facebook;

import bio.overture.ego.token.IDToken;
import bio.overture.ego.utils.TypeUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@NoArgsConstructor
public class FacebookTokenService {

  /*
  Constants
   */
  private final static String USER_EMAIL = "email";
  private final static String USER_NAME = "name";
  private final static String USER_GIVEN_NAME = "given_name";
  private final static String USER_LAST_NAME = "family_name";
  private final static String IS_VALID = "is_valid";
  private final static String DATA = "data";
  /*
    Dependencies
   */
  protected RestTemplate fbConnector;
  /*
   Variables
  */
  @Value("${facebook.client.id}")
  private String clientId;
  @Value("${facebook.client.secret}")
  private String clientSecret;
  @Value("${facebook.client.accessTokenUri}")
  private String accessTokenUri;
  @Value("${facebook.client.tokenValidateUri}")
  private String tokenValidateUri;
  @Value("${facebook.client.timeout.connect}")
  private int connectTimeout;
  @Value("${facebook.client.timeout.read}")
  private int readTimeout;
  @Value("${facebook.resource.userInfoUri}")
  private String userInfoUri;

  @PostConstruct
  private void init() {
    fbConnector = new RestTemplate(httpRequestFactory());
  }

  public boolean validToken(String fbToken) {
    log.debug("Validating Facebook token: {}", fbToken);
    val tokenCheckUri = getValidationUri(fbToken);
    try {
      return fbConnector.execute(new URI(tokenCheckUri), HttpMethod.GET, null,
        response -> {
          val jsonObj = getJsonResponseAsMap(response.getBody());
          if (jsonObj.isPresent()) {
            val output = ((HashMap<String, Object>) jsonObj.get().get(DATA));
            if (output.containsKey(IS_VALID)) {
              return (Boolean) output.get(IS_VALID);
            } else {
              log.error("Error while validating Facebook token: {}", output);
              return false;
            }
          } else
            return false;
        });
    } catch (URISyntaxException uex) {
      log.error("Invalid URI syntax: {}, {}", tokenCheckUri, uex.getMessage());
      return false;
    }
  }

  public Optional<IDToken> getAuthInfo(String fbToken) {
    log.debug("Getting details for Facebook token: {}", fbToken);
    val userDetailsUri = getUserDetailsUri(fbToken);
    try {
      return fbConnector.execute(new URI(userDetailsUri), HttpMethod.GET, null,
        response -> {
          val jsonObj = getJsonResponseAsMap(response.getBody());
          if (jsonObj.isPresent()) {
            val output = new HashMap<String, String>();
            output.put(USER_EMAIL, jsonObj.get().get(USER_EMAIL).toString());
            val name = jsonObj.get().get(USER_NAME).toString().split(" ");
            output.put(USER_GIVEN_NAME, name[0]);
            output.put(USER_LAST_NAME, name[1]);
            return Optional.of(TypeUtils.convertToAnotherType(output, IDToken.class));
          } else
            return Optional.empty();
        });
    } catch (URISyntaxException uex) {
      log.error("Invalid URI syntax: {}, {}", userDetailsUri, uex.getMessage());
      return Optional.empty();
    } catch (Exception ex) {
      log.error("Error getting email response from Facebook: {}", ex.getMessage());
      log.debug("Error getting email response from Facebook for uri: {}, {}", userDetailsUri, ex.getMessage());
      return Optional.empty();
    }
  }

  private Optional<Map> getJsonResponseAsMap(InputStream jsonResponse) {

    val objectMapper = new ObjectMapper();
    Map jsonObj = null;
    try {
      jsonObj = objectMapper.readValue(jsonResponse, Map.class);
      return Optional.of(jsonObj);
    } catch (IOException e) {
      log.error("Error parsing response : {}, {}", jsonResponse, e.getMessage());
      return Optional.empty();
    }
  }

  private String getUserDetailsUri(String fbToken) {
    return userInfoUri + "?fields=email,name,id&access_token=" + fbToken;
  }

  @SneakyThrows
  private String getValidationUri(String fbToken) {
    return tokenValidateUri + "?input_token=" + fbToken + "&access_token=" +
      URLEncoder.encode(clientId + "|" + clientSecret, "UTF-8");
  }

  private HttpComponentsClientHttpRequestFactory httpRequestFactory() {
    val factory = new HttpComponentsClientHttpRequestFactory();
    factory.setConnectTimeout(connectTimeout);
    factory.setReadTimeout(readTimeout);
    return factory;
  }

}
