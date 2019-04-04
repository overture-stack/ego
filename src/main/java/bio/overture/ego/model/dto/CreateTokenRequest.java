package bio.overture.ego.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTokenRequest {

  @NotNull private String token;

  @NonNull private Date issueDate;

  @NotNull private boolean isRevoked;
}
