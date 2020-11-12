package bio.overture.ego.repository.queryspecification.builder;

import static bio.overture.ego.model.enums.JavaFields.*;
import static javax.persistence.criteria.JoinType.LEFT;

import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.IdProviderType;
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

  public Specification<User> buildByProviderNameAndId(
      @NonNull IdProviderType provider, @NonNull String providerId) {
    return (fromUser, query, builder) -> {
      val root = setupFetchStrategy(fromUser);
      return equalsProviderNameAndIdPredicate(root, builder, provider, providerId);
    };
  }

  private Predicate equalsProviderNameAndIdPredicate(
      Root<User> root, CriteriaBuilder builder, IdProviderType provider, String providerId) {
    return builder.and(
        builder.equal(root.get(PROVIDERID), providerId),
        builder.equal(root.get(IDENTITYPROVIDER), provider));
  }
}
