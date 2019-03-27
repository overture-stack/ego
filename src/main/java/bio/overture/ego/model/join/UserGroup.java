package bio.overture.ego.model.join;

import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;

@Data
@Entity
@Table(name = Tables.USER_GROUP)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {JavaFields.USER, JavaFields.GROUP})
public class UserGroup implements Identifiable<UserGroupId> {

  @EmbeddedId private UserGroupId id;

  @ManyToOne
  @MapsId(value = JavaFields.USER_ID)
  @JoinColumn(name = SqlFields.USERID_JOIN, nullable = false, updatable = false)
  private User user;

  @ManyToOne
  @MapsId(value = JavaFields.GROUP_ID)
  @JoinColumn(name = SqlFields.GROUPID_JOIN, nullable = false, updatable = false)
  private Group group;
}
