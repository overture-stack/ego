package org.overture.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.overture.ego.model.enums.AclMask;
import org.overture.ego.model.enums.Fields;
import org.overture.ego.view.Views;

import javax.persistence.*;

@Entity
@Table(name = "aclentry")
@Data
@JsonPropertyOrder({"id","entity","aclUser", "mask"})
@JsonInclude(JsonInclude.Include.ALWAYS)
@EqualsAndHashCode(of={"id"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonView(Views.REST.class)
public class AclUserPermissions {

  @Id
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  int id;

  // Many to One
  @NonNull
  @Column(nullable = false, name = Fields.ENTITY)
  int entity;

  // Many to Many
  @NonNull
  @Column(nullable = false, name = Fields.SID)
  int sid;

  @NonNull
  @Column(nullable = false, name = Fields.MASK)
  AclMask mask;
}