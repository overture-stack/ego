package bio.overture.ego.model.dto;

import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.view.Views;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
/**
 * * A request create the group, policy and/or permission necessary to ensure that the specified
 * group exists, and has a group permission consisting of the specified policy at the specified
 * accessLevel.
 */
public class TransactionalGroupPermissionRequest {
  private String groupName;
  private String policyName;
  private AccessLevel mask;
}
