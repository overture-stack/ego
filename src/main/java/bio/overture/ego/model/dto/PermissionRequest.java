package bio.overture.ego.model.dto;

import bio.overture.ego.model.enums.AccessLevel;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionRequest {

  @NotNull @NonNull private UUID policyId;

  @NotNull @NonNull private AccessLevel mask;

  @Override
  public String toString() {
    return policyId + "." + mask;
  }
}
