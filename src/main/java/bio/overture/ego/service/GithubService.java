package bio.overture.ego.service;

import static bio.overture.ego.utils.Joiners.BLANK;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import lombok.val;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.stereotype.Service;

@Service
public class GithubService {

  public String getVerifiedEmail(OAuth2RestOperations restTemplate) {
    String email;
    email =
        (String)
            restTemplate
                .exchange(
                    "https://api.github.com/user/emails",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .getBody().stream()
                .filter(x -> x.get("verified").equals(true) && x.get("primary").equals(true))
                .findAny()
                .orElse(Collections.emptyMap())
                .get("email");
    return email;
  }

  public Map<String, Object> parseName(@NonNull String name, Map<String, Object> map) {
    List<String> names = Arrays.asList(name.split(" "));
    val numNames = names.size();

    if (numNames > 0) {
      if (numNames == 1) {
        map.put("given_name", names.get(0));
      } else {
        List<String> firstNames = names.subList(0, numNames - 1);
        List<String> lastName = names.subList(numNames - 1, numNames);
        map.put("given_name", BLANK.join(firstNames));
        map.put("family_name", lastName.get(0));
      }
    }
    return map;
  }
}
