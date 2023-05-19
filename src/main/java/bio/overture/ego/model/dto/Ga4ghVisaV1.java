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
public class Ga4ghVisaV1 {

  @JsonProperty("asserted")
  private int asserted;

  @JsonProperty("by")
  private String by;

  @JsonProperty("source")
  private Object source;

  @JsonProperty("type")
  private String type;

  @JsonProperty("value")
  private String value;
}
