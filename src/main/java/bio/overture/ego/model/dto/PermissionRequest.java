package bio.overture.ego.model.dto;

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
  @NonNull private String policyId;
  @NonNull private String mask;

  @Override
  public String toString() {
    return policyId + "." + mask;
  }
}
