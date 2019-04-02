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
    assertStatusCode(OK);
    return this;
  }

  public ResponseOption<T> assertNotFound() {
    assertStatusCode(NOT_FOUND);
    return this;
  }

  public ResponseOption<T> assertConflict() {
    assertStatusCode(CONFLICT);
    return this;
  }

  public ResponseOption<T> assertBadRequest() {
    assertStatusCode(BAD_REQUEST);
    return this;
  }

  public ResponseOption<T> assertHasBody() {
    assertThat(response.hasBody()).isTrue();
    return this;
  }

  public <R> R map(Function<ResponseEntity<T>, R> transformingFunction) {
    return transformingFunction.apply(getResponse());
  }

}
