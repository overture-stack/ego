package bio.overture.ego.repository;

import bio.overture.ego.model.entity.AbstractPermission;
import java.util.UUID;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface PermissionRepository<T extends AbstractPermission>
    extends BaseRepository<T, UUID> {}
