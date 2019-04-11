package bio.overture.ego.model.dto;

import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
@JsonView(Views.REST.class)
public class TokenScopeResponse {
  private String user_name;
  private String client_id;
  private Long exp;
  private Set<String> scope;
}
