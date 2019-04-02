package bio.overture.ego.utils.web;

import bio.overture.ego.utils.Streams;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.assertj.core.api.ListAssert;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Set;

import static bio.overture.ego.utils.Collectors.toImmutableList;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Streams.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

public class StringResponseOption extends ResponseOption<String>{

  public static final ObjectMapper MAPPER = new ObjectMapper();

  public StringResponseOption(ResponseEntity<String> response) {
    super(response);
  }

  public <R> R extractOneEntity(@NonNull Class<R> entityClass){
    return assertOk().assertHasBody().map(x -> extractOneEntityFromResponse(x, entityClass));
  }

  public <R> ListAssert<R> assertPageResultsOfType(Class<R> entityClass){
    return assertThat(extractPageResults(entityClass));
  }

  public <R> List<R> extractPageResults(@NonNull Class<R> entityClass){
    return assertOk()
        .assertHasBody()
        .map(x -> extractPageResultSetFromResponse(x, entityClass));
  }

  public <R> Set<R> extractManyEntities(@NonNull Class<R> entityClass){
    return assertOk()
        .assertHasBody()
        .map(x -> extractManyEntitiesFromResponse(x, entityClass));
  }

  @SneakyThrows
  public static <T> List<T> internalExtractPageResultSetFromResponse(ResponseEntity<String> r, Class<T> tClass) {
    val page = MAPPER.readTree(r.getBody());
    assertThat(page).isNotNull();
    return stream(page.path("resultSet").iterator())
        .map(x -> MAPPER.convertValue(x, tClass))
        .collect(toImmutableList());
  }

  @SneakyThrows
  public static <T> T internalExtractOneEntityFromResponse(ResponseEntity<String> r, Class<T> tClass) {
    return MAPPER.readValue(r.getBody(), tClass);
  }

  @SneakyThrows
  public static <T> Set<T> internalExtractManyEntitiesFromResponse(ResponseEntity<String> r, Class<T> tClass) {
    return Streams.stream(MAPPER.readTree(r.getBody()).iterator())
        .map(x -> MAPPER.convertValue(x, tClass))
        .collect(toImmutableSet());
  }

  @SneakyThrows
  public static <T> List<T> extractPageResultSetFromResponse(ResponseEntity<String> r, Class<T> tClass) {
    assertThat(r.getStatusCode()).isEqualTo(OK);
    assertThat(r.getBody()).isNotNull();
    return internalExtractPageResultSetFromResponse(r, tClass);
  }

  @SneakyThrows
  public static <T> T extractOneEntityFromResponse(ResponseEntity<String> r, Class<T> tClass) {
    assertThat(r.getStatusCode()).isEqualTo(OK);
    assertThat(r.getBody()).isNotNull();
    return internalExtractOneEntityFromResponse(r, tClass);
  }

  @SneakyThrows
  public static <T> Set<T> extractManyEntitiesFromResponse(ResponseEntity<String> r, Class<T> tClass) {
    assertThat(r.getStatusCode()).isEqualTo(OK);
    assertThat(r.getBody()).isNotNull();
    return internalExtractManyEntitiesFromResponse(r, tClass);
  }

}
