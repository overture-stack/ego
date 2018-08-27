package org.overture.ego.service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.overture.ego.model.entity.Policy;
import org.overture.ego.model.search.SearchFilter;
import org.overture.ego.repository.AclEntityRepository;
import org.overture.ego.repository.queryspecification.AclEntitySpecification;
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
public class AclEntityService extends BaseService<Policy, UUID> {

  /*
    Dependencies
   */
  @Autowired
  private AclEntityRepository aclEntityRepository;

  // Create
  public Policy create(@NonNull Policy policy) {
    return aclEntityRepository.save(policy);
  }


  // Read
  public Policy get(@NonNull String aclEntityId) {
    return getById(aclEntityRepository, fromString(aclEntityId));
  }

  public Policy getByName(@NonNull String aclEntityName) {
    return aclEntityRepository.findOneByNameIgnoreCase(aclEntityName);
  }

  public Page<Policy> listAclEntities(@NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return aclEntityRepository.findAll(AclEntitySpecification.filterBy(filters), pageable);
  }


  // Update
  public Policy update(@NonNull Policy updatedPolicy) {
    Policy policy = getById(aclEntityRepository, updatedPolicy.getId());
    policy.update(updatedPolicy);
    aclEntityRepository.save(policy);
    return updatedPolicy;
  }

  // Delete
  public void delete(@NonNull String aclEntityId) {
    aclEntityRepository.deleteById(fromString(aclEntityId));
  }

}
