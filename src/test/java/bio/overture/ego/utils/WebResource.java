package bio.overture.ego.utils;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

@RequiredArgsConstructor
public class WebResource<T> {

  @NonNull private final TestRestTemplate restTemplate;
  @NonNull private final String serverUrl;
  @NonNull private final Class<T> responseType;

  private String endpoint;
  private Object body;
  private HttpHeaders headers;

  public WebResource<T> endpoint(String formattedEndpoint, Object... args) {
    this.endpoint = String.format(formattedEndpoint, args);
    return this;
  }

  public WebResource<T> body(Object body) {
    this.body = body;
    return this;
  }

  public WebResource<T> headers(HttpHeaders httpHeaders) {
    this.headers = httpHeaders;
    return this;
  }

  public ResponseEntity<T> get() {
    return doRequest(null, HttpMethod.GET);
  }

  public ResponseEntity<T> put() {
    return doRequest(this.body, HttpMethod.PUT);
  }

  public ResponseEntity<T> post() {
    return doRequest(this.body, HttpMethod.POST);
  }

  public ResponseEntity<T> delete() {
    return doRequest(null, HttpMethod.DELETE);
  }

  private String getUrl() {
    return Joiners.PATH.join(this.serverUrl, this.endpoint);
  }

  private ResponseEntity<T> doRequest(Object body, HttpMethod httpMethod) {
    return restTemplate.exchange(
        getUrl(), httpMethod, new HttpEntity<>(body, this.headers), this.responseType);
  }

  public static <T> WebResource<T> createWebResource(
      TestRestTemplate restTemplate, String serverUrl, Class<T> responseType) {
    return new WebResource<>(restTemplate, serverUrl, responseType);
  }
}
