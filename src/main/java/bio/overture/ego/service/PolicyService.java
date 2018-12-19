package bio.overture.ego.service;

import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.PolicyRepository;
import bio.overture.ego.repository.queryspecification.PolicySpecification;
import bio.overture.ego.utils.Joiners;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static bio.overture.ego.model.exceptions.NotFoundException.checkExists;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Converters.convertToUUIDList;
import static java.util.UUID.fromString;

@Slf4j
@Service
@Transactional
public class PolicyService extends BaseService<Policy, UUID> {
  @Autowired private PolicyRepository policyRepository;
  // Create
  public Policy create(@NonNull Policy policy) {
    return policyRepository.save(policy);
  }

  // Read
  public Policy get(@NonNull String policyId) {
    return getById(policyRepository, fromString(policyId));
  }

  public Policy getByName(@NonNull String policyName) {
    return policyRepository.findOneByNameIgnoreCase(policyName);
  }

  public Page<Policy> listPolicies(
      @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return policyRepository.findAll(PolicySpecification.filterBy(filters), pageable);
  }

  // Update
  public Policy update(@NonNull Policy updatedPolicy) {
    Policy policy = getById(policyRepository, updatedPolicy.getId());
    policy.update(updatedPolicy);
    policyRepository.save(policy);
    return updatedPolicy;
  }

  // Delete
  public void delete(@NonNull String PolicyId) {
    policyRepository.deleteById(fromString(PolicyId));
  }

  public Set<Policy> getMany(@NonNull Collection<String> policyIds) {
    val policies = policyRepository.findAllByIdIn(convertToUUIDList(policyIds));
    val nonExistingApps = policies.stream()
        .map(Policy::getId)
        .filter(x -> !policyRepository.existsById(x))
        .collect(toImmutableSet());
    checkExists(nonExistingApps.isEmpty(),
        "The following policy ids were not found: %s",
        Joiners.COMMA.join(nonExistingApps));
    return policies;
  }

}
