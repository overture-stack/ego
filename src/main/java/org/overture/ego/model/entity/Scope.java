package org.overture.ego.model.entity;

import lombok.Data;
import org.overture.ego.model.enums.PolicyMask;

import java.util.UUID;

@Data
public abstract class Scope {
  UUID id;
  Policy entity;
  PolicyOwner sid;
  PolicyMask mask;

  public void update(Scope other) {
    this.entity = other.entity;
    this.sid = other.sid;
    this.mask = other.mask;
    // Don't merge the ID - that is procedural.
  }
}
