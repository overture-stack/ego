package bio.overture.ego.repository;

import bio.overture.ego.model.entity.Token;
import java.util.Optional;
import java.util.UUID;

public interface TokenStoreRepository extends BaseRepository<Token, UUID> {

  Optional<Token> getTokenByTokenIgnoreCase(String token);

  Token findOneByTokenIgnoreCase(String token);
}
