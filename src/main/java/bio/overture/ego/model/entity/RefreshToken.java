package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;

import java.util.*;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = Tables.REFRESHTOKEN)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@ToString(exclude = {"user"})
public class RefreshToken implements Identifiable<UUID> {

  @Id
  @Column(name = SqlFields.ID, nullable = false, updatable = false)
  @GenericGenerator(name = "refresh_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "refresh_uuid")
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = SqlFields.USERID_JOIN, referencedColumnName = SqlFields.ID, nullable = false)
  private User user;

  @NotNull
  @Column(name = SqlFields.JTI, updatable = false, nullable = false)
  private UUID jti;

  @NotNull
  @Column(name = SqlFields.ISSUEDATE)
  @Temporal(value = TemporalType.TIMESTAMP)
  private Date issueDate;

  @NotNull
  @Column(name = SqlFields.EXPIRYDATE)
  @Temporal(value = TemporalType.TIMESTAMP)
  private Date expiryDate;

}
