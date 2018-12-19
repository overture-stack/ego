package bio.overture.ego.repository;

import bio.overture.ego.model.entity.Permission;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@NoRepositoryBean
public interface PermissionRepository<T extends Permission>
    extends PagingAndSortingRepository<T, UUID>, JpaSpecificationExecutor {

  Set<T> findAllByIdIn(List<UUID> permIds);

}
