package bio.overture.ego.repository.queryspecification.builder;

import bio.overture.ego.model.entity.RefreshToken;
import bio.overture.ego.model.entity.User;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.criteria.Root;
import java.util.UUID;

import static bio.overture.ego.model.enums.JavaFields.*;
import static javax.persistence.criteria.JoinType.LEFT;

@Setter
@Accessors(fluent = true, chain = true)
public class RefreshTokenSpecificationBuilder extends AbstractSpecificationBuilder<RefreshToken, UUID> {

  private boolean fetchUser;


  @Override
  protected Root<RefreshToken> setupFetchStrategy(Root<RefreshToken> root) {
    if (fetchUser) {
      root.fetch(RefreshToken.Fields.user, LEFT);
    }
    return root;
  }
}
