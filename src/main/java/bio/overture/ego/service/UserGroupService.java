package bio.overture.ego.service;

import bio.overture.ego.model.join.UserGroup;
import lombok.NonNull;

public class UserGroupService {

  public static void associateSelf(@NonNull UserGroup ug) {
    ug.getGroup().getUserGroups().add(ug);
    ug.getUser().getUserGroups().add(ug);
  }
}
