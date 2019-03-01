package bio.overture.ego.model.entity;

import static bio.overture.ego.model.enums.AccessLevel.EGO_ACCESS_LEVEL_ENUM;

import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.enums.LombokFields;
import bio.overture.ego.model.enums.SqlFields;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Data
@MappedSuperclass
@EqualsAndHashCode(of = {LombokFields.id})
@ToString(exclude = {LombokFields.policy})
@TypeDef(name = EGO_ACCESS_LEVEL_ENUM, typeClass = PostgreSQLEnumType.class)
@JsonPropertyOrder({JavaFields.ID, JavaFields.POLICY, JavaFields.OWNER, JavaFields.ACCESS_LEVEL})
@JsonInclude(JsonInclude.Include.ALWAYS)
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
  @Type(type = EGO_ACCESS_LEVEL_ENUM)
  private AccessLevel accessLevel;

  public abstract O getOwner();

  public abstract void setOwner(O owner);
}
