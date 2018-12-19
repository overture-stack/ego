package bio.overture.ego.repository;

import bio.overture.ego.model.entity.Token;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface TokenStoreRepository
    extends PagingAndSortingRepository<Token, UUID>, JpaSpecificationExecutor {
  Token findOneByTokenIgnoreCase(String token);
}
