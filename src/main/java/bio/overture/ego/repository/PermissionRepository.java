package bio.overture.ego.repository;

import bio.overture.ego.model.entity.AbstractPermission;
import bio.overture.ego.model.enums.AccessLevel;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface PermissionRepository<T extends AbstractPermission>
    extends BaseRepository<T, UUID> {

  Set<T> findAllByPolicy_Id(UUID id);

  Optional<T> findByPolicy_IdAndOwner_id(UUID policyId, UUID ownerId);

  Set<T> findAllByPolicy_IdAndAccessLevel(UUID policyId, AccessLevel accessLevel);
}
