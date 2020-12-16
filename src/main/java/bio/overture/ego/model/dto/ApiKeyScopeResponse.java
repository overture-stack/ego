package bio.overture.ego.model.dto;

import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@JsonView(Views.REST.class)
public class ApiKeyScopeResponse {
  private UUID user_id;
  private String client_id;
  private Long exp;
  private Set<String> scope;
}
