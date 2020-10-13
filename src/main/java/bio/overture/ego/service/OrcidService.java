package bio.overture.ego.service;

import static org.springframework.http.HttpStatus.OK;

import bio.overture.ego.model.dto.OrcidEmail;
import bio.overture.ego.model.dto.OrcidEmailResponse;
import java.util.Collections;
import java.util.Map;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Service;

@Service
public class OrcidService {

  private String userRecordUri;

  @Autowired
  public OrcidService(@Value("${orcid.resource.userRecordUri}") String userRecordUri) {
    this.userRecordUri = userRecordUri;
  }

  public Map<String, Object> getPrimaryEmail(
      OAuth2RestOperations restTemplate, String orcid, Map<String, Object> map) {
    val url = String.format("%s/%s/email", userRecordUri, orcid);
    HttpHeaders headers = new HttpHeaders();
    headers.set("Accept", "application/json");
    HttpEntity<HttpHeaders> request = new HttpEntity<>(headers);
    ResponseEntity<OrcidEmailResponse> response =
        restTemplate.exchange(url, HttpMethod.GET, request, OrcidEmailResponse.class);

    if (response.getStatusCode() == OK) {
      val emails = response.getBody().getEmail();
      val primaryEmail =
          emails.stream()
              .filter(OrcidEmail::isPrimary)
              .filter(OrcidEmail::isVerified)
              .findFirst()
              .orElse(null)
              .getEmail();
      if (primaryEmail.isEmpty()) {
        return Collections.singletonMap("error", "Could not fetch user details");
      } else {
        map.put("email", primaryEmail);
        return map;
      }
    } else {
      return Collections.singletonMap("error", "Could not fetch user details");
    }
  }
}
