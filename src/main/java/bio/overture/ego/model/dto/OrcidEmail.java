package bio.overture.ego.model.dto;

import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
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
public class OrcidEmail {

  private String email;

  private boolean primary;

  private boolean verified;

  private String visibility;
}
