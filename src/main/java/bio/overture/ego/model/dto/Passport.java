package bio.overture.ego.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Passport {

  @JsonProperty("sub")
  private String sub;

  @JsonProperty("iss")
  private String iss;

  @JsonProperty("exp")
  private long exp;

  @JsonProperty("iat")
  private int iat;

  @JsonProperty("ga4gh_passport_v1")
  private List<String> ga4ghPassportV1;

  @JsonProperty("jti")
  private String jti;
}
