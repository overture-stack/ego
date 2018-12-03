package bio.overture.ego.model.dto;

import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Data
@JsonInclude
@JsonView(Views.REST.class)
@AllArgsConstructor
public class PolicyResponse {
  public String id;
  public String name;
  public AccessLevel mask;
}
