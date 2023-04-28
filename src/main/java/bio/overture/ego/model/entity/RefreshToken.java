package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import lombok.*;
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

  @JsonIgnore
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

  public Long getSecondsUntilExpiry() {
    val seconds = expiryDate.getTime() / 1000L - Calendar.getInstance().getTime().getTime() / 1000L;
    return seconds > 0 ? seconds : 0;
  }

  public void associateWithUser(@NonNull User user) {
    this.setUser(user);
    user.setRefreshToken(this);
  }
}
