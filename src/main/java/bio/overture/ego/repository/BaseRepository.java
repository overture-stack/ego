package bio.overture.ego.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

@NoRepositoryBean
public interface BaseRepository<T, ID>
    extends PagingAndSortingRepository<T, ID>, JpaSpecificationExecutor {

  Set<T> findAllByIdIn(List<ID> ids);
}
