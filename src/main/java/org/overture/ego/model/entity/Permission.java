package org.overture.ego.model.entity;

import lombok.Data;
import org.overture.ego.model.dto.Scope;
import org.overture.ego.model.enums.AccessLevel;

import java.util.UUID;
@Data
public abstract class Permission {
  UUID id;
  Policy policy;
  PolicyOwner owner;
  AccessLevel accessLevel;

  public void update(Permission other) {
    this.policy = other.getPolicy();
    this.owner = other.getOwner();
    this.accessLevel = other.getAccessLevel();
    // Don't merge the ID - that is procedural.
  }

  public Scope toScope() {
    return new Scope(getPolicy(), getAccessLevel());
  }
}
