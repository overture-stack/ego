package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.*;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = Tables.REFRESHTOKEN)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"jti"})
@EqualsAndHashCode(of = {"refreshId"})
public class RefreshToken implements Identifiable<UUID> {

  @Column(name = SqlFields.REFRESHID, updatable = false, nullable = false)
  @GenericGenerator(name = "refresh_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "refresh_uuid")
  private UUID id;

  @NotNull
  @Column(name = SqlFields.JTI, updatable = false, nullable = false)
  private UUID jti;

  @Id
  @MapsId(value = JavaFields.USER_ID) // to keep the token id as ID and change user's id to user_id
  @NotNull
  @OneToOne
  @JoinColumn(name = SqlFields.USERID_JOIN, nullable = false, updatable = false)
  private User user;

  @NotNull
  @Column(name = SqlFields.ISSUEDATE, updatable = false, nullable = false)
  private Date issueDate;

  @NotNull
  @Column(name = SqlFields.EXPIRYDATE, updatable = false, nullable = false)
  private Date expiryDate;

}
