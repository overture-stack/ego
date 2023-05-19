package bio.overture.ego.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
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
  @NotNull
  private String sub;

  @JsonProperty("iss")
  @NotNull
  private String iss;

  @JsonProperty("exp")
  @NotNull
  private int exp;

  @JsonProperty("iat")
  @NotNull
  private int iat;

  @JsonProperty("ga4gh_passport_v1")
  @NotNull
  private List<String> ga4ghPassportV1;

  @JsonProperty("jti")
  @NotNull
  private String jti;
}
