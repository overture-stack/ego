package bio.overture.ego.repository.queryspecification.builder;

import static bio.overture.ego.model.enums.JavaFields.OWNER;
import static bio.overture.ego.model.enums.JavaFields.SCOPES;
import static javax.persistence.criteria.JoinType.LEFT;

import bio.overture.ego.model.entity.Token;
import java.util.UUID;
import javax.persistence.criteria.Root;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Accessors(fluent = true, chain = true)
public class TokenSpecificationBuilder extends AbstractSpecificationBuilder<Token, UUID> {

  private boolean fetchOwner;
  private boolean fetchTokenScopes;

  @Override
  protected Root<Token> setupFetchStrategy(Root<Token> root) {
    if (fetchOwner) {
      root.fetch(OWNER);
    }
    if (fetchTokenScopes) {
      root.fetch(SCOPES, LEFT);
    }
    return root;
  }
}
