package bio.overture.ego.model.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PolicyRequest {

  @NotNull
  @Pattern(regexp = "^[A-Za-z0-9_-]+$")
  private String name;
}
