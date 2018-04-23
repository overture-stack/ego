package org.overture.ego.token;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.overture.ego.view.Views;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
public abstract class TokenClaims {
  @JsonView(Views.JWTAccessToken.class)
  @NonNull
  protected Integer iat;

  @JsonView(Views.JWTAccessToken.class)
  @NonNull
  protected Integer exp;

  @NonNull
  @JsonIgnore
  protected Integer validDuration;

  @JsonView(Views.JWTAccessToken.class)
  @Getter
  protected String sub;

  @JsonView(Views.JWTAccessToken.class)
  @NonNull
  protected String iss;

  @JsonView(Views.JWTAccessToken.class)
  @Getter
  protected List<String> aud;

  /*
    Defaults
   */
  @JsonView(Views.JWTAccessToken.class)
  private String jti = UUID.randomUUID().toString();

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  @JsonIgnore
  private long initTime = System.currentTimeMillis();

  @JsonView(Views.JWTAccessToken.class)
  public int getExp(){
    return ((int) ((this.initTime + validDuration)/ 1000L));
  }

  @JsonView(Views.JWTAccessToken.class)
  public int getIat(){
    return (int) (this.initTime / 1000L);
  }

}
