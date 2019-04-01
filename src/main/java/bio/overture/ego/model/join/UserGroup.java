package bio.overture.ego.model.join;

import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.enums.LombokFields;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import javax.persistence.CascadeType;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Entity
@Table(name = Tables.USER_GROUP)
@Builder
@EqualsAndHashCode(of = {LombokFields.id})
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {JavaFields.USER, JavaFields.GROUP})
public class UserGroup implements Identifiable<UserGroupId> {

  @EmbeddedId private UserGroupId id;

  @MapsId(value = JavaFields.USER_ID)
  @JoinColumn(name = SqlFields.USERID_JOIN, nullable = false, updatable = false)
  @ManyToOne(
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      fetch = FetchType.LAZY)
  private User user;

  @MapsId(value = JavaFields.GROUP_ID)
  @JoinColumn(name = SqlFields.GROUPID_JOIN, nullable = false, updatable = false)
  @ManyToOne(
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      fetch = FetchType.LAZY)
  private Group group;
}
