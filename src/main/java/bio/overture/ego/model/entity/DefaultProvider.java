package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import javax.persistence.*;
import lombok.*;

@Entity
@Table(name = Tables.DEFAULTPROVIDERTRIPWIRE)
@Data
@EqualsAndHashCode(of = "id")
@AllArgsConstructor
@NoArgsConstructor
public class DefaultProvider implements Identifiable<String> {

  @Id
  @Column(name = SqlFields.ID, nullable = false)
  private String id;
}
