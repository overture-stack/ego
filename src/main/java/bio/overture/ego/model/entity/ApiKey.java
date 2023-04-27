package bio.overture.ego.model.entity;

import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static com.google.common.collect.Sets.newHashSet;

import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.val;
import org.hibernate.annotations.GenericGenerator;

// TODO: rename TOKEN to API_TOKEN [anncatton]
@Entity
@Table(name = Tables.TOKEN)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"owner", "scopes"})
@EqualsAndHashCode(of = {"id"})
public class ApiKey implements Identifiable<UUID> {

  @Id
  @Column(name = SqlFields.ID, updatable = false, nullable = false)
  @GenericGenerator(name = "token_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "token_uuid")
  private UUID id;

  @NotNull
  @Column(name = SqlFields.NAME, unique = true, nullable = false)
  private String name;

  @NotNull
  @Column(name = SqlFields.ISSUEDATE, updatable = false, nullable = false)
  private Date issueDate;

  @NotNull
  @Column(name = SqlFields.EXPIRYDATE, updatable = false, nullable = false)
  private Date expiryDate;

  @NotNull
  @Column(name = SqlFields.ISREVOKED, nullable = false)
  private boolean isRevoked;

  @Column(name = SqlFields.DESCRIPTION)
  private String description;

  @NotNull
  @JsonIgnore
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = SqlFields.OWNER, nullable = false)
  private User owner;

  @JsonIgnore
  @OneToMany(
      mappedBy = JavaFields.TOKEN,
      orphanRemoval = true,
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY)
  @Builder.Default
  private Set<ApiKeyScope> scopes = newHashSet();

  public Long getSecondsUntilExpiry() {
    val seconds = expiryDate.getTime() / 1000L - Calendar.getInstance().getTime().getTime() / 1000L;
    return seconds > 0 ? seconds : 0;
  }

  public void addScope(Scope scope) {
    if (scopes == null) {
      scopes = new HashSet<>();
    }
    scopes.add(new ApiKeyScope(this, scope.getPolicy(), scope.getAccessLevel()));
  }

  @JsonIgnore
  public Set<Scope> scopes() {
    return mapToSet(scopes, s -> new Scope(s.getPolicy(), s.getAccessLevel()));
  }

  public void setScopes(Set<Scope> scopes) {
    this.scopes = mapToSet(scopes, s -> new ApiKeyScope(this, s.getPolicy(), s.getAccessLevel()));
  }
}
