package org.overture.ego.model.entity;

import lombok.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
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
public class Token {
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

  public String getAccessToken() {
    return this.token;
  }

  @OneToOne()
  @JoinColumn(name=Fields.ID)
  User owner;

  public String getUserName() {
    return owner.getName();
  }

  @OneToOne()
  @JoinColumn(name = Fields.ID)
  Application application;

  public String getClientId() {
    return application.getClientId();
  }

  @Column(nullable = false, name = Fields.ISSUEDATE, updatable = false)
  Date issueDate;

  public Long getSecondsUntilExpiry() {
    Date now=new Date();
    return issueDate.toInstant().getEpochSecond()-now.toInstant().getEpochSecond();
  }

  @Column(nullable = false, name = Fields.ISREVOKED, updatable = false)
  boolean isRevoked;

  @NonNull @ManyToMany()
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

  public Set<String> getScope() {
    return getPolicies().stream().map(policy->policy.getName()).collect(Collectors.toSet());
  }
}
