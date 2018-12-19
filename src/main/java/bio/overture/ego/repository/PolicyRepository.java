package bio.overture.ego.repository;

import bio.overture.ego.model.entity.Policy;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PolicyRepository
    extends PagingAndSortingRepository<Policy, UUID>, JpaSpecificationExecutor {

  Policy findOneByNameIgnoreCase(String name);

  Set<Policy> findAllByIdIn(List<UUID> policyIds);

}
