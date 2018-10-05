package org.overture.ego.model.dto;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.overture.ego.view.Views;

import java.util.Set;

@AllArgsConstructor
@Getter
@JsonView(Views.REST.class)
public class TokenResponse {
  String accessToken;
  private Set<String> scope;
  private Long exp;
  public String getTokenType() {
    return "Bearer";
  }
}
