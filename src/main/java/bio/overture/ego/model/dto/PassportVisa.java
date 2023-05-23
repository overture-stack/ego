package bio.overture.ego.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PassportVisa {

  @JsonProperty("sub")
  private String sub;

  @JsonProperty("ga4gh_visa_v1")
  private Ga4ghVisaV1 ga4ghVisaV1;

  @JsonProperty("iss")
  private String iss;

  @JsonProperty("exp")
  private long exp;

  @JsonProperty("iat")
  private int iat;

  @JsonProperty("jti")
  private String jti;
}
