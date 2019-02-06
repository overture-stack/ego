package bio.overture.ego.security;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.web.client.RestClientException;

public class GithubUserInfoTokenServices extends OAuth2UserInfoTokenServices {

  public GithubUserInfoTokenServices(
      String userInfoEndpointUrl, String clientId, OAuth2RestOperations restTemplate) {
    super(userInfoEndpointUrl, clientId, restTemplate);
  }

  @Override
  protected Map<String, Object> transformMap(Map<String, Object> map, String accessToken)
      throws NoSuchElementException {
    OAuth2RestOperations restTemplate = getRestTemplate(accessToken);
    String email;

    try {
      // [{email, primary, verified}]
      email =
          (String)
              restTemplate
                  .exchange(
                      "https://api.github.com/user/emails",
                      HttpMethod.GET,
                      null,
                      new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                  .getBody()
                  .stream()
                  .filter(x -> x.get("verified").equals(true) && x.get("primary").equals(true))
                  .findAny()
                  .orElse(Collections.emptyMap())
                  .get("email");
    } catch (RestClientException | ClassCastException ex) {
      return Collections.singletonMap("error", "Could not fetch user details");
    }

    if (email != null) {
      map.put("email", email);

      String name = (String) map.get("name");
      String[] names = name.split(" ");
      if (names.length == 2) {
        map.put("given_name", names[0]);
        map.put("family_name", names[1]);
      }
      return map;
    } else {
      return Collections.singletonMap("error", "Could not fetch user details");
    }
  }
}
