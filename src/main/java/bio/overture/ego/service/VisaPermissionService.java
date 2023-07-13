package bio.overture.ego.service;

import static java.lang.String.format;
import static org.mapstruct.factory.Mappers.getMapper;

import bio.overture.ego.model.dto.VisaPermissionRequest;
import bio.overture.ego.model.entity.Visa;
import bio.overture.ego.model.entity.VisaPermission;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.repository.VisaPermissionRepository;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class VisaPermissionService extends AbstractNamedService<VisaPermission, UUID> {
  /** Dependencies */
  @Autowired private VisaService visaService;

  @Autowired private PolicyService policyService;
  @Autowired private VisaPermissionRepository visaPermissionRepository;
  private static final VisaPermissionConverter VISA_PERMISSION_CONVERTER =
      getMapper(VisaPermissionConverter.class);

  @Autowired
  public VisaPermissionService(
      @NonNull VisaPermissionRepository visaPermissionRepository,
      @NonNull VisaService visaService) {
    super(VisaPermission.class, visaPermissionRepository);
    this.visaPermissionRepository = visaPermissionRepository;
    this.visaService = visaService;
  }

  public List<VisaPermission> getPermissionsByPolicyId(@NonNull UUID policyId) {
    val result = (List<VisaPermission>) visaPermissionRepository.findByPolicy_Id(policyId);
    if (result.isEmpty()) {
      throw new NotFoundException(format("No VisaPermissions exists with policyId '%s'", policyId));
    }
    return result;
  }

  public VisaPermission createOrUpdatePermissions(
      @NonNull VisaPermissionRequest visaPermissionRequest) {
    VisaPermission visaPermission = null;
    List<VisaPermission> visaPermissionEntities =
        visaPermissionRepository.findByPolicyIdAndVisaId(
            visaPermissionRequest.getPolicyId(), visaPermissionRequest.getVisaId());
    if (visaPermissionEntities.isEmpty()) {
      visaPermission = new VisaPermission();
      visaPermission.setVisa(visaService.getById(visaPermissionRequest.getVisaId()));
      visaPermission.setPolicy(policyService.getById(visaPermissionRequest.getPolicyId()));
      visaPermission.setAccessLevel(visaPermissionRequest.getAccessLevel());
      return visaPermissionRepository.save(visaPermission);
    } else {
      VISA_PERMISSION_CONVERTER.updateVisaPermission(
          visaPermissionRequest, visaPermissionEntities.get(0));
      return visaPermissionRepository.save(visaPermissionEntities.get(0));
    }
  }

  public void removePermission(@NonNull UUID policyId, @NotNull UUID visaId) {

    val visa = visaService.getById(visaId);

    if(visa == null) {
      throw new NotFoundException(format("No Visa exists with id '%s'", visaId));
    }

    val visaPermissionEntities = visaPermissionRepository.findByPolicyIdAndVisaId(policyId, visa.getId());

    if(visaPermissionEntities.isEmpty()){
      throw new NotFoundException(format("No VisaPermissions exists with policyId '%s'", policyId));
    }

    visaPermissionRepository.deleteById(visaPermissionEntities.get(0).getId());

  }

  // Fetches visa permissions for given visa request
  public List<VisaPermission> getPermissionsForVisa(@NonNull List<Visa> visas) {
    List<VisaPermission> visaPermissions = new ArrayList<>();
    visas.stream()
        .distinct()
        .forEach(
            visa -> {
              visaPermissions.addAll(visaPermissionRepository.findByVisa_Id(visa.getId()));
            });
    return visaPermissions;
  }

  @Override
  public VisaPermission getById(@NonNull UUID uuid) {
    return super.getById(uuid);
  }

  @Override
  public VisaPermission getWithRelationships(UUID uuid) {
    return null;
  }

  @Mapper(
      nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
      unmappedTargetPolicy = ReportingPolicy.WARN)
  public abstract static class VisaPermissionConverter {
    public abstract void updateVisaPermission(
        VisaPermissionRequest visaPermissionRequest, @MappingTarget VisaPermission visaPermission);
  }
}
