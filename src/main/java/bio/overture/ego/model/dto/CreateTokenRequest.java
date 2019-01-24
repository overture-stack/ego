package bio.overture.ego.model.dto;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTokenRequest {

  private String token;
  private Date expires;
  private boolean isRevoked;
}
