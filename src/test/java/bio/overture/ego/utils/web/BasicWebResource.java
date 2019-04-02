package bio.overture.ego.utils.web;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

public class BasicWebResource<T, O extends ResponseOption<T, O>> extends AbstractWebResource<T, O, BasicWebResource<T, O>> {

  public BasicWebResource(TestRestTemplate restTemplate,
      String serverUrl, Class<T> responseType) {
    super(restTemplate, serverUrl, responseType);
  }

  @Override protected O createResponseOption(ResponseEntity<T> responseEntity) {
    return (O)new ResponseOption<T, O>(responseEntity);
  }
}
