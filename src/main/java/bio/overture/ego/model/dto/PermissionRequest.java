package bio.overture.ego.model.dto;

import bio.overture.ego.model.enums.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionRequest {

  @NonNull private UUID policyId;

  @NonNull private AccessLevel mask;

  @Override
  public String toString() {
    return policyId + "." + mask;
  }

}