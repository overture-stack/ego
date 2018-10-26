package org.overture.ego.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.overture.ego.model.enums.PolicyMask;

import javax.persistence.*;
import java.io.Serializable;

@AllArgsConstructor
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
  @JoinColumn(name = "scope_id")
  private Policy policy;

  @Id
  @Column(name="accessLevel")
  private PolicyMask accessLevel;
}
