package bio.overture.ego.model.exceptions;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.*;

import bio.overture.ego.utils.Joiners;
import java.util.Date;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class ExceptionHandlers {

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Object> handleConstraintViolationException(
      HttpServletRequest req, NotFoundException ex) {
    val message = ex.getMessage();
    log.error(message);
    return new ResponseEntity<Object>(
        Map.of(
            "message", ex.getMessage(),
            "timestamp", new Date(),
            "path", req.getServletPath(),
            "error", NOT_FOUND.getReasonPhrase()),
        new HttpHeaders(),
        NOT_FOUND);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> handleConstraintViolationException(
      HttpServletRequest req, ConstraintViolationException ex) {
    val message = buildConstraintViolationMessage(ex);
    log.error(message);
    return new ResponseEntity<Object>(message, new HttpHeaders(), BAD_REQUEST);
  }

  @ExceptionHandler(ClientInvalidTokenException.class)
  public ResponseEntity<Object> handleInvalidTokenException(
      HttpServletRequest req, ClientInvalidTokenException ex) {
    val message = ex.getMessage();
    log.error(message);
    return new ResponseEntity<Object>(
        Map.of("valid", false, "error", message), new HttpHeaders(), OK);
  }

  private static String buildConstraintViolationMessage(ConstraintViolationException ex) {
    return format(
        "Constraint violation: [message] : %s ------- [violations] : %s",
        ex.getMessage(), Joiners.COMMA.join(ex.getConstraintViolations()));
  }
}
