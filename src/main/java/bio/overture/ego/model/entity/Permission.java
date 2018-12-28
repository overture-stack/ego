package bio.overture.ego.model.entity;

import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.enums.AccessLevel;
import java.util.UUID;
import lombok.Data;

@Data
public abstract class Permission implements Identifiable<UUID> {
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
