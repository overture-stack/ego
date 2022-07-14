package bio.overture.ego.model.dto;

import javax.validation.constraints.NotNull;

public class CreateUserCommand {
  @NotNull public String email;

  @NotNull public String firstName;

  @NotNull public String lastName;

  @NotNull public String providerType;

  @NotNull public String providerSubjectId;

  @NotNull public String providerAccessToken;

  @NotNull public Boolean includeGa4ghPermissions;
}
