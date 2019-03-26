package bio.overture.ego.model.entity;

import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static com.google.common.collect.Sets.newHashSet;

import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.enums.LombokFields;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.Tables;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.*;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.val;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = Tables.TOKEN)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {LombokFields.owner, LombokFields.scopes})
@EqualsAndHashCode(of = {LombokFields.id})
public class Token implements Identifiable<UUID> {

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
  private Set<TokenScope> scopes = newHashSet();

  public Long getSecondsUntilExpiry() {
    val seconds = expiryDate.getTime() / 1000L - Calendar.getInstance().getTime().getTime() / 1000L;
    return seconds > 0 ? seconds : 0;
  }

  public void addScope(Scope scope) {
    if (scopes == null) {
      scopes = new HashSet<>();
    }
    scopes.add(new TokenScope(this, scope.getPolicy(), scope.getAccessLevel()));
  }

  @JsonIgnore
  public Set<Scope> scopes() {
    return mapToSet(scopes, s -> new Scope(s.getPolicy(), s.getAccessLevel()));
  }

  public void setScopes(Set<Scope> scopes) {
    this.scopes = mapToSet(scopes, s -> new TokenScope(this, s.getPolicy(), s.getAccessLevel()));
  }
}
