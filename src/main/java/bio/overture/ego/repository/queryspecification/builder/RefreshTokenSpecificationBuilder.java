package bio.overture.ego.repository.queryspecification.builder;

import static jakarta.persistence.criteria.JoinType.LEFT;

import bio.overture.ego.model.entity.RefreshToken;
import jakarta.persistence.criteria.Root;
import java.util.UUID;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(fluent = true, chain = true)
public class RefreshTokenSpecificationBuilder
    extends AbstractSpecificationBuilder<RefreshToken, UUID> {

  private boolean fetchUser;

  @Override
  protected Root<RefreshToken> setupFetchStrategy(Root<RefreshToken> root) {
    if (fetchUser) {
      root.fetch(RefreshToken.Fields.user, LEFT);
    }
    return root;
  }
}
