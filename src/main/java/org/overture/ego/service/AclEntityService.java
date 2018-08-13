package org.overture.ego.service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.overture.ego.model.entity.AclEntity;
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
public class AclEntityService extends BaseService<AclEntity, UUID> {

  /*
    Dependencies
   */
  @Autowired
  private AclEntityRepository aclEntityRepository;

  // Create
  public AclEntity create(@NonNull AclEntity aclEntity) {
    return aclEntityRepository.save(aclEntity);
  }


  // Read
  public AclEntity get(@NonNull String aclEntityId) {
    return getById(aclEntityRepository, fromString(aclEntityId));
  }

  public AclEntity getByName(@NonNull String aclEntityName) {
    return aclEntityRepository.findOneByNameIgnoreCase(aclEntityName);
  }

  public Page<AclEntity> listAclEntities(@NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return aclEntityRepository.findAll(AclEntitySpecification.filterBy(filters), pageable);
  }


  // Update
  public AclEntity update(@NonNull AclEntity updatedAclEntity) {
    AclEntity aclEntity = getById(aclEntityRepository, updatedAclEntity.getId());
    aclEntity.update(updatedAclEntity);
    aclEntityRepository.save(aclEntity);
    return updatedAclEntity;
  }

  // Delete
  public void delete(@NonNull String aclEntityId) {
    aclEntityRepository.deleteById(fromString(aclEntityId));
  }

}
