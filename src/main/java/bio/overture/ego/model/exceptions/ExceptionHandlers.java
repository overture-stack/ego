package bio.overture.ego.model.exceptions;

import bio.overture.ego.utils.Joiners;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

import static java.lang.String.format;

@Slf4j
@ControllerAdvice
public class ExceptionHandlers {

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> handleConstraintViolationException(
      HttpServletRequest req, ConstraintViolationException ex) {
    val message = buildConstraintViolationMessage(ex);
    log.error(message);
    return new ResponseEntity<Object>(
        message, new HttpHeaders(), HttpStatus.BAD_REQUEST);
  }

  private static String buildConstraintViolationMessage(ConstraintViolationException ex){
    return format("Constraint violation: [message] : %s ------- [violations] : %s",
        ex.getMessage(), Joiners.COMMA.join(ex.getConstraintViolations()));
  }



}
