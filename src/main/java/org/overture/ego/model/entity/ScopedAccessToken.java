package org.overture.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.joda.time.DateTime;
import org.overture.ego.model.enums.Fields;

import javax.persistence.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "token")
@Data
@EqualsAndHashCode(of={"id"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScopedAccessToken {
  @Id
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GenericGenerator(
      name = "token_entity_uuid",
      strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "token_entity_uuid")
  UUID id;

  @Column(nullable = false, name = Fields.TOKEN)
  @NonNull
  String token;

  @OneToOne()
  @JoinColumn(name=Fields.OWNER)
  @JsonIgnore
  User owner;

  @NonNull @ManyToMany()
  @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinTable(name = "tokenapplication", joinColumns = {@JoinColumn(name = Fields.TOKENID_JOIN)},
    inverseJoinColumns = {@JoinColumn(name = Fields.APPID_JOIN)})
  Set<Application> applications;

  @Column(nullable = false, name = Fields.ISSUEDATE, updatable = false)
  Date expires;

  public void setExpires(int seconds) {
    expires = DateTime.now().plusSeconds(seconds).toDate();
  }
  @NonNull
  public Long getSecondsUntilExpiry() {
    val seconds = (expires.getTime() - DateTime.now().getMillis()) / 1000;
    return seconds > 0 ? seconds:0;
  }

  @Column(nullable = false, name = Fields.ISREVOKED, updatable = false)
  boolean isRevoked;

  @ManyToMany()
  @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinTable(name = "tokenscope", joinColumns = {@JoinColumn(name = Fields.TOKENID_JOIN)},
    inverseJoinColumns = {@JoinColumn(name = Fields.SCOPEID_JOIN)})
  Set<Policy> policies;

  public void addPolicy(Policy policy) {
    if (policies == null) {
      policies = new HashSet<>();
    }
    policies.add(policy);
  }

  public void addApplication(Application app) {
    if (applications == null) {
      applications = new HashSet<>();
    }
    applications.add(app);
  }

  public Set<String> getScope() {
    return getPolicies().stream().map(policy->policy.getName()).collect(Collectors.toSet());
  }
}
