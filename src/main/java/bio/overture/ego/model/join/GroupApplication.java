package bio.overture.ego.model.join;

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.enums.JavaFields;
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
@Table(name = Tables.GROUP_APPLICATION)
@Builder
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"group", "application"})
public class GroupApplication implements Identifiable<GroupApplicationId> {

  @EmbeddedId private GroupApplicationId id;

  @MapsId(value = JavaFields.GROUP_ID)
  @JoinColumn(name = SqlFields.GROUPID_JOIN, nullable = false, updatable = false)
  @ManyToOne(
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      fetch = FetchType.LAZY)
  private Group group;

  @MapsId(value = JavaFields.APPLICATION_ID)
  @JoinColumn(name = SqlFields.APPID_JOIN, nullable = false, updatable = false)
  @ManyToOne(
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      fetch = FetchType.LAZY)
  private Application application;
}
