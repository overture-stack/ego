package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.AccessLevel;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@TypeDef(name = "ego_access_level_enum", typeClass = PostgreSQLEnumType.class)
@Table(name = "tokenscope")
class TokenScope implements Serializable {

  @Id
  @NotNull
  @ManyToOne
  @JoinColumn(name = "token_id", nullable = false)
  private Token token;

  @Id
  @NotNull
  @ManyToOne
  @JoinColumn(name = "policy_id", nullable = false)
  private Policy policy;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Type(type = "ego_access_level_enum")
  @Column(name = "access_level", nullable = false)
  private AccessLevel accessLevel;

  @Override
  public String toString() {
    return policy.getName() + "." + accessLevel.toString();
  }
}
