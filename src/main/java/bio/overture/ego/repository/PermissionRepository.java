package bio.overture.ego.repository;

import bio.overture.ego.model.entity.AbstractPermission;
import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.enums.AccessLevel;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface PermissionRepository<O extends Identifiable<UUID>, T extends AbstractPermission<O>>
    extends BaseRepository<T, UUID> {

  Set<T> findAllByPolicy_Id(UUID id);

  Set<T> findAllByOwner_Id(UUID id);

  Optional<T> findByPolicy_IdAndOwner_id(UUID policyId, UUID ownerId);

  Set<T> findAllByPolicy_IdAndAccessLevel(UUID policyId, AccessLevel accessLevel);
}
