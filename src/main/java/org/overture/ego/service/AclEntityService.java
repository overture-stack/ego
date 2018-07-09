package org.overture.ego.service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.overture.ego.model.entity.AclEntity;
import org.overture.ego.repository.AclEntityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class AclEntityService extends BaseService<AclEntity> {

  /*
    Dependencies
   */
  @Autowired
  private AclEntityRepository aclEntityRepository;

  public AclEntity create(@NonNull AclEntity aclEntity) {
    return aclEntityRepository.save(aclEntity);
  }

  public AclEntity get(@NonNull String aclEntityId) {
    return getById(aclEntityRepository, Integer.parseInt(aclEntityId));
  }

  // Update
  public AclEntity update(@NonNull AclEntity updatedAclEntity) {
    AclEntity aclEntity = getById(aclEntityRepository, updatedAclEntity.getId());
    aclEntity.update(updatedAclEntity);
    aclEntityRepository.save(aclEntity);
    return updatedAclEntity;
  }

}
