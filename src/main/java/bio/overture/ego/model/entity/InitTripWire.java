package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = Tables.INITTRIPWIRE)
public class InitTripWire {

  @Id
  @Column(name = SqlFields.INITIALIZED)
  // 0 for false, >0 for true
  private int initialized;
}
