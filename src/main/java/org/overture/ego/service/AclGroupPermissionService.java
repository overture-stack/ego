package org.overture.ego.service;

import org.overture.ego.repository.AclGroupPermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class AclGroupPermissionService extends PermissionService {

  /*
    Dependencies
   */
  @Autowired
  private AclGroupPermissionRepository repository;
}
