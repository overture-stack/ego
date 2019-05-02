package bio.overture.ego.repository.queryspecification.builder;

import static bio.overture.ego.model.enums.JavaFields.*;
import static javax.persistence.criteria.JoinType.LEFT;

import bio.overture.ego.model.entity.User;
import java.util.UUID;
import javax.persistence.criteria.Root;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(fluent = true, chain = true)
public class UserSpecificationBuilder extends AbstractSpecificationBuilder<User, UUID> {

  private boolean fetchUserPermissions;
  private boolean fetchUserGroups;
  private boolean fetchApplications;

  @Override
  protected Root<User> setupFetchStrategy(Root<User> root) {
    if (fetchApplications) {
      root.fetch(APPLICATIONS, LEFT);
    }
    if (fetchUserGroups) {
      root.fetch(USERGROUPS, LEFT).fetch(GROUP, LEFT);
    }
    if (fetchUserPermissions) {
      root.fetch(USERPERMISSIONS, LEFT).fetch(POLICY, LEFT);
      root.fetch(USERGROUPS, LEFT).fetch(GROUP, LEFT).fetch(PERMISSIONS, LEFT).fetch(POLICY, LEFT);
    }
    return root;
  }
}
