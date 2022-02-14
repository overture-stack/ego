package bio.overture.ego.model.exceptions;

import org.springframework.security.oauth2.core.OAuth2ErrorCodes;

public class InvalidTokenException extends OAuth2Exception {
  public InvalidTokenException(String message) {
    super(OAuth2ErrorCodes.INVALID_TOKEN, message);
  }
}
