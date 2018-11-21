package bio.overture.ego.repository;

import bio.overture.ego.model.entity.Token;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.UUID;

public interface TokenStoreRepository
  extends PagingAndSortingRepository<Token, UUID>, JpaSpecificationExecutor {
  Token findOneByTokenIgnoreCase(String token);
}
