package bio.overture.ego.model.join;

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import jakarta.persistence.CascadeType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Entity
@Table(name = Tables.USER_APPLICATION)
@Builder
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user", "application"})
public class UserApplication implements Identifiable<UserApplicationId> {

  @EmbeddedId private UserApplicationId id;

  @MapsId(value = JavaFields.USER_ID)
  @JoinColumn(name = SqlFields.USERID_JOIN, nullable = false, updatable = false)
  @ManyToOne(
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      fetch = FetchType.LAZY)
  private User user;

  @MapsId(value = JavaFields.APPLICATION_ID)
  @JoinColumn(name = SqlFields.APPID_JOIN, nullable = false, updatable = false)
  @ManyToOne(
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      fetch = FetchType.LAZY)
  private Application application;
}
