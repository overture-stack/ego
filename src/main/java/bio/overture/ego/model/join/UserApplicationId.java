package bio.overture.ego.model.join;

import bio.overture.ego.model.enums.SqlFields;
import java.io.Serializable;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class UserApplicationId implements Serializable {

  @Column(name = SqlFields.USERID_JOIN)
  private UUID userId;

  @Column(name = SqlFields.APPID_JOIN)
  private UUID applicationId;
}
