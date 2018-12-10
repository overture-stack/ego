package bio.overture.ego.repository;

import bio.overture.ego.model.entity.Permission;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

@NoRepositoryBean
public interface PermissionRepository<T extends Permission>
    extends PagingAndSortingRepository<T, UUID>, JpaSpecificationExecutor {}
