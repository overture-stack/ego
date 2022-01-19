package bio.overture.ego.model.exceptions;

import org.springframework.security.oauth2.core.OAuth2ErrorCodes;

public class InvalidRequestException extends OAuth2Exception {
  public InvalidRequestException(String message) {
    super(OAuth2ErrorCodes.INVALID_REQUEST, message);
  }
}
