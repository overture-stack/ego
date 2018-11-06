package org.overture.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.joda.time.DateTime;
import org.overture.ego.model.dto.Scope;
import org.overture.ego.model.enums.Fields;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.overture.ego.utils.CollectionUtils.mapToSet;

@Entity
@Table(name = "token")
@Data
@EqualsAndHashCode(of = { "id" })
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScopedAccessToken {
  @Id
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GenericGenerator(
    name = "token_uuid",
    strategy = "org.hibernate.id.UUIDGenerator")
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

  @NonNull @ManyToMany()
  @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinTable(name = "tokenapplication", joinColumns = { @JoinColumn(name = Fields.TOKENID_JOIN) },
    inverseJoinColumns = { @JoinColumn(name = Fields.APPID_JOIN) })
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
    scopes.add(new TokenScope(this, scope.getPolicy(), scope.getPolicyMask()));
  }

  @JsonIgnore
  public  Set<Scope> scopes() {
    return mapToSet(scopes, s -> new Scope(s.getPolicy(), s.getAccessLevel()) );
  }

  public void setScopes(Set<Scope> scopes) {
    this.scopes = mapToSet(scopes, s -> new TokenScope(this, s.getPolicy(), s.getPolicyMask()));
  }

  public void addApplication(Application app) {
    if (applications == null) {
      applications = new HashSet<>();
    }
    applications.add(app);
  }
}
