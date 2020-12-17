package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
