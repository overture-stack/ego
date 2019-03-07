package bio.overture.ego.model.dto;

import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Set;

@Value
@Builder
@JsonView(Views.REST.class)
public class TokenResponse {
  @NonNull private final String accessToken;
  @NonNull private final Set<String> scope;
  @NonNull private final Long exp;
  private String description;
}
