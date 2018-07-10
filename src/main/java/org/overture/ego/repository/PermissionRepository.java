package org.overture.ego.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

@NoRepositoryBean
public interface PermissionRepository<T> extends PagingAndSortingRepository<T, Integer>, JpaSpecificationExecutor {
}
