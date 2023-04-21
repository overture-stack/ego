package bio.overture.ego.repository;

import bio.overture.ego.model.entity.VisaPermission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisaPermissionRepository extends NamedRepository<VisaPermission, UUID> {
  @Override
  @Deprecated
  default Optional<VisaPermission> findByName(String name) {
    return null;
  }

  List<VisaPermission> findAll();

  List<VisaPermission> findByVisaId(UUID visaId);
}
