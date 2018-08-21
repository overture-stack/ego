package org.overture.ego.model.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value= HttpStatus.BAD_REQUEST, reason=DontSetIdInCreateRequestException.reason)
public class DontSetIdInCreateRequestException extends RuntimeException {
  public static final String reason="Create requests must not include the 'id' field.";
}
