package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.AccessLevel;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import java.io.Serializable;
import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@TypeDef(name = "ego_access_level_enum", typeClass = PostgreSQLEnumType.class)
@Table(name = "tokenscope")
class TokenScope implements Serializable {
  @Id
  @ManyToOne
  @JoinColumn(name = "token_id")
  private Token token;

  @Id
  @ManyToOne
  @JoinColumn(name = "policy_id")
  private Policy policy;

  @Column(name = "access_level", nullable = false)
  @Type(type = "ego_access_level_enum")
  @Enumerated(EnumType.STRING)
  private AccessLevel accessLevel;

  @Override
  public String toString() {
    return policy.getName() + "." + accessLevel.toString();
  }
}
