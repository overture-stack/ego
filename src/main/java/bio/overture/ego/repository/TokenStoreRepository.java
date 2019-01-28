package bio.overture.ego.repository;

import bio.overture.ego.model.entity.Token;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TokenStoreRepository extends NamedRepository<Token, UUID> {

  Optional<Token> getTokenByNameIgnoreCase(String name);

  Token findOneByNameIgnoreCase(String token);

  Set<Token> findAllByIdIn(List<UUID> ids);

  @Override
  default Optional<Token> findByName(String name) {
    return getTokenByNameIgnoreCase(name);
  }
}
