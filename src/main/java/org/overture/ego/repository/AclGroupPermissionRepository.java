package org.overture.ego.repository;

import org.overture.ego.model.entity.AclGroupPermission;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface AclGroupPermissionRepository
    extends PagingAndSortingRepository<AclGroupPermission, Integer>, JpaSpecificationExecutor {
}
