package bio.overture.ego.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PassportRefreshTokenResponse {
  private String access_token;
  private String token_type;
  private String refresh_token;
  private Long expires_in;
  private String scope;
  private String id_token;
}
