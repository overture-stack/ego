package bio.overture.ego.service;

import static bio.overture.ego.model.exceptions.UniqueViolationException.checkUnique;
import static bio.overture.ego.utils.FieldUtils.onUpdateDetected;
import static java.util.UUID.fromString;
import static org.mapstruct.factory.Mappers.getMapper;

import bio.overture.ego.model.dto.PolicyRequest;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.PolicyRepository;
import bio.overture.ego.repository.queryspecification.PolicySpecification;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.TargetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class PolicyService extends AbstractNamedService<Policy, UUID> {

  private static final PolicyConverter POLICY_CONVERTER = getMapper(PolicyConverter.class);

  private final PolicyRepository policyRepository;

  @Autowired
  public PolicyService(@NonNull PolicyRepository policyRepository) {
    super(Policy.class, policyRepository);
    this.policyRepository = policyRepository;
  }

  public Policy create(@NonNull PolicyRequest createRequest) {
    checkNameUnique(createRequest.getName());
    val policy = POLICY_CONVERTER.convertToPolicy(createRequest);
    return getRepository().save(policy);
  }

  public Policy get(@NonNull String policyId) {
    return getById(fromString(policyId));
  }

  public Page<Policy> listPolicies(
      @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return policyRepository.findAll(PolicySpecification.filterBy(filters), pageable);
  }

  public Policy partialUpdate(@NonNull String id, @NonNull PolicyRequest updateRequest) {
    val policy = getById(fromString(id));
    validateUpdateRequest(policy, updateRequest);
    POLICY_CONVERTER.updatePolicy(updateRequest, policy);
    return getRepository().save(policy);
  }

  public void delete(String id) {
    delete(fromString(id));
  }

  private void validateUpdateRequest(Policy originalPolicy, PolicyRequest updateRequest) {
    onUpdateDetected(
        originalPolicy.getName(),
        updateRequest.getName(),
        () -> checkNameUnique(updateRequest.getName()));
  }

  private void checkNameUnique(String name) {
    checkUnique(
        !policyRepository.existsByNameIgnoreCase(name), "A policy with same name already exists");
  }

  @Mapper(
      nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
      unmappedTargetPolicy = ReportingPolicy.WARN)
  public abstract static class PolicyConverter {

    public abstract Policy convertToPolicy(PolicyRequest request);

    public abstract void updatePolicy(Policy updatingPolicy, @MappingTarget Policy policyToUpdate);

    public Policy copy(Policy policyToCopy) {
      val newPolicy = initPolicyEntity(Policy.class);
      updatePolicy(policyToCopy, newPolicy);
      return newPolicy;
    }

    public abstract void updatePolicy(PolicyRequest request, @MappingTarget Policy policyToUpdate);

    protected Policy initPolicyEntity(@TargetType Class<Policy> policyClass) {
      return Policy.builder().build();
    }
  }
}
