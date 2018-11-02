package org.overture.ego.service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.overture.ego.model.entity.Policy;
import org.overture.ego.model.search.SearchFilter;
import org.overture.ego.repository.PolicyRepository;
import org.overture.ego.repository.queryspecification.PolicySpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static java.util.UUID.fromString;

@Slf4j
@Service
@Transactional
public class PolicyService extends BaseService<Policy, UUID> {

  /*
    Dependencies
   */
  @Autowired
  private PolicyRepository policyRepository;

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

  public Page<Policy> listPolicies(@NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
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

}
