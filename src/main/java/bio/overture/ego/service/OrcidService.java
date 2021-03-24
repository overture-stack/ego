package bio.overture.ego.service;

import static bio.overture.ego.model.exceptions.InternalServerException.buildInternalServerException;
import static java.util.Collections.singletonMap;
import static org.springframework.http.HttpStatus.OK;

import bio.overture.ego.model.dto.OrcidEmail;
import bio.overture.ego.model.dto.OrcidEmailResponse;
import java.util.Map;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;

@Slf4j
@Service
public class OrcidService {

  private String userRecordUri;

  @Autowired
  public OrcidService(@NonNull @Value("${orcid.resource.userRecordUri}") String userRecordUri) {
    this.userRecordUri = userRecordUri;
  }

  public Map<String, Object> getPrimaryEmail(
      OAuth2RestOperations restTemplate, @NonNull String orcid, @NonNull Map<String, Object> map) {
    val url = String.format("%s/%s/email", userRecordUri, orcid);
    val headers = new HttpHeaders();
    headers.set("Accept", "application/json");
    val request = new HttpEntity<>(headers);
    ResponseEntity<OrcidEmailResponse> response = null;
    try {
      response = restTemplate.exchange(url, HttpMethod.GET, request, OrcidEmailResponse.class);
    } catch (HttpStatusCodeException err) {
      // Notes: not wrapping exception since we do not know if the response from ORCID will contain
      // anything sensitive.
      log.error(
          "Invalid {} response from ORCID service: {}",
          err.getStatusCode().value(),
          err.getMessage());
      throw buildInternalServerException(
          "Invalid %s response from ORCID service.", err.getStatusCode().value());
    }

    if (response.getStatusCode() == OK && response.hasBody() && response.getBody() != null) {
      val emailList = response.getBody().getEmail();
      val primaryEmail =
          emailList.stream()
              .filter(OrcidEmail::isPrimary)
              .filter(OrcidEmail::isVerified)
              .findFirst()
              .map(OrcidEmail::getEmail);

      if (primaryEmail.isEmpty()) {
        log.error("No primary email found.");
        return singletonMap("primaryEmailError", "Could not fetch user details");
      } else {
        map.put("email", primaryEmail.get());
        // orcid allows a null value for the family_name field, which breaks the @NotNull constraint
        // on User.lastName
        map.putIfAbsent("family_name", "");
        return map;
      }
    } else {
      log.error("Response body was empty.");
      return singletonMap("error", "Could not fetch user details");
    }
  }
}
