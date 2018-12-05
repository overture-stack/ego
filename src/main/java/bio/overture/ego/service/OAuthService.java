package bio.overture.ego.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import bio.overture.ego.token.IDToken;

@Service
public class OAuthService {

  @Value("${oauth.linkedIn.clientSecret}")
  private String clientSecret;

  @Value("${oauth.linkedIn.clientID}")
  private String clientID;

  RestTemplate restTemplate = new RestTemplate();

  public Optional<IDToken> getAuthInfoFromLinkedIn(String code) {
    try {
      final Optional<String> accessToken = getAccessTokenFromLinkedIn(code);
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("Authorization", "Bearer " + accessToken.get());
      HttpEntity<String> request = new HttpEntity<String>("", headers);

      ResponseEntity<String> response = restTemplate.exchange(
          "https://api.linkedin.com/v1/people/~:(email-address,first-name,last-name)?format=json", HttpMethod.GET,
          request, String.class);

      return parseIDToken(response.getBody());

    } catch (RestClientException | NoSuchElementException e) {
      return Optional.empty();
    }

  }

  public Optional<String> getAccessTokenFromLinkedIn(String code) {
    String tokenEndpoint = "https://www.linkedin.com/oauth/v2/accessToken?grant_type={grant_type}&code={code}&redirect_uri={redirect_uri}&client_id={client_id}&client_secret={client_secret}";

    Map<String, String> uriVariables = new HashMap<String, String>();
    uriVariables.put("grant_type", "authorization_code");
    uriVariables.put("code", code);
    uriVariables.put("redirect_uri", "http://localhost:8081/oauth/linkedin-cb");
    uriVariables.put("client_id", clientID);
    uriVariables.put("client_secret", clientSecret);

    try {
      ResponseEntity<String> response = restTemplate.getForEntity(tokenEndpoint, String.class, uriVariables);
      ObjectMapper mapper = new ObjectMapper();
      Map<String, String> jsonObject = mapper.readValue(response.getBody(), new TypeReference<Map<String, String>>() {
      });
      String accessToken = jsonObject.get("access_token");
      return Optional.of(accessToken);

    } catch (RestClientException | IOException e) {
      return Optional.empty();
    }

  }

  public Optional<IDToken> parseIDToken(String idTokenJson) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      Map<String, String> jsonObject = mapper.readValue(idTokenJson, new TypeReference<Map<String, String>>() {
      });
      IDToken idToken = IDToken.builder().email((String) jsonObject.get("emailAddress"))
          .given_name((String) jsonObject.get("firstName")).family_name((String) jsonObject.get("lastName")).build();
      return Optional.of(idToken);
    } catch (IOException e) {
      return Optional.empty();
    }
  }
}
