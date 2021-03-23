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
import static com.google.common.collect.Sets.newHashSet;

import bio.overture.ego.model.enums.ApplicationType;
import bio.overture.ego.model.enums.JavaFields;
import bio.overture.ego.model.enums.SqlFields;
import bio.overture.ego.model.enums.StatusType;
import bio.overture.ego.model.enums.Tables;
import bio.overture.ego.model.join.GroupApplication;
import bio.overture.ego.model.join.UserApplication;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
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
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Entity
@Table(name = Tables.APPLICATION)
@Data
@Accessors(chain = true)
@ToString(exclude = {"groupApplications", "userApplications", "applicationPermissions"})
@JsonPropertyOrder({
  JavaFields.ID,
  JavaFields.NAME,
  JavaFields.APPLICATIONTYPE,
  JavaFields.CLIENTID,
  JavaFields.CLIENTSECRET,
  JavaFields.REDIRECTURI,
  JavaFields.DESCRIPTION,
  JavaFields.STATUS,
  JavaFields.ERROR_REDIRECT_URI
})
@EqualsAndHashCode(of = {"id"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonView(Views.REST.class)
@FieldNameConstants
@TypeDef(name = "application_type_enum", typeClass = PostgreSQLEnumType.class)
@TypeDef(name = EGO_ENUM, typeClass = PostgreSQLEnumType.class)
@JsonInclude(JsonInclude.Include.CUSTOM)
public class Application implements PolicyOwner, NameableEntity<UUID> {

  @Id
  @Column(name = SqlFields.ID, updatable = false, nullable = false)
  @GenericGenerator(name = "application_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "application_uuid")
  private UUID id;

  @NotNull
  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.NAME, nullable = false, unique = true)
  private String name;

  @NotNull
  @Type(type = EGO_ENUM)
  @Enumerated(EnumType.STRING)
  @Column(name = SqlFields.TYPE, nullable = false)
  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  private ApplicationType type;

  @NotNull
  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.CLIENTID, nullable = false, unique = true)
  private String clientId;

  @NotNull
  @Column(name = SqlFields.CLIENTSECRET, nullable = false)
  private String clientSecret;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.REDIRECTURI)
  private String redirectUri;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.DESCRIPTION)
  private String description;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.ERRORREDIRECTURI)
  private String errorRedirectUri;

  @NotNull
  @Type(type = EGO_ENUM)
  @Enumerated(EnumType.STRING)
  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = SqlFields.STATUS, nullable = false)
  private StatusType status;

  @JsonIgnore
  @Builder.Default
  @OneToMany(
      mappedBy = JavaFields.APPLICATION,
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private Set<GroupApplication> groupApplications = newHashSet();

  @JsonIgnore
  @Builder.Default
  @OneToMany(
      mappedBy = JavaFields.APPLICATION,
      cascade = CascadeType.ALL,
      fetch = FetchType.LAZY,
      orphanRemoval = true)
  private Set<UserApplication> userApplications = newHashSet();

  @JsonIgnore
  @OneToMany(
      mappedBy = JavaFields.OWNER,
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @Builder.Default
  private Set<ApplicationPermission> applicationPermissions = newHashSet();
}
