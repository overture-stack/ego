package bio.overture.ego.provider.linkedin;

import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

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
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LinkedInOAuthService {

  @Value("${linkedIn.clientSecret}")
  private String clientSecret;

  @Value("${linkedIn.clientID}")
  private String clientID;

  @Value("${linkedIn.redirectUri}")
  private String redirectUri;

  private RestTemplate restTemplate = new RestTemplate();

  private static final ObjectMapper objectMapper = new ObjectMapper();

  static final String TOKEN_ENDPOINT = "https://www.linkedin.com/oauth/v2/accessToken?grant_type={grant_type}&code={code}&redirect_uri={redirect_uri}&client_id={client_id}&client_secret={client_secret}";

  public Optional<IDToken> getAuthInfoFromLinkedIn(String code) {
    try {
      val accessToken = getAccessTokenFromLinkedIn(code);
      val headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("Authorization", "Bearer " + accessToken.get());
      val request = new HttpEntity<String>("", headers);

      ResponseEntity<String> response = restTemplate.exchange(
          "https://api.linkedin.com/v1/people/~:(email-address,first-name,last-name)?format=json", HttpMethod.GET,
          request, String.class);

      return parseIDToken(response.getBody());

    } catch (RestClientException | NoSuchElementException e) {
      log.warn(e.getMessage(), e);
      return Optional.empty();
    }

  }

  public Optional<String> getAccessTokenFromLinkedIn(String code) {

    val uriVariables = ImmutableMap.of( //
        "grant_type", "authorization_code", //
        "code", code, //
        "redirect_uri", redirectUri, //
        "client_id", clientID, //
        "client_secret", clientSecret //
    );

    try {
      val response = restTemplate.getForEntity(TOKEN_ENDPOINT, String.class, uriVariables);
      val jsonObject = objectMapper.<Map<String, String>>readValue(response.getBody(),
          new TypeReference<Map<String, String>>() {
          });
      val accessToken = jsonObject.get("access_token");
      return Optional.of(accessToken);

    } catch (RestClientException | IOException e) {
      log.warn(e.getMessage(), e);
      return Optional.empty();
    }
  }

  static private Optional<IDToken> parseIDToken(String idTokenJson) {
    try {
      val jsonObject = objectMapper.<Map<String, String>>readValue(idTokenJson,
          new TypeReference<Map<String, String>>() {
          });
      IDToken idToken = IDToken.builder() //
          .email(jsonObject.get("emailAddress")) //
          .given_name(jsonObject.get("firstName")) //
          .family_name(jsonObject.get("lastName")) //
          .build();
      return Optional.of(idToken);
    } catch (IOException e) {
      log.warn(e.getMessage(), e);
      return Optional.empty();
    }
  }
}
