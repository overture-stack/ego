package bio.overture.ego.repository;

import bio.overture.ego.model.entity.AbstractPermission;
import bio.overture.ego.model.entity.NameableEntity;
import java.util.UUID;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface NameablePermissionRepository<
        O extends NameableEntity<UUID>, T extends AbstractPermission<O>>
    extends PermissionRepository<O, T> {}
