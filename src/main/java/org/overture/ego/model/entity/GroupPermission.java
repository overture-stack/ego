package org.overture.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.overture.ego.model.enums.Fields;
import org.overture.ego.model.enums.AccessLevel;
import org.overture.ego.view.Views;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "grouppermission")
@Data
@JsonPropertyOrder({ "id", "policy", "owner", "access_level" })
@JsonInclude()
@EqualsAndHashCode(of = { "id" })
@TypeDef(
  name = "ego_access_level_enum",
  typeClass = PostgreSQLEnumType.class
)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonView(Views.REST.class)
public class GroupPermission extends Permission {
  @Id
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GenericGenerator(
    name = "group_permission_uuid",
    strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "group_permission_uuid")
  UUID id;

  @NonNull
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(nullable = false, name = Fields.POLICYID_JOIN)
  Policy policy;

  @NonNull
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(nullable = false, name = Fields.GROUPID_JOIN)
  Group owner;

  @NonNull
  @Column(nullable = false, name = Fields.ACCESS_LEVEL)
  @Enumerated(EnumType.STRING)
  @Type(type = "ego_access_level_enum")
  AccessLevel accessLevel;
}
