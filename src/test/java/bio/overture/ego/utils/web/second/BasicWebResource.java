package bio.overture.ego.utils.web.second;

import bio.overture.ego.utils.web.ResponseOption;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

public class BasicWebResource<T> extends AbstractWebResource<T, ResponseOption<T>, BasicWebResource<T>> {

  public BasicWebResource(TestRestTemplate restTemplate,
      String serverUrl, Class<T> responseType) {
    super(restTemplate, serverUrl, responseType);
  }

  @Override protected ResponseOption<T> createResponseOption(ResponseEntity<T> responseEntity) {
    return new ResponseOption<>(responseEntity);
  }
}
