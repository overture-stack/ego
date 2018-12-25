package bio.overture.ego.repository;

import bio.overture.ego.service.BaseService;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@NoRepositoryBean
public interface CommonRepository<T, ID> extends BaseRepository<T, ID> {

  Optional<T> findByName(String name);

}
