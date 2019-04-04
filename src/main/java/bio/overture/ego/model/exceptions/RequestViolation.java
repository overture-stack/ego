package bio.overture.ego.model.exceptions;

import javax.validation.ConstraintViolation;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class RequestViolation {
  @NonNull private final String fieldName;
  private final Object fieldValue;
  @NonNull private final String error;

  public static <T> RequestViolation createRequestViolation(ConstraintViolation<T> v) {
    return RequestViolation.builder()
        .error(v.getMessage())
        .fieldName(v.getPropertyPath().toString())
        .fieldValue(v.getInvalidValue())
        .build();
  }
}
