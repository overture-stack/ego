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

  public Specification<User> buildByProviderTypeAndId(
      @NonNull ProviderType providerType, @NonNull String providerId) {
    return (fromUser, query, builder) -> {
      val root = setupFetchStrategy(fromUser);
      return equalsProviderTypeAndIdPredicate(root, builder, providerType, providerId);
    };
  }

  private Predicate equalsProviderTypeAndIdPredicate(
      Root<User> root, CriteriaBuilder builder, ProviderType providerType, String providerId) {
    return builder.and(
        builder.equal(root.get(PROVIDERID), providerId),
        builder.equal(root.get(PROVIDERTYPE), providerType));
  }
}
