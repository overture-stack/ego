package bio.overture.ego.model.entity;

import bio.overture.ego.model.enums.Fields;
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
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Table(name = "policy")
@Data
@JsonPropertyOrder({"id", "owner", "name"})
@JsonInclude()
@EqualsAndHashCode(of = {"id"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonView(Views.REST.class)
public class Policy implements Identifiable<UUID> {

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinColumn(name = Fields.POLICYID_JOIN)
  @JsonIgnore
  protected Set<GroupPermission> groupPermissions;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinColumn(name = Fields.POLICYID_JOIN)
  @JsonIgnore
  protected Set<UserPermission> userPermissions;

  @Id
  @Column(name = Fields.ID, updatable = false, nullable = false)
  @GenericGenerator(name = "policy_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "policy_uuid")
  UUID id;

  @NotNull
  @Column(name = Fields.OWNER, nullable = false)
  UUID owner;

  @NotNull
  @Column(name = Fields.NAME, unique = true, nullable = false)
  String name;

  public void update(Policy other) {
    this.owner = other.owner;
    this.name = other.name;

    // Don't merge the ID - that is procedural.

    // Don't merge groupPermissions or userPermissions if not present in other.
    // This is because the PUT action for update usually does not include these fields
    // as a consequence of the GET option to retrieve a aclEntity not including these fields
    // To clear groupPermissions and userPermissions, use the dedicated services for deleting
    // associations or pass in an empty Set.
    if (other.groupPermissions != null) {
      this.groupPermissions = other.groupPermissions;
    }

    if (other.userPermissions != null) {
      this.userPermissions = other.userPermissions;
    }
  }
}
