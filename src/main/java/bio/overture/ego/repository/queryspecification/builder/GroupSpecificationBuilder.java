package bio.overture.ego.repository.queryspecification.builder;

import static bio.overture.ego.model.enums.JavaFields.APPLICATIONS;
import static bio.overture.ego.model.enums.JavaFields.PERMISSIONS;
import static bio.overture.ego.model.enums.JavaFields.USER;
import static bio.overture.ego.model.enums.JavaFields.USERGROUPS;
import static javax.persistence.criteria.JoinType.LEFT;

import bio.overture.ego.model.entity.Group;
import java.util.UUID;
import javax.persistence.criteria.Root;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;

@Setter
@Accessors(fluent = true, chain = true)
public class GroupSpecificationBuilder extends AbstractSpecificationBuilder<Group, UUID> {

  private boolean fetchApplications;
  private boolean fetchUserGroups;
  private boolean fetchGroupPermissions;

  @Override
  protected Root<Group> setupFetchStrategy(Root<Group> root) {
    if (fetchApplications) {
      root.fetch(APPLICATIONS, LEFT);
    }
    if (fetchUserGroups) {
      val fromUserGroup = root.fetch(USERGROUPS, LEFT);
      fromUserGroup.fetch(USER, LEFT);
    }
    if (fetchGroupPermissions) {
      root.fetch(PERMISSIONS, LEFT);
    }
    return root;
  }
}
