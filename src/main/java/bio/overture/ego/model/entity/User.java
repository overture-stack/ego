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

import static bio.overture.ego.grpc.ProtoUtils.toProtoString;
import static bio.overture.ego.model.enums.AccessLevel.EGO_ENUM;
import static bio.overture.ego.service.UserService.resolveUsersPermissions;
import static bio.overture.ego.utils.CollectionUtils.mapToImmutableSet;
import static bio.overture.ego.utils.PolicyPermissionUtils.extractPermissionStrings;
import static com.google.common.collect.Sets.newHashSet;

import bio.overture.ego.model.dto.Scope;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
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

import io.swagger.annotations.ApiParam;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.LazyInitializationException;
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
      LombokFields.userApplications,
      LombokFields.userPermissions,
      LombokFields.tokens
    })
@JsonPropertyOrder({
  JavaFields.ID,
  JavaFields.NAME,
  JavaFields.EMAIL,
  JavaFields.USERTYPE,
  JavaFields.STATUS,
  JavaFields.USERGROUPS,
  JavaFields.USERAPPLICATIONS,
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

  // TODO: [rtisma] move getPermissions to UserService once DTO task is complete.
  // JsonViews creates
  // a dependency for this method. For now, using a UserService static method.
  // Creates permissions in JWTAccessToken::context::user
  @JsonView(Views.JWTAccessToken.class)
  public List<String> getPermissions() {
    return extractPermissionStrings(resolveUsersPermissions(this));
  }

  /*
   * ===========================
   * Protobuf Conversion Methods
   * ===========================
   */

  public bio.overture.ego.grpc.User toProto() {

    val builder =
        bio.overture.ego.grpc.User.newBuilder()
            .setId(toProtoString(this.getId()))
            .setEmail(toProtoString(this.getEmail()))
            .setFirstName(toProtoString(this.getFirstName()))
            .setLastName(toProtoString(this.getLastName()))
            .setName(toProtoString(this.getName()))
            .setCreatedAt(toProtoString(this.getCreatedAt()))
            .setLastLogin(toProtoString(this.getLastLogin()))
            .setPreferredLanguage(toProtoString(this.getPreferredLanguage()))
            .setStatus(toProtoString(this.getStatus()))
            .setType(toProtoString(this.getType()));

    try {
      final Set<String> applications =
          mapToImmutableSet(
              this.getUserApplications(),
              userApplication -> userApplication.getApplication().getId().toString());
      builder.addAllApplications(applications);

    } catch (LazyInitializationException e) {
      log.error(
          "Could not add applications to gRPC User Object, cannot lazy load values:",
          e.getMessage());
    }

    try {
      final Set<String> groups =
          mapToImmutableSet(this.getUserGroups(), group -> group.getGroup().getId().toString());
      builder.addAllGroups(groups);

    } catch (LazyInitializationException e) {
      log.error(
          "Could not add groups to gRPC User Object, cannot lazy load values:", e.getMessage());
    }

    try {
      final Set<String> permissions =
          mapToImmutableSet(
              resolveUsersPermissions(this), permission -> new Scope(permission).toString());
      builder.addAllScopes(permissions);

    } catch (LazyInitializationException e) {
      log.error(
          "Could not add permissions to gRPC User Object, cannot lazy load values:",
          e.getMessage());
    }

    return builder.build();
  }

  public static User fromProto(bio.overture.ego.grpc.User proto) {
    val user = new User();

    if (proto.hasEmail()) {
      user.setEmail(proto.getEmail().getValue());
    }

    if (proto.hasFirstName()) {
      user.setFirstName(proto.getFirstName().getValue());
    }

    if (proto.hasLastName()) {
      user.setLastName(proto.getLastName().getValue());
    }

    if (proto.hasName()) {
      user.setName(proto.getName().getValue());
    }

    try {
      if (proto.hasCreatedAt()) {
        final String date = proto.getCreatedAt().getValue();
        if (!date.isEmpty()) {
          user.setCreatedAt(dateFormat.parse(date));
        }
      }
    } catch (ParseException e) {
      log.debug("Could not parse created date from protobuf: ", e.getMessage());
    }

    try {
      if (proto.hasLastLogin()) {
        final String date = proto.getLastLogin().getValue();
        if (!date.isEmpty()) {
          user.setLastLogin(dateFormat.parse(date));
        }
      }
    } catch (ParseException e) {
      log.debug("Could not parse last login date from protobuf: ", e.getMessage());
    }

    try {
      if (proto.hasPreferredLanguage()) {
        final String preferredLanguage = proto.getPreferredLanguage().getValue();
        user.setPreferredLanguage(LanguageType.valueOf(preferredLanguage));
      }
    } catch (IllegalArgumentException e) {
      log.debug("Could not set preferred language from protobuf: ", e.getMessage());
    }

    try {
      if (proto.hasStatus()) {

        final String status = proto.getStatus().getValue();
        user.setStatus(StatusType.resolveStatusType(status));
      }
    } catch (IllegalArgumentException e) {
      log.debug("Could not set status from protobuf: ", e.getMessage());
    }

    try {
      if (proto.hasType()) {
        final String userType = proto.getType().getValue();
        user.setType(UserType.resolveUserType(userType));
      }

    } catch (IllegalArgumentException e) {
      log.debug("Could not set user type from protobuf: ", e.getMessage());
    }

    return user;
  }

  private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

  @PrePersist
  private void onCreate() {
    this.createdAt = new Date();
  }
}
