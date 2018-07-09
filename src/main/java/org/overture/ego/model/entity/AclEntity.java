package org.overture.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.overture.ego.model.enums.Fields;
import org.overture.ego.view.Views;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name = "aclentity")
@Data
@JsonPropertyOrder({"id","owner","name"})
@JsonInclude(JsonInclude.Include.ALWAYS)
@EqualsAndHashCode(of={"id"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonView(Views.REST.class)
public class AclEntity {

  @Id
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  int id;

  @NonNull
  @Column(nullable = false, name = Fields.OWNER)
  int owner;

  @NonNull
  @Column(nullable = false, name = Fields.NAME)
  String name;

  @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinColumn(name=Fields.ENTITY)
  @JsonIgnore
  protected Set<AclGroupPermission> groupPermissions;

  @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinColumn(name=Fields.ENTITY)
  @JsonIgnore
  protected Set<AclUserPermission> userPermissions;

}
