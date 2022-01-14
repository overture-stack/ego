package bio.overture.ego.model.exceptions;

public class OAuth2Exception extends RuntimeException {
  protected String code;

  public OAuth2Exception(String code, String message) {
    super(message);
    this.code = code;
  }

  public String getCode() {
    return code;
  }

  @Override
  public String getMessage() {
    return super.getMessage();
  }
}
