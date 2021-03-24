package bio.overture.ego.repository.queryspecification.builder;

import static bio.overture.ego.model.enums.JavaFields.*;
import static javax.persistence.criteria.JoinType.LEFT;

import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.ProviderType;
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
public class UserSpecificationBuilder extends AbstractSpecificationBuilder<User, UUID> {

  private boolean fetchUserAndGroupPermissions;
  private boolean fetchUserGroups;
  private boolean fetchApplications;
  private boolean fetchRefreshToken;

  @Override
  protected Root<User> setupFetchStrategy(Root<User> root) {
    if (fetchApplications) {
      root.fetch(USERAPPLICATIONS, LEFT).fetch(APPLICATION, LEFT);
    }
    if (fetchUserGroups) {
      root.fetch(USERGROUPS, LEFT).fetch(GROUP, LEFT);
    }
    if (fetchUserAndGroupPermissions) {
      root.fetch(USERPERMISSIONS, LEFT).fetch(POLICY, LEFT);
      root.fetch(USERGROUPS, LEFT).fetch(GROUP, LEFT).fetch(PERMISSIONS, LEFT).fetch(POLICY, LEFT);
    }
    if (fetchRefreshToken) {
      root.fetch(User.Fields.refreshToken, LEFT);
    }
    return root;
  }

  public Specification<User> buildByProviderTypeAndSubjectId(
      @NonNull ProviderType providerType, @NonNull String providerSubjectId) {
    return (fromUser, query, builder) -> {
      val root = setupFetchStrategy(fromUser);
      return equalsProviderTypeAndSubjectIdPredicate(
          root, builder, providerType, providerSubjectId);
    };
  }

  private Predicate equalsProviderTypeAndSubjectIdPredicate(
      Root<User> root,
      CriteriaBuilder builder,
      ProviderType providerType,
      String providerSubjectId) {
    return builder.and(
        builder.equal(root.get(PROVIDER_SUBJECT_ID), providerSubjectId),
        builder.equal(root.get(PROVIDERTYPE), providerType));
  }

  public Specification<User> buildByEmail(@NonNull String email) {
    return (fromUser, query, builder) -> {
      val root = setupFetchStrategy(fromUser);
      return equalsEmailPredicate(root, builder, email);
    };
  }

  private Predicate equalsEmailPredicate(Root<User> root, CriteriaBuilder builder, String email) {
    return builder.and(builder.equal(root.get(EMAIL), email));
  }
}
