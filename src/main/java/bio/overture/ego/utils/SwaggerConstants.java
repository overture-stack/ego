package bio.overture.ego.utils;

import java.util.Set;

public class SwaggerConstants {
  public static final String AUTH_CONTROLLER = "auth-controller";
  public static final String POST_ACCESS_TOKEN = "postAccessToken";

  public static final String SECURITY_SCHEME_NAME = "Bearer";

  public static final Set<String> POST_ACCESS_TOKEN_PARAMS =
      Set.of("client_secret", "client_id", "grant_type");
  public static final Set<String> APPLICATION_SCOPED_PATHS =
      Set.of(
          "/o/check_api_key",
          "/o/check_token",
          "/transaction/group_permissions",
          "/transaction/mass_delete");
}
