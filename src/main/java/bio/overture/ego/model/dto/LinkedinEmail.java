package bio.overture.ego.model.dto;

import static java.util.Objects.isNull;

import bio.overture.ego.model.enums.LinkedinContactType;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.LinkedHashMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonInclude
@JsonView(Views.REST.class)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LinkedinEmail {

  @JsonProperty("handle~")
  private LinkedHashMap<String, String> handle;

  private boolean primary;
  private LinkedinContactType type;

  public String getEmail() {
    if (isNull(this.handle) || isNull(this.handle.get("emailAddress"))) {
      return null;
    }
    return this.handle.get("emailAddress");
  }
}
