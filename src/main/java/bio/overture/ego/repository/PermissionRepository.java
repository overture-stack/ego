package bio.overture.ego.repository;

import bio.overture.ego.model.entity.AbstractPermission;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.UUID;

@NoRepositoryBean
public interface PermissionRepository<T extends AbstractPermission> extends BaseRepository<T, UUID> {}
