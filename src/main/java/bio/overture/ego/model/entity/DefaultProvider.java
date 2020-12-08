package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import javax.persistence.*;
import lombok.*;

@Entity
@Table(name = Tables.DEFAULTPROVIDERTRIPWIRE)
@Data
@Builder
@EqualsAndHashCode(of = "id")
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({JavaFields.ID})
public class DefaultProvider implements Identifiable<String> {

  @Id
  @Column(name = SqlFields.ID, nullable = false)
  private String id;
}
