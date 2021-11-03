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
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SSOAuthenticationFailureHandler implements AuthenticationFailureHandler {

  // constants
  private static final String ERROR_CODE_PARAM = "error_code";
  private static final String ERROR_TYPE_PARAM = "error_type";
  private static final String PROVIDER_TYPE_PARAM = "provider_type";

  private ApplicationService applicationService;

  public SSOAuthenticationFailureHandler(@NonNull ApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @Override
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    val rootExceptionThrowable = exception.getCause() == null ? exception : exception.getCause();
    val application =
        applicationService.getByClientId(
            (String) request.getSession().getAttribute("ego_client_id"));
    String errorRedirectUri = (String) request.getSession().getAttribute("ego_error_redirect_uri");

    val errorRedirect = Redirects.getErrorRedirectUri(application, errorRedirectUri);

    try {
      URIBuilder errorUri = new URIBuilder(errorRedirect);
      errorUri.addParameter(ERROR_CODE_PARAM, "403");
      val reqUri = new ArrayList<>(asList(request.getRequestURI().split("/")));
      val providerType = reqUri.get(reqUri.size() - 1).toUpperCase();
      if (rootExceptionThrowable instanceof NoPrimaryEmailException) {
        errorUri = buildNoPrimaryEmailExceptionResponse(errorUri, providerType);
      } else if (rootExceptionThrowable instanceof OAuth2AuthenticationException) {
        errorUri = buildOAuth2ExceptionResponse(errorUri, providerType);
      } else {
        throw new InternalServerException("Invalid response from OAuth Service");
      }
      response.setStatus(403);
      response.sendRedirect(errorUri.toString());
    } catch (URISyntaxException e) {
      val errMessage = format("Invalid redirect uri from application: '%s", application.getName());
      log.warn(errMessage);
      throw new InternalServerException(errMessage);
    }
  }

  // A user's email is not visible in the IdP token response
  public URIBuilder buildNoPrimaryEmailExceptionResponse(URIBuilder uri, String providerType)
      throws InternalServerException {
    uri.addParameter(ERROR_TYPE_PARAM, "no_primary_email");
    return buildUriWithProviderTypeParam(uri, providerType);
  }

  // A user denies Ego access/cancels login, catch both as "access_denied"
  // - Google does not have a cancel/deny option
  // - Orcid and Github throw a UserDeniedAuthorizationException, with params
  // error=access_denied
  // - LinkedIn throws an OAuth2Exception with param error=invalid_request,
  // so catching this + Orcid and Github under that ex type, as UserDeniedAuthorizationException
  // inherits from OAuth2Exception
  public URIBuilder buildOAuth2ExceptionResponse(URIBuilder uri, String providerType) {
    uri.addParameter(ERROR_TYPE_PARAM, "access_denied");
    return buildUriWithProviderTypeParam(uri, providerType);
  }

  private URIBuilder buildUriWithProviderTypeParam(URIBuilder uri, String provider) {
    try {
      ProviderType.resolveProviderType(provider);
      uri.addParameter(PROVIDER_TYPE_PARAM, provider);
    } catch (IllegalArgumentException e) {
      val errMessage = format("Invalid provider: '%s'", provider);
      log.warn(errMessage);
      throw new IllegalArgumentException(errMessage);
    }
    return uri;
  }
}
