package bio.overture.ego.model.entity;

import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.enums.Fields;
import bio.overture.ego.model.enums.LombokFields;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.val;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.joda.time.DateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static bio.overture.ego.utils.CollectionUtils.mapToSet;

@Entity
@Table(name = "token")
@Data
@ToString( exclude = {
    LombokFields.applications,
    LombokFields.owner,
    LombokFields.scopes
})
@EqualsAndHashCode(of = {"id"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Token implements Identifiable<UUID> {
  @Id
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GenericGenerator(name = "token_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "token_uuid")
  UUID id;

  @Column(nullable = false, name = Fields.TOKEN)
  @NonNull
  String token;

  @OneToOne()
  @JoinColumn(name = Fields.OWNER)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JsonIgnore
  User owner;

  @NonNull
  @ManyToMany()
  @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinTable(
      name = "tokenapplication",
      joinColumns = {@JoinColumn(name = Fields.TOKENID_JOIN)},
      inverseJoinColumns = {@JoinColumn(name = Fields.APPID_JOIN)})
  @JsonIgnore
  Set<Application> applications;

  @Column(nullable = false, name = Fields.ISSUEDATE, updatable = false)
  Date expires;

  @Column(nullable = false, name = Fields.ISREVOKED, updatable = false)
  boolean isRevoked;

  @OneToMany(mappedBy = "token")
  @Cascade(org.hibernate.annotations.CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JsonIgnore
  Set<TokenScope> scopes;

  public void setExpires(int seconds) {
    expires = DateTime.now().plusSeconds(seconds).toDate();
  }

  @NonNull
  public Long getSecondsUntilExpiry() {
    val seconds = (expires.getTime() - DateTime.now().getMillis()) / 1000;
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

  public void addApplication(Application app) {
    if (applications == null) {
      applications = new HashSet<>();
    }
    applications.add(app);
  }
}
