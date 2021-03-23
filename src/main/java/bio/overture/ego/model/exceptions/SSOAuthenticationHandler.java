package bio.overture.ego.model.exceptions;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import bio.overture.ego.model.enums.ProviderType;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.utils.Redirects;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SSOAuthenticationHandler implements AuthenticationFailureHandler {

  // constants
  private static final String ERROR_CODE_PARAM = "error_code";
  private static final String ERROR_TYPE_PARAM = "error_type";
  private static final String PROVIDER_TYPE_PARAM = "provider_type";

  private ApplicationService applicationService;

  public SSOAuthenticationHandler(@NonNull ApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    val rootExceptionThrowable = exception.getCause();
    val application =
        applicationService.getByClientId(
            (String) request.getSession().getAttribute("ego_client_id"));
    String errorRedirectUri = (String) request.getSession().getAttribute("ego_error_redirect_uri");

    val errorRedirect = Redirects.getErrorRedirectUri(application, errorRedirectUri);

    try {
      URIBuilder errorUri = new URIBuilder(errorRedirect);
      errorUri.addParameter(ERROR_CODE_PARAM, "403");
      if (rootExceptionThrowable instanceof NoPrimaryEmailException) {
        errorUri = buildNoPrimaryExceptionResponse(request, errorUri);
      }
      if (rootExceptionThrowable instanceof OAuth2Exception) {
        errorUri = buildOAuth2ExceptionResponse(errorUri);
      }
      response.setStatus(403);
      response.sendRedirect(errorUri.toString());
    } catch (URISyntaxException e) {
      log.warn("Invalid redirect uri from application");
      throw new InternalServerException("Invalid redirect uri");
    }
  }

  // A user's email is not visible in the IdP token response
  public URIBuilder buildNoPrimaryExceptionResponse(HttpServletRequest req, URIBuilder uri)
      throws InternalServerException {
    val reqUri = new ArrayList<>(asList(req.getRequestURI().split("/")));
    val provider = reqUri.get(reqUri.size() - 1).toUpperCase();
    uri.addParameter(ERROR_TYPE_PARAM, "no_primary_email");
    try {
      ProviderType.resolveProviderType(provider);
      uri.addParameter(PROVIDER_TYPE_PARAM, provider);
    } catch (IllegalArgumentException e) {
      log.warn(format("Invalid provider: '%s'", provider));
      throw new IllegalArgumentException(format("Invalid provider: '%s'", provider));
    }
    return uri;
  }

  // A user denies Ego access/cancels login
  // - Google does not have a cancel/deny option
  // - Orcid and Github throw a UserDeniedAuthorizationException, with params
  // error=access_denied
  // - LinkedIn throws an OAuth2Exception with param error=invalid_request,
  // so catching this + Orcid and Github under that ex type, as UserDeniedAuthorizationException
  // inherits from OAuth2Exception
  public URIBuilder buildOAuth2ExceptionResponse(URIBuilder uri) {
    uri.addParameter(ERROR_TYPE_PARAM, "access_denied");
    return uri;
  }
}
