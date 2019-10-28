package bio.overture.ego.repository.queryspecification.builder;

import static bio.overture.ego.model.enums.JavaFields.OWNER;
import static bio.overture.ego.model.enums.JavaFields.SCOPES;
import static javax.persistence.criteria.JoinType.LEFT;

import bio.overture.ego.model.entity.ApiKey;
import java.util.UUID;
import javax.persistence.criteria.Root;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(fluent = true, chain = true)
public class TokenSpecificationBuilder extends AbstractSpecificationBuilder<ApiKey, UUID> {

  private boolean fetchOwner;
  private boolean fetchTokenScopes;

  @Override
  protected Root<ApiKey> setupFetchStrategy(Root<ApiKey> root) {
    if (fetchOwner) {
      root.fetch(OWNER);
    }
    if (fetchTokenScopes) {
      root.fetch(SCOPES, LEFT);
    }
    return root;
  }
}
