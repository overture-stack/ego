package org.overture.ego.model.params;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class ScopeName {
  @NonNull
  private String policyId;
  @NonNull
  private String mask;

  public ScopeName(String name) {

  }
  @Override
  public String toString() {
    return policyId + ":" + mask;
  }
}
