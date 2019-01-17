package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.enums.Fields;
import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.enums.LombokFields;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

import static bio.overture.ego.model.enums.AccessLevel.EGO_ACCESS_LEVEL_ENUM;

@Data
@MappedSuperclass
@EqualsAndHashCode(of = { LombokFields.id })
@TypeDef(name = EGO_ACCESS_LEVEL_ENUM, typeClass = PostgreSQLEnumType.class)
@JsonPropertyOrder({
    JavaFields.ID,
    JavaFields.POLICY,
    JavaFields.OWNER,
    JavaFields.ACCESS_LEVEL
})
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonSubTypes({
    @JsonSubTypes.Type(value=UserPermission.class, name=JavaFields.USERPERMISSIONS),
    @JsonSubTypes.Type(value=GroupPermission.class, name=JavaFields.GROUPPERMISSION)
})
public abstract class AbstractPermission implements Identifiable<UUID> {

  @Id
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GenericGenerator(name = "permission_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "permission_uuid")
  private UUID id;

  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(nullable = false, name = Fields.POLICYID_JOIN)
  private Policy policy;

  @Column(nullable = false, name = Fields.ACCESS_LEVEL)
  @Enumerated(EnumType.STRING)
  @Type(type = EGO_ACCESS_LEVEL_ENUM)
  private AccessLevel accessLevel;

}
