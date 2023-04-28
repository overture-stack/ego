package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.ProviderType;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

@Entity
@Table(name = Tables.DEFAULTPROVIDERTRIPWIRE)
@Data
@EqualsAndHashCode(of = "id")
@AllArgsConstructor
@NoArgsConstructor
public class DefaultProvider implements Identifiable<ProviderType> {

  @Id
  @Column(name = SqlFields.ID, nullable = false)
  @Type(PostgreSQLEnumType.class)
  @Enumerated(EnumType.STRING)
  private ProviderType id;
}
