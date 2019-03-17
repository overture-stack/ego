package bio.overture.ego.controller;

import bio.overture.ego.utils.WebResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;

import static bio.overture.ego.utils.WebResource.createWebResource;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
public abstract class AbstractControllerTest {

  /** Constants */
  public static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String ACCESS_TOKEN = "TestToken";

  /**
   * Config
   */

  /** State */
  @LocalServerPort private int port;

  private TestRestTemplate restTemplate = new TestRestTemplate();
  private HttpHeaders headers = new HttpHeaders();

  @Before
  public void setup() {
    headers.add(AUTHORIZATION, "Bearer " + ACCESS_TOKEN);
    headers.setContentType(APPLICATION_JSON);
    beforeTest();
  }

  /** Additional setup before each test */
  protected abstract void beforeTest();
  protected abstract boolean enableLogging();

  public WebResource<String> initStringRequest() {
    val out = initRequest(String.class);
    return enableLogging() ? out.prettyLogging() : out;
  }

  public <T> WebResource<T> initRequest(@NonNull Class<T> responseType) {
    return createWebResource(restTemplate, getServerUrl(), responseType).headers(this.headers);
  }

  public String getServerUrl() {
    return "http://localhost:" + port;
  }
}
