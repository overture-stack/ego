package bio.overture.ego.model.exceptions;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.*;

import bio.overture.ego.utils.Joiners;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Date;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
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

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<Object> handleMissingRequestHeaderException(
      HttpServletRequest req, MissingRequestHeaderException ex) {
    val message = ex.getMessage();
    log.error(message);
    return new ResponseEntity<Object>(
        Map.of(
            "message", ex.getMessage(),
            "timestamp", new Date(),
            "path", req.getServletPath(),
            "error", UNAUTHORIZED.getReasonPhrase()),
        new HttpHeaders(),
        UNAUTHORIZED);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> handleConstraintViolationException(
      HttpServletRequest req, ConstraintViolationException ex) {
    val message = buildConstraintViolationMessage(ex);
    log.error(message);
    return new ResponseEntity<Object>(message, new HttpHeaders(), BAD_REQUEST);
  }

  private static String buildConstraintViolationMessage(ConstraintViolationException ex) {
    return format(
        "Constraint violation: [message] : %s ------- [violations] : %s",
        ex.getMessage(), Joiners.COMMA.join(ex.getConstraintViolations()));
  }
}
