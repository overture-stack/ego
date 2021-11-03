package bio.overture.ego.model.exceptions;

import static org.springframework.http.HttpStatus.FORBIDDEN;

import bio.overture.ego.service.InvalidTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ResponseStatus(FORBIDDEN)
public class NoPrimaryEmailException extends InvalidTokenException {

  public NoPrimaryEmailException(String msg) {
    super(msg);
  }
}
