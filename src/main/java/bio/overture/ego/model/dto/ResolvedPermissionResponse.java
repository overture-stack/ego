package bio.overture.ego.model.dto;

import bio.overture.ego.model.entity.*;
import bio.overture.ego.model.enums.AccessLevel;
import java.util.UUID;
import lombok.*;

@Value
@Builder
public class ResolvedPermissionResponse {
  @NonNull private final Policy policy;
  @NonNull private final AccessLevel accessLevel;
  @NonNull private final String ownerType;
  @NonNull private final Identifiable owner;
  @NonNull private final UUID id;
}
