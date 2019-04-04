package bio.overture.ego.model.exceptions;

import lombok.NonNull;
import lombok.val;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.Validation;
import javax.validation.Validator;

import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Joiners.PRETTY_COMMA;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(BAD_REQUEST)
public class RequestValidationException extends RuntimeException {

  /**
   * Validator is thread-safe so can be a constant
   * https://docs.jboss.org/hibernate/stable/validator/reference/en-US/html_single/#_validating_constraints
   */
  private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

  public RequestValidationException(String message) {
    super(message);
  }

  public static <T> void checkRequestValid(@NonNull T objectToValidate ) {
    val errors = VALIDATOR.validate(objectToValidate);
    if (!errors.isEmpty()) {
      val requestViolations = errors.stream().map(RequestViolation::createRequestViolation).collect(toImmutableSet());
      val formattedMessage = "The object of type '%s' with value '%s' has the following constraint violations: [%s]";
      throw new RequestValidationException(format(formattedMessage,
          objectToValidate.getClass().getSimpleName(),
          objectToValidate,
          PRETTY_COMMA.join(requestViolations)));
    }
  }

}
