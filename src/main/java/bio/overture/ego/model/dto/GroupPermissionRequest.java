package bio.overture.ego.model.dto;

import bio.overture.ego.model.enums.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupPermissionRequest {
  private String groupName;
  private String policyName;
  private AccessLevel mask;
}
