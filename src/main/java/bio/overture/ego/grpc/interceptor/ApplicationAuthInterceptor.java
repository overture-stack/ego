package bio.overture.ego.grpc.interceptor;

import static io.grpc.Metadata.ASCII_STRING_MARSHALLER;

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.ApplicationType;
import bio.overture.ego.model.enums.StatusType;
import bio.overture.ego.model.enums.UserType;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.service.UserService;
import io.grpc.*;
import java.util.Optional;
import java.util.UUID;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("auth")
@Primary
public class ApplicationAuthInterceptor implements AuthInterceptor {

  public static final Context.Key<String> AUTHORIZED_JWT = Context.key("jwt");
  public static final Context.Key<AuthInfo> AUTH_INFO = Context.key("auth_info");

  @Autowired private TokenService tokenService;
  @Autowired private UserService userService;
  @Autowired private ApplicationService applicationService;

  private static final Metadata.Key<String> JWT_METADATA_KEY =
      Metadata.Key.of("jwt", ASCII_STRING_MARSHALLER);

  private static final ServerCall.Listener NOOP_LISTENER = new ServerCall.Listener() {};

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata metadata, ServerCallHandler<ReqT, RespT> next) {
    try {
      // You need to implement validateIdentity
      String token = metadata.get(JWT_METADATA_KEY);

      val userInfo = getUserInfo(token);
      val appInfo = getAppInfo(token);

      if (!validateJwt(token) || !(userInfo.isPresent() || appInfo.isPresent())) {
        call.close(Status.UNAUTHENTICATED.withDescription("Invalid JWT"), metadata);
        return NOOP_LISTENER;
      }

      val isApproved =
          (userInfo.isPresent() && userInfo.get().getStatus() == StatusType.APPROVED)
              || (appInfo.isPresent() && appInfo.get().getStatus() == StatusType.APPROVED);

      if (!isApproved) {
        call.close(Status.UNAUTHENTICATED.withDescription("Status not approved."), metadata);
        return NOOP_LISTENER;
      }

      val isAdmin =
          (userInfo.isPresent() && userInfo.get().getType() == UserType.ADMIN)
              || (appInfo.isPresent() && appInfo.get().getType() == ApplicationType.ADMIN);
      val id = userInfo.isPresent() ? userInfo.get().getId() : appInfo.get().getId();

      val authInfo = new AuthInfo(userInfo.isPresent(), appInfo.isPresent(), isAdmin, id);

      Context context =
          Context.current().withValue(AUTHORIZED_JWT, token).withValue(AUTH_INFO, authInfo);
      return Contexts.interceptCall(context, call, metadata, next);

    } catch (IllegalArgumentException e) {
      call.close(Status.UNAUTHENTICATED.withDescription("Missing JWT"), metadata);
      return NOOP_LISTENER;
    }
  }

  private boolean validateJwt(String token) {
    return tokenService.isValidToken(token);
  }

  @SneakyThrows
  private Optional<User> getUserInfo(String token) {
    try {
      val claims = tokenService.getTokenUserInfo(token);
      return claims == null ? Optional.empty() : userService.findById(claims.getId());
    } catch (NotFoundException e) {
      return Optional.empty();
    }
  }

  @SneakyThrows
  private Optional<Application> getAppInfo(String token) {
    try {
      val claims = tokenService.getTokenAppInfo(token);
      return claims == null
          ? Optional.empty()
          : applicationService.getClientApplication(claims.getClientId());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Getter
  public class AuthInfo {

    boolean user;

    boolean app;

    boolean admin;

    UUID id;

    public AuthInfo(
        @NonNull final boolean isUser,
        @NonNull final boolean isApp,
        @NonNull final boolean isAdmin,
        @NonNull final UUID id) {
      this.user = isUser;
      this.app = isApp;
      this.admin = isAdmin;
      this.id = id;
    }
  }
}
