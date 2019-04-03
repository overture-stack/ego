package bio.overture.ego.model.join;

import bio.overture.ego.model.enums.SqlFields;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class GroupApplicationId implements Serializable {

  @Column(name = SqlFields.GROUPID_JOIN)
  private UUID groupId;

  @Column(name = SqlFields.APPID_JOIN)
  private UUID applicationId;
}
