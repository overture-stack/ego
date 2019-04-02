package bio.overture.ego.utils.web.second;

import bio.overture.ego.utils.web.CleanResponse;
import bio.overture.ego.utils.web.QueryParam;
import bio.overture.ego.utils.web.ResponseOption;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.Set;

import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Joiners.AMPERSAND;
import static bio.overture.ego.utils.Joiners.PATH;
import static bio.overture.ego.utils.web.QueryParam.createQueryParam;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Objects.isNull;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractWebResource<T,  R extends ResponseOption<T>, W extends AbstractWebResource<T, R, W> > {

  private static final ObjectMapper REGULAR_MAPPER = new ObjectMapper();
  private static final ObjectMapper PRETTY_MAPPER = new ObjectMapper();

  static {
    PRETTY_MAPPER.enable(INDENT_OUTPUT);
  }

  @NonNull private final TestRestTemplate restTemplate;
  @NonNull private final String serverUrl;
  @NonNull private final Class<T> responseType;

  private String endpoint;
  private Set<QueryParam> queryParams = newHashSet();
  private Object body;
  private HttpHeaders headers;
  private boolean enableLogging = false;
  private boolean pretty = false;

  protected abstract R createResponseOption(ResponseEntity<T> responseEntity);

  private W thisInstance(){
    return (W)this;
  }

  public W endpoint(String formattedEndpoint, Object... args) {
    this.endpoint = format(formattedEndpoint, args);
    return thisInstance();
  }

  public W body(Object body) {
    this.body = body;
    return thisInstance();
  }

  public W headers(HttpHeaders httpHeaders) {
    this.headers = httpHeaders;
    return thisInstance();
  }

  public W logging() {
    return configLogging(true, false);
  }

  public W prettyLogging() {
    return configLogging(true, true);
  }

  public W queryParam(String key, Object... values) {
    queryParams.add(createQueryParam(key, values));
    return thisInstance();
  }

  private W configLogging(boolean enable, boolean pretty) {
    this.enableLogging = enable;
    this.pretty = pretty;
    return thisInstance();
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

  public R deleteAnd() {
    return createResponseOption(delete());
  }

  public R getAnd() {
      return createResponseOption(get());
  }

  public R putAnd() {
    return createResponseOption(put());
  }

  public R postAnd() {
    return createResponseOption(post());
  }

  private Optional<String> getQuery() {
    val queryStrings = queryParams.stream().map(QueryParam::toString).collect(toImmutableSet());
    return queryStrings.isEmpty() ? Optional.empty() : Optional.of(AMPERSAND.join(queryStrings));
  }

  private String getUrl() {
    return PATH.join(this.serverUrl, this.endpoint) + getQuery().map(x -> "?" + x).orElse("");
  }

  @SneakyThrows
  private ResponseEntity<T> doRequest(Object body, HttpMethod httpMethod) {
    logRequest(enableLogging, pretty, httpMethod, getUrl(), body);
    val response =
        restTemplate.exchange(
            getUrl(), httpMethod, new HttpEntity<>(body, this.headers), this.responseType);
    logResponse(enableLogging, pretty, response);
    return response;
  }

  @SneakyThrows
  private static void logRequest(
      boolean enable, boolean pretty, HttpMethod httpMethod, String url, Object body) {
    if (enable) {
      if (isNull(body)) {
        log.info("[REQUEST] {} {}", httpMethod, url);
      } else {
        if (pretty) {
          log.info(
              "[REQUEST] {} {} < \n{}", httpMethod, url, PRETTY_MAPPER.writeValueAsString(body));
        } else {
          log.info(
              "[REQUEST] {} {} < {}", httpMethod, url, REGULAR_MAPPER.writeValueAsString(body));
        }
      }
    }
  }

  @SneakyThrows
  private static <T> void logResponse(boolean enable, boolean pretty, ResponseEntity<T> response) {
    if (enable) {
      val output =
          CleanResponse.builder()
              .body(response.hasBody() ? response.getBody() : null)
              .statusCodeName(response.getStatusCode().name())
              .statusCodeValue(response.getStatusCodeValue())
              .build();
      if (pretty) {
        log.info("[RESPONSE] > \n{}", PRETTY_MAPPER.writeValueAsString(output));
      } else {
        log.info("[RESPONSE] > {}", REGULAR_MAPPER.writeValueAsString(output));
      }
    }
  }

}
