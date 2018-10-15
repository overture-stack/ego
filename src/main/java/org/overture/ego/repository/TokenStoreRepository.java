package org.overture.ego.repository;

import org.overture.ego.model.entity.ScopedAccessToken;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.UUID;

public interface TokenStoreRepository
  extends PagingAndSortingRepository<ScopedAccessToken, UUID>, JpaSpecificationExecutor {
  ScopedAccessToken findOneByTokenIgnoreCase(String token);
}
