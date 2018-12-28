package bio.overture.ego.service;

import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.PolicyRepository;
import bio.overture.ego.repository.queryspecification.PolicySpecification;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
public class PolicyService extends AbstractNamedService<Policy> {

  private final PolicyRepository policyRepository;

  @Autowired
  public PolicyService(@NonNull PolicyRepository policyRepository) {
    super(Policy.class, policyRepository);
    this.policyRepository = policyRepository;
  }

  // Create
  public Policy create(@NonNull Policy policy) {
    return policyRepository.save(policy);
  }

  // Read
  public Policy get(@NonNull String policyId) {
    return getById(policyId);
  }

  public Page<Policy> listPolicies(
      @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return policyRepository.findAll(PolicySpecification.filterBy(filters), pageable);
  }

  // Update
  public Policy update(@NonNull Policy updatedPolicy) {
    Policy policy = getById(updatedPolicy.getId().toString());
    policy.update(updatedPolicy);
    policyRepository.save(policy);
    return updatedPolicy;
  }
}
