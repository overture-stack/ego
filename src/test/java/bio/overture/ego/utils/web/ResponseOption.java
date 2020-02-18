package bio.overture.ego.utils.web;

import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

import java.util.function.Function;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.junit.Assert;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RequiredArgsConstructor
public class ResponseOption<T, O extends ResponseOption<T, O>> {

  @Getter @NonNull private final ResponseEntity<T> response;

  public O assertStatusCode(HttpStatus code) {

    assertEquals(response.getStatusCode(), code);
    return thisInstance();
  }

  public O assertOk() {
    return assertStatusCode(OK);
  }

  public O assertNotFound() {
    return assertStatusCode(NOT_FOUND);
  }

  public O assertConflict() {
    return assertStatusCode(CONFLICT);
  }

  public O assertBadRequest() {
    return assertStatusCode(BAD_REQUEST);
  }

  public O assertHasBody() {
    Assert.assertTrue(response.hasBody());
    Assert.assertNotNull(response.getBody());
    return thisInstance();
  }

  public <R> R map(Function<ResponseEntity<T>, R> transformingFunction) {
    return transformingFunction.apply(getResponse());
  }

  private O thisInstance() {
    return (O) this;
  }
}
