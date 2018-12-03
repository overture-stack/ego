package bio.overture.ego.service;

import java.util.Map;
import java.util.Optional;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;

import org.springframework.boot.json.BasicJsonParser;
import org.springframework.stereotype.Service;

import bio.overture.ego.token.IDToken;

@Service
public class OAuthService {

  public Optional<IDToken> getAuthInfo(String code, OAuth20Service service) {
    // TODO: Fetch user info based on service type
    try {
      final OAuth2AccessToken accessToken = service.getAccessToken(code);

      final OAuthRequest request = new OAuthRequest(Verb.GET,
          "https://api.linkedin.com/v1/people/~:(email-address,first-name,last-name)?format=json");
      service.signRequest(accessToken, request);
      final Response response = service.execute(request);
      BasicJsonParser parser = new BasicJsonParser();
      Map<String, Object> result = parser.parseMap(response.getBody());

      IDToken idToken = IDToken.builder().email((String) result.get("emailAddress"))
          .given_name((String) result.get("firstName")).family_name((String) result.get("lastName")).build();
      return Optional.of(idToken);

    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
