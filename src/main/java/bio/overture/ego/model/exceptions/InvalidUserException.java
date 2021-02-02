package bio.overture.ego.model.exceptions;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.FORBIDDEN;

import lombok.NonNull;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(FORBIDDEN)
public class InvalidUserException extends RuntimeException {
  public InvalidUserException(@NonNull String message) {
    super(message);
  }

  public static void checkValidUser(
      boolean expression, @NonNull String formattedMessage, @NonNull Object... args) {
    if (!expression) {
      throw new InvalidUserException(format(formattedMessage, args));
    }
  }

  public static InvalidUserException buildInvalidUserException(
      @NonNull String formattedMessage, Object... args) {
    return new InvalidUserException(format(formattedMessage, args));
  }
}
