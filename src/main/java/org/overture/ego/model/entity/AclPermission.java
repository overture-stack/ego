package org.overture.ego.model.entity;

import lombok.Data;
import org.overture.ego.model.enums.AclMask;

import java.util.UUID;

@Data
public abstract class AclPermission {
  UUID id;
  AclEntity entity;
  AclOwnerEntity sid;
  AclMask mask;

  public void update(AclPermission other) {
    this.entity = other.entity;
    this.sid = other.sid;
    this.mask = other.mask;
    // Don't merge the ID - that is procedural.
  }
}
