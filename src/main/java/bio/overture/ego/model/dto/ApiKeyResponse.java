package bio.overture.ego.model.dto;

import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.Date;
import java.util.Set;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonView(Views.REST.class)
public class ApiKeyResponse {

  @NonNull private String name;
  @NonNull private Set<String> scope;
  @NonNull private Date exp;
  @NonNull private Date iss;
  @NonNull private Boolean isRevoked;
  private String description;
}
