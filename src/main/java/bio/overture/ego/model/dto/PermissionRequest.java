package bio.overture.ego.model.dto;

import bio.overture.ego.model.enums.AccessLevel;
import java.util.UUID;
import javax.validation.constraints.NotNull;
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
