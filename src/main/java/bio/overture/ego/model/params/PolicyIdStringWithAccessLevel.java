package bio.overture.ego.model.params;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PolicyIdStringWithAccessLevel {
  @NonNull private String policyId;
  @NonNull private String mask;

  @Override
  public String toString() {
    return policyId + "." + mask;
  }
}
