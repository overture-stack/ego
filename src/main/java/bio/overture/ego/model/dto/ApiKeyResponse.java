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
  @NonNull private final String name;
  @NonNull private final Set<String> scope;
  @NonNull private final Date exp;
  @NonNull private final Date iss;
  @NonNull private final Boolean isRevoked;
  @NonNull private final Long secondsUntilExpiry;
  private String description;
}
