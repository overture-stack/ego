package org.overture.ego.repository;

import org.overture.ego.model.entity.AclEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface AclEntityRepository
    extends PagingAndSortingRepository<AclEntity, Integer>, JpaSpecificationExecutor {
}
