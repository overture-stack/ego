package bio.overture.ego.model.dto;

import bio.overture.ego.model.enums.AccessLevel;
import java.util.UUID;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VisaPermissionRequest {

  private UUID policyId;

  private UUID visaId;

  private AccessLevel accessLevel;
}
