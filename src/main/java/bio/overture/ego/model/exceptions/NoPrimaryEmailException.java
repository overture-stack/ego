package bio.overture.ego.model.exceptions;

import static org.springframework.http.HttpStatus.FORBIDDEN;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ResponseStatus(FORBIDDEN)
public class NoPrimaryEmailException extends AccessDeniedException {

  public NoPrimaryEmailException(String msg) {
    super(msg);
    log.info("in the error");
  }
}
