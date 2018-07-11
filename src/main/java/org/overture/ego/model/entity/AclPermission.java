package org.overture.ego.model.entity;

import lombok.Data;
import org.overture.ego.model.enums.AclMask;

@Data
public abstract class AclPermission {
  int id;
  AclEntity entityId;
  AclOwnerEntity sid;
  AclMask mask;

  public void update(AclPermission other) {
    this.entityId = other.entityId;
    this.sid = other.sid;
    this.mask = other.mask;
    // Don't merge the ID - that is procedural.
  }
}
