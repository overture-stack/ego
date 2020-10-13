package bio.overture.ego.service;

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
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Service;

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
    val response = restTemplate.exchange(url, HttpMethod.GET, request, OrcidEmailResponse.class);

    if (response.getStatusCode() == OK && response.hasBody() && response.getBody() != null) {
      val emailList = response.getBody().getEmail();
      val primaryEmail =
          emailList.stream()
              .filter(OrcidEmail::isPrimary)
              .filter(OrcidEmail::isVerified)
              .findFirst()
              .map(OrcidEmail::getEmail);

      if (primaryEmail.isEmpty()) {
        return singletonMap("error", "Could not fetch user details");
      } else {
        map.put("email", primaryEmail.get());
        return map;
      }
    } else {
      return singletonMap("error", "Could not fetch user details");
    }
  }
}
