package bio.overture.ego.utils.web;

import static bio.overture.ego.utils.Collectors.toImmutableList;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Streams.stream;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import bio.overture.ego.utils.Streams;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import org.assertj.core.api.ListAssert;
import org.assertj.core.api.ObjectAssert;
import org.springframework.http.ResponseEntity;

public class StringResponseOption extends ResponseOption<String, StringResponseOption> {

  public static final ObjectMapper MAPPER = new ObjectMapper();

  public StringResponseOption(ResponseEntity<String> response) {
    super(response);
  }

  public <R> R extractOneEntity(@NonNull Class<R> entityClass) {
    return assertOk()
        .assertHasBody()
        .map(x -> internalExtractOneEntityFromResponse(x, entityClass));
  }

  public <R> StringResponseOption assertPageResultHasSize(
      @NonNull Class<R> entityClass, final int expectedSize) {
    assertEquals(extractPageResults(entityClass).size(), expectedSize);
    return this;
  }

  public <R> ListAssert<R> assertPageResultsOfType(Class<R> entityClass) {
    return assertThat(extractPageResults(entityClass));
  }

  public <R> ObjectAssert<R> assertEntityOfType(Class<R> entityClass) {
    return assertThat(extractOneEntity(entityClass));
  }

  public <R, O> Stream<O> transformPageResultsToStream(
      @NonNull Class<R> entityClass, @NonNull Function<R, O> transformingFunction) {
    return extractPageResults(entityClass).stream().map(transformingFunction);
  }

  public <R, O> List<O> transformPageResultsToList(
      @NonNull Class<R> entityClass, @NonNull Function<R, O> transformingFunction) {
    return transformPageResultsToStream(entityClass, transformingFunction)
        .collect(toUnmodifiableList());
  }

  public <R, O> Set<O> transformPageResultsToSet(
      @NonNull Class<R> entityClass, @NonNull Function<R, O> transformingFunction) {
    return transformPageResultsToStream(entityClass, transformingFunction)
        .collect(toUnmodifiableSet());
  }

  public <R> List<R> extractPageResults(@NonNull Class<R> entityClass) {
    return assertOk()
        .assertHasBody()
        .map(x -> internalExtractPageResultSetFromResponse(x, entityClass));
  }

  public <R> Set<R> extractManyEntities(@NonNull Class<R> entityClass) {
    return assertOk()
        .assertHasBody()
        .map(x -> internalExtractManyEntitiesFromResponse(x, entityClass));
  }

  @SneakyThrows
  private static <T> List<T> internalExtractPageResultSetFromResponse(
      ResponseEntity<String> r, Class<T> tClass) {
    val page = MAPPER.readTree(r.getBody());
    assertThat(page).isNotNull();
    return stream(page.path("resultSet").iterator())
        .map(x -> MAPPER.convertValue(x, tClass))
        .collect(toImmutableList());
  }

  @SneakyThrows
  private static <T> T internalExtractOneEntityFromResponse(
      ResponseEntity<String> r, Class<T> tClass) {
    return MAPPER.readValue(r.getBody(), tClass);
  }

  @SneakyThrows
  private static <T> Set<T> internalExtractManyEntitiesFromResponse(
      ResponseEntity<String> r, Class<T> tClass) {
    return Streams.stream(MAPPER.readTree(r.getBody()).iterator())
        .map(x -> MAPPER.convertValue(x, tClass))
        .collect(toImmutableSet());
  }
}
