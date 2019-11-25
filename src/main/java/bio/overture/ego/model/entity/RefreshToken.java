package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import com.fasterxml.jackson.annotation.JsonIgnore;

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

//  @Id
//  @Column(name = SqlFields.USER_ID, nullable = false, updatable = false)
//  private UUID id;
//
//  @NotNull
//  @Column(name = SqlFields.REFRESHID, updatable = false, nullable = false)
//  @GenericGenerator(name = "refresh_uuid", strategy = "org.hibernate.id.UUIDGenerator")
//  @GeneratedValue(generator = "refresh_uuid")
//  private UUID refreshId;

  @Id
  @Column(name = SqlFields.REFRESHID, nullable = false, updatable = false)
  @GenericGenerator(name = "refresh_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "refresh_uuid")
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = SqlFields.USER_ID, referencedColumnName = SqlFields.ID)
  private User user;

  @NotNull
  @Column(name = SqlFields.JTI, updatable = false, nullable = false)
  private UUID jti;

//  @MapsId
//  @OneToOne(fetch = FetchType.LAZY)
//  private User user;

  @NotNull
  @Column(name = SqlFields.ISSUEDATE, updatable = false, nullable = false)
  private Date issueDate;

  @NotNull
  @Column(name = SqlFields.EXPIRYDATE, updatable = false, nullable = false)
  private Date expiryDate;

}
