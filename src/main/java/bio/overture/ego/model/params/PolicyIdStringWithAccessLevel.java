package bio.overture.ego.model.params;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class PolicyIdStringWithAccessLevel {
  @NonNull private String policyId;
  @NonNull private String mask;

  @Override
  public String toString() {
    return policyId + "." + mask;
  }
}
