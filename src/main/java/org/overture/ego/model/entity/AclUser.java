package org.overture.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.overture.ego.model.enums.Fields;
import org.overture.ego.view.Views;

import javax.persistence.*;

@Entity
@Table(name = "acluser")
@Data
@JsonPropertyOrder({"id","egoId","principal"})
@JsonInclude(JsonInclude.Include.ALWAYS)
@EqualsAndHashCode(of={"id"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonView(Views.REST.class)
public class AclUser {

  @Id
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  int id;

  @NonNull
  @Column(nullable = false, name = Fields.EGO_ID)
  int egoId;

  @NonNull
  @Column(nullable = false, name = Fields.PRINCIPAL)
  boolean principal;
}
