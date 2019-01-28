package bio.overture.ego.model.entity;

import static bio.overture.ego.model.enums.AccessLevel.EGO_ACCESS_LEVEL_ENUM;

import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@TypeDef(name = EGO_ACCESS_LEVEL_ENUM, typeClass = PostgreSQLEnumType.class)
@Table(name = Tables.TOKENSCOPE)
class TokenScope implements Serializable {

  // TODO; [rtisma] correct the Id stuff. There is a way to define a 2-tuple primary key. refer to
  // song Info entity (@EmbeddedId)
  // TODO: [rtisma] update sql to use FOREIGNKEY.
  @Id
  @JsonIgnore
  @NotNull
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = SqlFields.TOKENID_JOIN, nullable = false)
  private Token token;

  @Id
  @NotNull
  @ManyToOne
  @JoinColumn(name = SqlFields.POLICYID_JOIN, nullable = false)
  private Policy policy;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Type(type = EGO_ACCESS_LEVEL_ENUM)
  @Column(name = SqlFields.ACCESS_LEVEL, nullable = false)
  private AccessLevel accessLevel;

  @Override
  public String toString() {
    return policy.getName() + "." + accessLevel.toString();
  }
}
