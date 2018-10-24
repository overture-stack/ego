package org.overture.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.overture.ego.model.enums.PolicyMask;
import org.overture.ego.model.enums.Fields;
import org.overture.ego.view.Views;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "acluserpermission")
@Data
@JsonPropertyOrder({"id","entity","sid","mask"})
@JsonInclude(JsonInclude.Include.ALWAYS)
@EqualsAndHashCode(of={"id"})
@TypeDef(
    name = "ego_acl_enum",
    typeClass = PostgreSQLEnumType.class
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonView(Views.REST.class)
public class UserPermission extends Permission {

  @Id
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GenericGenerator(
      name = "acl_user_permission_uuid",
      strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "acl_user_permission_uuid")
  UUID id;

  @NonNull
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(nullable = false, name = Fields.ENTITY)
  Policy entity;

  @NonNull
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(nullable = false, name = Fields.SID)
  User sid;

  @NonNull
  @Column(nullable = false, name = Fields.MASK)
  @Enumerated(EnumType.STRING)
  @Type( type = "ego_acl_enum" )
  PolicyMask mask;
}
