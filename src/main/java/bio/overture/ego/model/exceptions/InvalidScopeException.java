package bio.overture.ego.model.exceptions;

import org.springframework.security.oauth2.core.OAuth2ErrorCodes;

public class InvalidScopeException extends OAuth2Exception {
  public InvalidScopeException(String message) {
    super(OAuth2ErrorCodes.INVALID_SCOPE, message);
  }
}
