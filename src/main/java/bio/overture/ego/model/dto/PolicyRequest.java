package bio.overture.ego.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
