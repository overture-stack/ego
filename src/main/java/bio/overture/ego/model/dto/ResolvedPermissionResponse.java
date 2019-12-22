package bio.overture.ego.model.dto;

import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.enums.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class ResolvedPermissionResponse {
  @NonNull private final Policy policy;
  @NonNull private final AccessLevel accessLevel;
  @NonNull private final String ownerType;
  @NonNull private final Identifiable owner;
  @NonNull private final UUID id;
}
