package bio.overture.ego.repository.queryspecification.builder;

import bio.overture.ego.model.entity.Group;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;

import javax.persistence.criteria.Root;
import java.util.UUID;

import static bio.overture.ego.model.enums.JavaFields.APPLICATION;
import static bio.overture.ego.model.enums.JavaFields.GROUPAPPLICATIONS;
import static bio.overture.ego.model.enums.JavaFields.PERMISSIONS;
import static bio.overture.ego.model.enums.JavaFields.USER;
import static bio.overture.ego.model.enums.JavaFields.USERGROUPS;
import static javax.persistence.criteria.JoinType.LEFT;

@Setter
@Accessors(fluent = true, chain = true)
public class GroupSpecificationBuilder extends AbstractSpecificationBuilder<Group, UUID> {

  private boolean fetchApplications;
  private boolean fetchUserGroups;
  private boolean fetchGroupPermissions;

  @Override
  protected Root<Group> setupFetchStrategy(Root<Group> root) {
    if (fetchApplications) {
      val fromGroupApplications = root.fetch(GROUPAPPLICATIONS, LEFT);
      fromGroupApplications.fetch(APPLICATION, LEFT);
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
