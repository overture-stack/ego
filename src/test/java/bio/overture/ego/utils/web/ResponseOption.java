package bio.overture.ego.utils.web;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.function.Function;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@RequiredArgsConstructor
public class ResponseOption<T> {

  @Getter
  @NonNull private final ResponseEntity<T> response;

  public ResponseOption<T> assertStatusCode(HttpStatus code) {
    assertThat(response.getStatusCode()).isEqualTo(code);
    return this;
  }

  public ResponseOption<T> assertOk() {
    return assertStatusCode(OK);
  }

  public ResponseOption<T> assertNotFound() {
    return assertStatusCode(NOT_FOUND);
  }

  public ResponseOption<T> assertConflict() {
    return assertStatusCode(CONFLICT);
  }

  public ResponseOption<T> assertBadRequest() {
   return assertStatusCode(BAD_REQUEST);
  }

  public ResponseOption<T> assertHasBody() {
    assertThat(response.hasBody()).isTrue();
    assertThat(response.getBody()).isNotNull();
    return this;
  }

  public <R> R map(Function<ResponseEntity<T>, R> transformingFunction) {
    return transformingFunction.apply(getResponse());
  }

}
