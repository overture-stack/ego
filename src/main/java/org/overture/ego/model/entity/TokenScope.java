package org.overture.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.overture.ego.model.enums.PolicyMask;

import javax.persistence.*;
import java.io.Serializable;

@NoArgsConstructor
@Data
@Entity
@Table(name = "tokenscope")
class TokenScope implements Serializable {
  @Id
  @ManyToOne
  @JoinColumn(name="token_id")
  private ScopedAccessToken token;

  @Id
  @ManyToOne
  @JoinColumn(name = "policy_id")
  private Policy policy;

  @Id
  @Column(name="access_level")
  private String accessLevel;

  public TokenScope(ScopedAccessToken token, Policy policy, PolicyMask accessLevel) {
    setToken(token);
    setPolicy(policy);
    setAccessLevel(accessLevel);
  }

  void setAccessLevel(PolicyMask m) {
    this.accessLevel = m.toString();
  }

  @JsonIgnore
  PolicyMask getAccessLevel() {
    return PolicyMask.fromValue(accessLevel);
  }
}
