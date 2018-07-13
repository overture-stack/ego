package org.overture.ego.model.params;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Permission {
  @NonNull
  private String aclEntityId;
  @NonNull
  private String mask;
}
