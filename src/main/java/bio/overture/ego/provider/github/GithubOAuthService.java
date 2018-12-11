package bio.overture.ego.provider.github;

import bio.overture.ego.token.IDToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class GithubOAuthService {

  @Value("${github.clientSecret}")
  private String clientSecret;

  @Value("${github.clientID}")
  private String clientID;

  @Value("${github.redirectUri}")
  private String redirectUri;

  private RestTemplate restTemplate = new RestTemplate();

  private static final ObjectMapper objectMapper =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  static final String TOKEN_ENDPOINT =
      "https://github.com/login/oauth/access_token?code={code}&redirect_uri={redirect_uri}&client_id={client_id}&client_secret={client_secret}";

  public Optional<IDToken> getAuthInfo(String code) {
    val accessToken = getAccessToken(code);
    Optional<String> name;
    Optional<String> email;
    if (accessToken.isPresent()) {
      name = getName(accessToken.get());
      email = getEmail(accessToken.get());
    } else {
      return Optional.empty();
    }

    try {
      return Optional.of(
          IDToken.builder() //
              .email(email.get()) //
              .given_name(name.get().split(" ")[0]) //
              .family_name(name.get().split(" ")[1]) //
              .build());
    } catch (NoSuchElementException | ArrayIndexOutOfBoundsException e) {
      return Optional.empty();
    }
  }

  private Optional<String> getName(String accessToken) {
    val headers = new HttpHeaders();

    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + accessToken);
    val request = new HttpEntity<String>("", headers);
    try {
      ResponseEntity<String> response =
          restTemplate.exchange(
              "https://api.github.com/user", HttpMethod.GET, request, String.class);
      val name =
          (String)
              objectMapper
                  .<Map<String, Object>>readValue(
                      response.getBody(), new TypeReference<Map<String, Object>>() {})
                  .get("name");
      return Optional.of(name);
    } catch (RestClientException | IOException e) {
      log.warn(e.getMessage(), e);
      return Optional.empty();
    }
  }

  private Optional<String> getEmail(String accessToken) {
    val headers = new HttpHeaders();

    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + accessToken);
    val request = new HttpEntity<String>("", headers);
    try {
      ResponseEntity<String> response =
          restTemplate.exchange(
              "https://api.github.com/user/emails", HttpMethod.GET, request, String.class);
      val emails =
          objectMapper.<Map<String, Object>[]>readValue(
              response.getBody(), new TypeReference<Map<String, Object>[]>() {});

      val email =
          (String)
              Arrays.stream(emails)
                  .filter(
                      emailObject -> {
                        val primary = emailObject.get("primary");
                        val verified = emailObject.get("verified");
                        if (primary instanceof Boolean && verified instanceof Boolean) {
                          return ((Boolean) primary) && ((Boolean) verified);
                        } else {
                          return false;
                        }
                      })
                  .findAny()
                  .get()
                  .get("email");
      return Optional.of(email);
    } catch (RestClientException | IOException e) {
      log.warn(e.getMessage(), e);
      return Optional.empty();
    }
  }

  public Optional<String> getAccessToken(String code) {

    val uriVariables =
        ImmutableMap.of( //
            "code", code, //
            "redirect_uri", redirectUri, //
            "client_id", clientID, //
            "client_secret", clientSecret //
            );

    try {
      val headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("Accept", "application/json");
      val request = new HttpEntity<String>("", headers);

      ResponseEntity<String> response =
          restTemplate.exchange(
              TOKEN_ENDPOINT, HttpMethod.GET, request, String.class, uriVariables);

      val jsonObject =
          objectMapper.<Map<String, String>>readValue(
              response.getBody(), new TypeReference<Map<String, String>>() {});
      val accessToken = jsonObject.get("access_token");
      return Optional.of(accessToken);

    } catch (RestClientException | IOException e) {
      log.warn(e.getMessage(), e);
      return Optional.empty();
    }
  }
}
