package bio.overture.ego.repository.queryspecification.builder;

import static bio.overture.ego.model.enums.JavaFields.CLIENTID;
import static bio.overture.ego.model.enums.JavaFields.GROUP;
import static bio.overture.ego.model.enums.JavaFields.GROUPAPPLICATIONS;
import static bio.overture.ego.model.enums.JavaFields.USERS;
import static javax.persistence.criteria.JoinType.LEFT;

import bio.overture.ego.model.entity.Application;
import java.util.UUID;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
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
      val fromGroupApplications = root.fetch(GROUPAPPLICATIONS, LEFT);
      fromGroupApplications.fetch(GROUP, LEFT);
    }
    if (fetchUsers) {
      root.fetch(USERS, LEFT);
    }
    return root;
  }
}
