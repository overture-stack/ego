package bio.overture.ego.repository;

import java.util.Optional;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface NamedRepository<T, ID> extends BaseRepository<T, ID> {

  Optional<T> findByName(String name);
}
