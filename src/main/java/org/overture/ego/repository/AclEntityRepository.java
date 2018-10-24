package org.overture.ego.repository;

import org.overture.ego.model.entity.Policy;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.UUID;

public interface AclEntityRepository
  extends PagingAndSortingRepository<Policy, UUID>, JpaSpecificationExecutor {

  Policy findOneByNameIgnoreCase(String name);
}
