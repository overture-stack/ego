package org.overture.ego.token.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.overture.ego.model.entity.Application;
import org.overture.ego.view.Views;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonView(Views.JWTAccessToken.class)
public class AppTokenContext {

  @NonNull
  @JsonProperty("application")
  private Application appInfo;
}
