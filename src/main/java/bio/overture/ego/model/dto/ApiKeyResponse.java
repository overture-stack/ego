package bio.overture.ego.model.dto;

import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.Date;
import java.util.Set;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@JsonView(Views.REST.class)
public class ApiKeyResponse {
  @NonNull private final String apiKey;
  @NonNull private final Set<String> scope;
  @NonNull private final Long exp;
  @NonNull private final Date iss;
  private String description;
}
