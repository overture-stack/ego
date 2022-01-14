package bio.overture.ego.model.exceptions;

import org.springframework.security.oauth2.core.OAuth2ErrorCodes;

public class UnauthorizedClientException extends OAuth2Exception {
  public UnauthorizedClientException(String message) {
    super(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT, message);
  }
}
