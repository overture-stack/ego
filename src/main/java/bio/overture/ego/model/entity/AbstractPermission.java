package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.enums.SqlFields;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

@Data
@MappedSuperclass
@FieldNameConstants
@EqualsAndHashCode(of = {"id"})
@ToString(exclude = {"policy"})
@JsonPropertyOrder({JavaFields.ID, JavaFields.POLICY, JavaFields.OWNER, JavaFields.ACCESS_LEVEL})
@JsonSubTypes({
  @JsonSubTypes.Type(value = UserPermission.class, name = JavaFields.USERPERMISSIONS),
  @JsonSubTypes.Type(value = GroupPermission.class, name = JavaFields.GROUPPERMISSION)
})
public abstract class AbstractPermission<O extends Identifiable<UUID>>
    implements Identifiable<UUID> {

  @Id
  @Column(name = SqlFields.ID, updatable = false, nullable = false)
  @GenericGenerator(name = "permission_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "permission_uuid")
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = SqlFields.POLICYID_JOIN, nullable = false)
  private Policy policy;

  @NotNull
  @Column(name = SqlFields.ACCESS_LEVEL, nullable = false)
  @Enumerated(EnumType.STRING)
  @Type(PostgreSQLEnumType.class)
  private AccessLevel accessLevel;

  public abstract O getOwner();

  public abstract void setOwner(O owner);
}
