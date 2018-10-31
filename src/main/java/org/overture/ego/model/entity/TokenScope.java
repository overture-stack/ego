package org.overture.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.overture.ego.model.enums.PolicyMask;

import javax.persistence.*;
import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@TypeDef(
  name = "ego_acl_enum",
  typeClass = PostgreSQLEnumType.class
)
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


  @Column(name="access_level", nullable = false)
  @Type(type = "ego_acl_enum")
  @Enumerated(EnumType.STRING)
  private PolicyMask accessLevel;
}
