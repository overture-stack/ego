package bio.overture.ego.repository;

import bio.overture.ego.model.entity.Visa;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VisaRepository extends NamedRepository<Visa, UUID> {
  @Override
  @Deprecated
  default Optional<Visa> findByName(String name) {
    return null;
  }

  List<Visa> findAll();
}
