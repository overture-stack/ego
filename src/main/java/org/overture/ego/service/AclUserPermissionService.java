package org.overture.ego.service;

import org.overture.ego.repository.AclUserPermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;


public class AclUserPermissionService extends PermissionService {

  /*
    Dependencies
   */
  @Autowired
  private AclUserPermissionRepository repository;
}
