package bio.overture.ego.service;

import static bio.overture.ego.model.enums.JavaFields.ID;
import static bio.overture.ego.model.enums.JavaFields.PERMISSIONS;
import static bio.overture.ego.model.enums.JavaFields.USERPERMISSIONS;
import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.model.exceptions.RequestValidationException.checkRequestValid;
import static bio.overture.ego.model.exceptions.UniqueViolationException.checkUnique;
import static bio.overture.ego.utils.FieldUtils.onUpdateDetected;
import static jakarta.persistence.criteria.JoinType.LEFT;
import static org.mapstruct.factory.Mappers.getMapper;

import bio.overture.ego.event.token.ApiKeyEventsPublisher;
import bio.overture.ego.model.dto.PolicyRequest;
import bio.overture.ego.model.entity.ApiKeyScope;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.PolicyRepository;
import bio.overture.ego.repository.queryspecification.PolicySpecification;
import bio.overture.ego.utils.Collectors;
import java.util.List;
import java.util.Optional;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class PolicyService extends AbstractNamedService<Policy, UUID> {

  /** Constants */
  private static final PolicyConverter POLICY_CONVERTER = getMapper(PolicyConverter.class);

  /** Dependencies */
  private final PolicyRepository policyRepository;

  private final ApiKeyEventsPublisher apiKeyEventsPublisher;

  @Autowired
  public PolicyService(
      @NonNull PolicyRepository policyRepository,
      @NonNull ApiKeyEventsPublisher apiKeyEventsPublisher) {
    super(Policy.class, policyRepository);
    this.policyRepository = policyRepository;
    this.apiKeyEventsPublisher = apiKeyEventsPublisher;
  }

  public Policy create(@NonNull PolicyRequest createRequest) {
    validateCreateRequest(createRequest);
    val policy = POLICY_CONVERTER.convertToPolicy(createRequest);
    return getRepository().save(policy);
  }

  public Policy getPolicyByNameCreateIfNecessary(String name) {
    val policy = policyRepository.getPolicyByNameIgnoreCase(name);
    if (policy.isPresent()) {
      return policy.get();
    }
    return create(new PolicyRequest(name));
  }

  @Override
  public Policy getWithRelationships(@NonNull UUID id) {
    val result = (Optional<Policy>) getRepository().findOne(fetchSpecification(id, true, true));
    checkNotFound(result.isPresent(), "The policyId '%s' does not exist", id);
    return result.get();
  }

  public void delete(@NonNull UUID id) {
    checkExistence(id);
    val policy = this.getById(id);

    // For semantic/readability reasons, revoke api keys AFTER policy is deleted.
    val apiKeysToRevoke =
        policy.getApiKeyScopes().stream()
            .map(ApiKeyScope::getToken)
            .collect(Collectors.toImmutableSet());
    super.delete(id);
    apiKeyEventsPublisher.requestApiKeyCleanup(apiKeysToRevoke);
  }

  public Page<Policy> listPolicies(
      @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return policyRepository.findAll(PolicySpecification.filterBy(filters), pageable);
  }

  public Policy partialUpdate(@NonNull UUID id, @NonNull PolicyRequest updateRequest) {
    val policy = getById(id);
    validateUpdateRequest(policy, updateRequest);
    POLICY_CONVERTER.updatePolicy(updateRequest, policy);
    return getRepository().save(policy);
  }

  private void validateCreateRequest(PolicyRequest createRequest) {
    checkRequestValid(createRequest);
    checkNameUnique(createRequest.getName());
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

  private static Specification<Policy> fetchSpecification(
      UUID id, boolean fetchGroupPermissions, boolean fetchUserPermissions) {
    return (fromPolicy, query, builder) -> {
      if (fetchGroupPermissions) {
        fromPolicy.fetch(PERMISSIONS, LEFT);
      }
      if (fetchUserPermissions) {
        fromPolicy.fetch(USERPERMISSIONS, LEFT);
      }
      return builder.equal(fromPolicy.get(ID), id);
    };
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
