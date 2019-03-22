package bio.overture.ego.repository;

import bio.overture.ego.model.entity.Token;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TokenStoreRepository extends NamedRepository<Token, UUID> {

  Optional<Token> getTokenByNameIgnoreCase(String name);

  Token findOneByNameIgnoreCase(String token);

  Set<Token> findAllByIdIn(List<UUID> ids);

  @Modifying
  @Query(value = "update token set isrevoked=true where token.id in (select revokes.id from ((select token.id, string_agg(concat(cast (tokenscope.policy_id as text), '.', tokenscope.access_level), ',' order by tokenscope.policy_id, tokenscope.access_level) as policies from token left join tokenscope on token.id = tokenscope.token_id where token.owner=:userId group by token.id order by policies, token.issuedate desc) EXCEPT (select distinct on (policies) token.id, string_agg(concat(cast (tokenscope.policy_id as text), '.', tokenscope.access_level), ',' order by tokenscope.policy_id, tokenscope.access_level) as policies from token left join tokenscope on token.id = tokenscope.token_id where token.owner=:userId group by token.id order by policies, token.issuedate desc)) as revokes)",
          nativeQuery = true
  )
  int revokeRedundantTokens(@Param("userId") UUID userId);

//  Set<Token> findAllByOwnerAndScopes(List<UUID> ids);

  @Override
  default Optional<Token> findByName(String name) {
    return getTokenByNameIgnoreCase(name);
  }
}
