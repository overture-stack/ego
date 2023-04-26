package bio.overture.ego.repository.queryspecification.builder;

import static bio.overture.ego.model.enums.JavaFields.*;
import static jakarta.persistence.criteria.JoinType.LEFT;

import bio.overture.ego.model.entity.Application;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.UUID;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.val;
import org.springframework.data.jpa.domain.Specification;

@Setter
@Accessors(fluent = true, chain = true)
public class ApplicationSpecificationBuilder
    extends AbstractSpecificationBuilder<Application, UUID> {

  private boolean fetchGroups;
  private boolean fetchUsers;
  private boolean fetchApplicationAndGroupPermissions;

  public Specification<Application> buildByClientIdIgnoreCase(@NonNull String clientId) {
    return (fromApplication, query, builder) -> {
      val root = setupFetchStrategy(fromApplication);
      return equalsNameIgnoreCasePredicate(root, builder, clientId);
    };
  }

  private Predicate equalsNameIgnoreCasePredicate(
      Root<Application> root, CriteriaBuilder builder, String clientId) {
    return builder.equal(
        builder.upper(root.get(CLIENTID)), builder.upper(builder.literal(clientId)));
  }

  @Override
  protected Root<Application> setupFetchStrategy(Root<Application> root) {
    if (fetchGroups) {
      root.fetch(GROUPAPPLICATIONS, LEFT).fetch(GROUP, LEFT);
    }
    if (fetchUsers) {
      root.fetch(USERAPPLICATIONS, LEFT).fetch(USER, LEFT);
    }

    if (fetchApplicationAndGroupPermissions) {
      root.fetch(Application.Fields.applicationPermissions, LEFT).fetch(POLICY, LEFT);
      root.fetch(GROUPAPPLICATIONS, LEFT)
          .fetch(GROUP, LEFT)
          .fetch(PERMISSIONS, LEFT)
          .fetch(POLICY, LEFT);
    }
    return root;
  }
}
