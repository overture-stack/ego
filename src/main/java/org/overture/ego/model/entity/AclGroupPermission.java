package org.overture.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "aclgrouppermission")
@Data
@JsonPropertyOrder({"id","entity","sid", "mask"})
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
public class AclGroupPermission extends AclPermission {

  @Id
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)

  int id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false, name = Fields.ENTITY)
  @JsonIgnore
  AclEntity entity;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(nullable = false, name = Fields.SID)
  @JsonIgnore
  Group sid;

  @NonNull
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, name = Fields.MASK)
  @Type( type = "ego_acl_enum" )
  AclMask mask;
}
