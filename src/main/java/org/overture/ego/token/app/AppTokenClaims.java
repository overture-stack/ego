package org.overture.ego.token.app;

import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.overture.ego.token.TokenClaims;
import org.overture.ego.view.Views;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
public class AppTokenClaims extends TokenClaims {

  /*
  Constants
 */
  public static final String[] AUTHORIZED_GRANTS=
      {"authorization_code","client_credentials", "password", "refresh_token"};
  public static final String[] SCOPES = {"read","write", "delete"};
  public static final String ROLE = "ROLE_CLIENT";

  @NonNull
  @JsonView(Views.JWTAccessToken.class)
  private AppTokenContext context;

  @JsonView(Views.JWTAccessToken.class)
  public String getSub(){
    if(StringUtils.isEmpty(sub)) {
      return String.valueOf(this.context.getAppInfo().getId());
    } else {
      return sub;
    }
  }

  @JsonView(Views.JWTAccessToken.class)
  public List<String> getAud(){
    return Arrays.asList(this.context.getAppInfo().getName());
  }

}
