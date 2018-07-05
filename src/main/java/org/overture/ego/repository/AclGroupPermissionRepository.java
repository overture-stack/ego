package org.overture.ego.repository;

import org.overture.ego.model.entity.Application;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface AclGroupPermissionRepository
    extends PagingAndSortingRepository<Application, Integer>, JpaSpecificationExecutor {
}
