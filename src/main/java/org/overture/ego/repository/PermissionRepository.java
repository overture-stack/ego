package org.overture.ego.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

@NoRepositoryBean
public interface PermissionRepository<T> extends PagingAndSortingRepository<T, Integer>, JpaSpecificationExecutor {
  Optional<T> findOneBySid(int sid);
}
