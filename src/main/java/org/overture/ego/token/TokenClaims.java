package org.overture.ego.token;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.overture.ego.view.Views;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@JsonView(Views.JWTAccessToken.class)
public abstract class TokenClaims {
  @NonNull
  protected Integer iat;

  @NonNull
  protected Integer exp;

  @NonNull
  @JsonIgnore
  protected Integer validDuration;

  @Getter
  protected String sub;

  @NonNull
  protected String iss;

  @Getter
  protected List<String> aud;

  /*
    Defaults
   */
  private String jti = UUID.randomUUID().toString();

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  @JsonIgnore
  private long initTime = System.currentTimeMillis();

  public int getExp(){
    return ((int) ((this.initTime + validDuration)/ 1000L));
  }

  public int getIat(){
    return (int) (this.initTime / 1000L);
  }

}
