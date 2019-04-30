/*
 * Copyright (c) 2017. The Ontario Institute for Cancer Research. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bio.overture.ego.model.entity;

import static bio.overture.ego.model.enums.AccessLevel.EGO_ENUM;
import static bio.overture.ego.service.UserService.resolveUsersPermissions;
import static bio.overture.ego.utils.PolicyPermissionUtils.extractPermissionStrings;
import static com.google.common.collect.Sets.newHashSet;

import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.enums.LanguageType;
import bio.overture.ego.model.enums.LombokFields;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.StatusType;
import bio.overture.ego.model.enums.Tables;
import bio.overture.ego.model.enums.UserType;
import bio.overture.ego.model.join.UserApplication;
import bio.overture.ego.model.join.UserGroup;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Slf4j
@Entity
@Table(name = Tables.EGOUSER)
@Data
@ToString(
    exclude = {
      LombokFields.userGroups,
      LombokFields.applications,
      LombokFields.userPermissions,
      LombokFields.tokens
    })
@JsonPropertyOrder({
  JavaFields.ID,
  JavaFields.NAME,
  JavaFields.EMAIL,
  JavaFields.USERTYPE,
  JavaFields.STATUS,
  JavaFields.GROUPS,
  JavaFields.APPLICATIONS,
  JavaFields.USERPERMISSIONS,
  JavaFields.FIRSTNAME,
  JavaFields.LASTNAME,
  JavaFields.CREATEDAT,
  JavaFields.LASTLOGIN,
  JavaFields.PREFERREDLANGUAGE
})
@JsonInclude()
@EqualsAndHashCode(of = {LombokFields.id})
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonView(Views.REST.class)
@TypeDef(name = EGO_ENUM, typeClass = PostgreSQLEnumType.class)
public class User implements PolicyOwner, NameableEntity<UUID> {

  // TODO: find JPA equivalent for GenericGenerator
  @Id
  @Column(name = SqlFields.ID, updatable = false, nullable = false)
  @GenericGenerator(name = "user_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "user_uuid")
  private UUID id;

  @NotNull
  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.NAME, unique = true, nullable = false)
  private String name;

  @NotNull
  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.EMAIL, unique = true, nullable = false)
  private String email;

  @NotNull
  @Type(type = EGO_ENUM)
  @Enumerated(EnumType.STRING)
  @Column(name = SqlFields.TYPE, nullable = false)
  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  private UserType type;

  @NotNull
  @Type(type = EGO_ENUM)
  @Enumerated(EnumType.STRING)
  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.STATUS, nullable = false)
  private StatusType status;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.FIRSTNAME)
  private String firstName;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.LASTNAME)
  private String lastName;

  @NotNull
  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.CREATEDAT)
  @Temporal(value = TemporalType.TIMESTAMP)
  private Date createdAt;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.LASTLOGIN)
  @Temporal(value = TemporalType.TIMESTAMP)
  private Date lastLogin;

  @Type(type = EGO_ENUM)
  @Enumerated(EnumType.STRING)
  @Column(name = SqlFields.PREFERREDLANGUAGE)
  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  private LanguageType preferredLanguage;

  @JsonIgnore
  @OneToMany(
      mappedBy = JavaFields.OWNER,
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @Builder.Default
  private Set<UserPermission> userPermissions = newHashSet();

  @JsonIgnore
  @Builder.Default
  @OneToMany(
      mappedBy = JavaFields.OWNER,
      orphanRemoval = true,
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY)
  private Set<Token> tokens = newHashSet();

  @JsonIgnore
  @Builder.Default
  @OneToMany(
      mappedBy = JavaFields.USER,
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private Set<UserGroup> userGroups = newHashSet();

  @JsonIgnore
  @Builder.Default
  @OneToMany(
      mappedBy = JavaFields.USER,
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private Set<UserApplication> userApplications = newHashSet();

  // TODO: [rtisma] move getPermissions to UserService once DTO task is complete. JsonViews creates
  // a dependency for this method. For now, using a UserService static method.
  // Creates permissions in JWTAccessToken::context::user
  @JsonView(Views.JWTAccessToken.class)
  public List<String> getPermissions() {
    return extractPermissionStrings(resolveUsersPermissions(this));
  }

  @PrePersist
  private void onCreate() {
    this.createdAt = new Date();
  }
}
