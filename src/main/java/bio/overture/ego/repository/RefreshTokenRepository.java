package bio.overture.ego.repository;

import bio.overture.ego.model.entity.RefreshToken;
import bio.overture.ego.model.entity.User;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends BaseRepository<RefreshToken, UUID> {

  Optional<RefreshToken> getByUser(User user);
}
