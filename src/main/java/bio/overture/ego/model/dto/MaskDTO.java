package bio.overture.ego.model.dto;

import bio.overture.ego.model.enums.AccessLevel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MaskDTO {

  @NotNull @NonNull private AccessLevel mask;

  public static MaskDTO createMaskDTO(AccessLevel mask) {
    return new MaskDTO(mask);
  }
}
