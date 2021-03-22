package bio.overture.ego.service;

import static bio.overture.ego.model.enums.LinkedinContactType.EMAIL;
import static bio.overture.ego.model.exceptions.InternalServerException.buildInternalServerException;
import static java.util.Collections.singletonMap;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import bio.overture.ego.model.dto.LinkedinEmail;
import bio.overture.ego.model.dto.LinkedinEmailResponse;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

@Slf4j
@Service
public class LinkedinService {

  private String userEmailUri;

  @Autowired
  public LinkedinService(@NonNull @Value("${linkedIn.resource.userEmailUri}") String userEmailUri) {
    this.userEmailUri = userEmailUri;
  }

  public Map<String, Object> getPrimaryEmail(
      OAuth2RestOperations restTemplate, @NonNull Map<String, Object> map) {
    val headers = new HttpHeaders();
    headers.set(ACCEPT, APPLICATION_JSON_VALUE);
    val request = new HttpEntity<>(headers);
    ResponseEntity<LinkedinEmailResponse> response;
    try {
      response = restTemplate.exchange(userEmailUri, GET, request, LinkedinEmailResponse.class);
    } catch (HttpStatusCodeException err) {
      log.error(
          "Invalid {} response from LINKEDIN service: {}",
          err.getStatusCode().value(),
          err.getMessage());
      throw buildInternalServerException(
          "Invalid %s response from LINKEDIN service.", err.getStatusCode().value());
    }

    if (!(response.getStatusCode() == OK && response.hasBody() && response.getBody() != null)) {
      log.error("Response body was empty.");
      return singletonMap("error", "Could not fetch user details");
    }

    val emails = response.getBody().getElements();
    val primaryEmail =
        emails.stream()
            .filter(e -> e.getType() == EMAIL)
            .filter(LinkedinEmail::isPrimary)
            .findFirst()
            .map(LinkedinEmail::getEmail);

    if (primaryEmail.isEmpty()) {
      return singletonMap("error", "A primary email was not found for the user.");
    }
    val email = primaryEmail.get();

    map.put("email", email);
    map.put("given_name", map.get("localizedFirstName"));
    map.put("family_name", map.get("localizedLastName"));
    return map;
  }
}
