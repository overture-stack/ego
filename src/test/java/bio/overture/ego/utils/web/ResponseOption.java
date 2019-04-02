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
public class ResponseOption<T, O extends ResponseOption<T, O>> {

  @Getter
  @NonNull private final ResponseEntity<T> response;

  public O assertStatusCode(HttpStatus code) {
    assertThat(response.getStatusCode()).isEqualTo(code);
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
    assertThat(response.hasBody()).isTrue();
    assertThat(response.getBody()).isNotNull();
    return thisInstance();
  }

  public <R> R map(Function<ResponseEntity<T>, R> transformingFunction) {
    return transformingFunction.apply(getResponse());
  }

  private O thisInstance(){
    return (O)this;
  }

}
