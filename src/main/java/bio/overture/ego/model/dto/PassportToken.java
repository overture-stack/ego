package bio.overture.ego.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PassportToken {

  private String access_token;
  private String refresh_token;
  private Long expires_in;
  private String issued_token_type;
}
