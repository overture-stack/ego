package bio.overture.ego.security;

import org.springframework.security.oauth2.client.OAuth2RestOperations;

import java.util.Collections;
import java.util.Map;

public class LinkedInUserInfoTokenServices extends OAuth2UserInfoTokenServices {

    public LinkedInUserInfoTokenServices(
            String userInfoEndpointUrl, String clientId, OAuth2RestOperations restTemplate) {
      super(userInfoEndpointUrl, clientId, restTemplate);
    }

    @Override
    protected Map<String, Object> transformMap(Map<String, Object> map, String accessToken) {
      String email = (String) map.get("emailAddress");

      if (email != null) {
        map.put("email", email);
        map.put("given_name", map.get("firstName"));
        map.put("family_name", map.get("lastName"));
        return map;
      } else {
        return Collections.singletonMap("error", "Could not fetch user details");
      }
    }
}
