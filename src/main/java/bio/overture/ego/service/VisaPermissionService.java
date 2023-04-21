package bio.overture.ego.service;

import static java.lang.String.format;

import bio.overture.ego.event.token.ApiKeyEventsPublisher;
import bio.overture.ego.model.entity.VisaPermission;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.repository.VisaPermissionRepository;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class VisaPermissionService extends AbstractNamedService<VisaPermission, UUID> {

  /** Dependencies */
  @Autowired private VisaService visaService;

  @Autowired private VisaPermissionRepository visaPermissionRepository;
  private final ApiKeyEventsPublisher apiKeyEventsPublisher;

  @Autowired
  public VisaPermissionService(
      @NonNull VisaPermissionRepository visaPermissionRepository,
      @NonNull VisaService visaService,
      @NonNull ApiKeyEventsPublisher apiKeyEventsPublisher) {
    super(VisaPermission.class, visaPermissionRepository);
    this.visaPermissionRepository = visaPermissionRepository;
    this.visaService = visaService;
    this.apiKeyEventsPublisher = apiKeyEventsPublisher;
  }

  public List<VisaPermission> getPermissionsByVisaId(@NonNull UUID visaId) {
    val result = (List<VisaPermission>) visaPermissionRepository.findByVisaId(visaId);
    System.out.println("Result :::::::::::::::::::::::" + result);
    if (result.isEmpty()) {
      throw new NotFoundException(format("No VisaPermissions exists with visaId '%s'", visaId));
    }
    return result;
  }

  @Override
  public VisaPermission getById(@NonNull UUID uuid) {
    return super.getById(uuid);
  }

  @Override
  public VisaPermission getWithRelationships(UUID uuid) {
    return null;
  }
}
