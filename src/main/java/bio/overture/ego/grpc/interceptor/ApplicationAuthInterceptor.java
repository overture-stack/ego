package bio.overture.ego.grpc.interceptor;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import bio.overture.ego.model.enums.ApplicationType;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.TokenService;
import io.grpc.*;
import io.jsonwebtoken.Claims;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("auth")
@Primary
public class ApplicationAuthInterceptor implements AuthInterceptor {

  @Autowired private TokenService tokenService;
  @Autowired private ApplicationService applicationService;

  public static final Metadata.Key<String> JWT_METADATA_KEY =
      Metadata.Key.of("jwt", ASCII_STRING_MARSHALLER);

  private static final ServerCall.Listener NOOP_LISTENER = new ServerCall.Listener() {};

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata metadata, ServerCallHandler<ReqT, RespT> next) {
    try {
      // You need to implement validateIdentity
      String token = metadata.get(JWT_METADATA_KEY);

      if (!validateJwt(token)) {
        call.close(Status.UNAUTHENTICATED.withDescription("Invalid JWT"), metadata);
        return NOOP_LISTENER;
      }

      if (!checkAuthorization(token)) {
        call.close(
            Status.PERMISSION_DENIED.withDescription(
                "This application is not authorized to access Ego data."),
            metadata);
        return NOOP_LISTENER;
      }

      Context context = Context.current(); // .withValue(JWT_CONTEXT_KEY, claims);
      return Contexts.interceptCall(context, call, metadata, next);

    } catch (IllegalArgumentException e) {
      call.close(Status.UNAUTHENTICATED.withDescription("Missing JWT"), metadata);
      return NOOP_LISTENER;
    }
  }

  private boolean validateJwt(String token) {
    return tokenService.isValidToken(token);
  }

  private boolean checkAuthorization(String token) {
    val claims = tokenService.getTokenAppInfo(token);
    if (claims == null) {
      return false;
    }

    val application = applicationService.getByClientId(claims.getClientId());

    return application.getType().equals(ApplicationType.ADMIN);
  }
}
