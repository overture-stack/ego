package org.overture.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.overture.ego.model.enums.AclMask;
import org.overture.ego.model.enums.Fields;
import org.overture.ego.view.Views;

import javax.persistence.*;

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
public class AclUserPermission extends AclPermission {

  @Id
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  int id;

  @NonNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false, name = Fields.ENTITY)
  AclEntity entity;

  @NonNull
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false, name = Fields.SID)
  User sid;

  @NonNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, name = Fields.MASK)
  @Type( type = "ego_acl_enum" )
  AclMask mask;
}
