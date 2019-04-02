package bio.overture.ego.utils.web.second;

import bio.overture.ego.utils.web.StringResponseOption;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

public class StringWebResource extends AbstractWebResource<String, StringResponseOption, StringWebResource> {

  public StringWebResource(TestRestTemplate restTemplate, String serverUrl) {
    super(restTemplate, serverUrl, String.class);
  }

  @Override
  protected StringResponseOption createResponseOption(ResponseEntity<String> responseEntity) {
    return new StringResponseOption(responseEntity);
  }

}
