package bio.overture.ego.repository;

import bio.overture.ego.model.entity.Policy;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface PolicyRepository
    extends PagingAndSortingRepository<Policy, UUID>, JpaSpecificationExecutor {

  Policy findOneByNameIgnoreCase(String name);
}
