package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

// TODO: rename TOKENSCOPE to API_KEY_SCOPE [anncatton]
@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Table(name = Tables.TOKENSCOPE)
public class ApiKeyScope implements Serializable {

  // TODO; [rtisma] correct the Id stuff. There is a way to define a 2-tuple primary key. refer to
  // song Info entity (@EmbeddedId)
  // TODO: [rtisma] update sql to use FOREIGNKEY.
  @Id
  @JsonIgnore
  @NotNull
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = SqlFields.TOKENID_JOIN, nullable = false)
  private ApiKey token;

  @Id
  @NotNull
  @ManyToOne
  @JoinColumn(name = SqlFields.POLICYID_JOIN, nullable = false)
  private Policy policy;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Type(PostgreSQLEnumType.class)
  @Column(name = SqlFields.ACCESS_LEVEL, nullable = false)
  private AccessLevel accessLevel;

  @Override
  public String toString() {
    return policy.getName() + "." + accessLevel.toString();
  }
}
