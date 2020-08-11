package bio.overture.ego.model.dto;

import bio.overture.ego.model.entity.*;
import bio.overture.ego.model.enums.AccessLevel;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;
import lombok.*;

@Data
@JsonInclude(JsonInclude.Include.CUSTOM)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResolvedPermissionResponse {
  @NonNull private Policy policy;
  @NonNull private AccessLevel accessLevel;
  @NonNull private String ownerType;

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.CLASS,
      include = JsonTypeInfo.As.PROPERTY,
      property = "className")
  @JsonSubTypes({
    @JsonSubTypes.Type(value = Group.class, name = "group"),
    @JsonSubTypes.Type(value = User.class, name = "user"),
    @JsonSubTypes.Type(value = Application.class, name = "application")
  })
  @NonNull
  private Identifiable owner;

  @NonNull private UUID id;
}
