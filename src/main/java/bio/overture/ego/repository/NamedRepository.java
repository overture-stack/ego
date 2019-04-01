package bio.overture.ego.repository;

import java.util.Optional;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface NamedRepository<T, ID> extends BaseRepository<T, ID> {

  /**
   * TODO: [rtisma] Deprecated because this should be implemented at the service layer using dynamic
   * fetching and not the entity graph. Leaving this for now. Once all services are implementing \
   * findByName, this can be removed from the NameRepository interface, and anything extending it
   */
  @Deprecated
  Optional<T> findByName(String name);
}
