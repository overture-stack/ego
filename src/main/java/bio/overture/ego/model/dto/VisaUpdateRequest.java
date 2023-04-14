package bio.overture.ego.model.dto;

import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VisaUpdateRequest {

  @NotNull private UUID id;

  @NotNull private String type;

  @NotNull private String source;

  @NotNull private String value;

  @NotNull private String by;
}
