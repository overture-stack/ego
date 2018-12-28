package bio.overture.ego.repository;

import bio.overture.ego.model.entity.Permission;
import java.util.UUID;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface PermissionRepository<T extends Permission> extends BaseRepository<T, UUID> {}
