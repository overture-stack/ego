package bio.overture.ego.model.entity;

import static com.google.common.collect.Sets.newHashSet;

import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = Tables.POLICY)
@JsonInclude()
@JsonPropertyOrder({JavaFields.ID, JavaFields.OWNER, JavaFields.NAME})
@JsonView(Views.REST.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants
@EqualsAndHashCode(of = {"id"})
@NamedEntityGraph(
    name = "policy-entity-with-relationships",
    attributeNodes = {
      @NamedAttributeNode(value = JavaFields.USERPERMISSIONS),
      @NamedAttributeNode(value = JavaFields.GROUPPERMISSIONS),
    })
public class Policy implements Identifiable<UUID> {

  @Id
  @Column(name = SqlFields.ID, updatable = false, nullable = false)
  @GenericGenerator(name = "policy_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "policy_uuid")
  private UUID id;

  @NotNull
  @Column(name = SqlFields.NAME, unique = true, nullable = false)
  private String name;

  @JsonIgnore
  @Builder.Default
  @OneToMany(
      mappedBy = JavaFields.POLICY,
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private Set<GroupPermission> groupPermissions = newHashSet();

  @JsonIgnore
  @Builder.Default
  @OneToMany(
      mappedBy = JavaFields.POLICY,
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private Set<UserPermission> userPermissions = newHashSet();

  @JsonIgnore
  @Builder.Default
  @OneToMany(
      mappedBy = JavaFields.POLICY,
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private Set<ApiKeyScope> apiKeyScopes = newHashSet();
}
