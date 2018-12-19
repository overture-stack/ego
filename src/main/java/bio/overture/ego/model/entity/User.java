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

import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.enums.Fields;
import bio.overture.ego.model.enums.Tables;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static bio.overture.ego.utils.Converters.nullToEmptySet;
import static bio.overture.ego.utils.HibernateSessions.unsetSession;
import static bio.overture.ego.utils.PolicyPermissionUtils.extractPermissionStrings;
import static java.lang.String.format;

//TODO: simplify annotations. Find common annotations for Ego entities, and put them all under a single annotation
@Slf4j
@Entity
@Table(name = Tables.EGOUSER)
@Data
@ToString(exclude = {"groups", "applications", "userPermissions"})
@JsonPropertyOrder({
  "id",
  "name",
  "email",
  "role",
  "status",
  "groups",
  "applications",
  "userPermissions",
  "firstName",
  "lastName",
  "createdAt",
  "lastLogin",
  "preferredLanguage"
})
@JsonInclude()
@EqualsAndHashCode(of = {"id"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonView(Views.REST.class)
public class User implements PolicyOwner {

  //TODO: find JPA equivalent for GenericGenerator
  @Id
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GenericGenerator(name = "user_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "user_uuid")
  private UUID id;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @NonNull
  @Column(nullable = false, name = Fields.NAME, unique = true)
  private String name;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @NonNull
  @Column(nullable = false, name = Fields.EMAIL, unique = true)
  private String email;

  @NonNull
  @Column(nullable = false, name = Fields.ROLE)
  private String role;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = Fields.STATUS)
  private String status;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = Fields.FIRSTNAME)
  private String firstName;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = Fields.LASTNAME)
  private String lastName;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = Fields.CREATEDAT)
  private Date createdAt;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = Fields.LASTLOGIN)
  private Date lastLogin;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = Fields.PREFERREDLANGUAGE)
  private String preferredLanguage;


  @JsonIgnore
  @OneToMany(
      cascade = {CascadeType.PERSIST, CascadeType.MERGE},
      fetch = FetchType.LAZY)
  @JoinColumn(name = Fields.USERID_JOIN)
  protected Set<UserPermission> userPermissions; //TODO: @rtisma test that this initialization is the same as the init method (that it does not cause isseus with hibernate)

  @JsonIgnore
  @ManyToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = Tables.GROUP_USER,
      joinColumns = {@JoinColumn(name = Fields.USERID_JOIN)},
      inverseJoinColumns = {@JoinColumn(name = Fields.GROUPID_JOIN)})
  protected Set<Group> groups;

  //TODO @rtisma: test persist and merge cascade types for ManyToMany relationships. Must be able to step away from
  // happy path
  @JsonIgnore
  @ManyToMany(
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinTable(
      name = Tables.USER_APPLICATION,
      joinColumns = {@JoinColumn(name = Fields.USERID_JOIN)},
      inverseJoinColumns = {@JoinColumn(name = Fields.APPID_JOIN)})
  protected Set<Application> applications;

  @JsonIgnore
  public Set<Group> getGroups(){
    groups = nullToEmptySet(groups);
    return groups;
  }

  @JsonIgnore
  public Set<Application> getApplications(){
    applications = nullToEmptySet(applications);
    return applications;
  }

  @JsonIgnore
  public Set<UserPermission> getUserPermissions(){
    userPermissions = nullToEmptySet(userPermissions);
    return userPermissions;
  }

  @JsonIgnore
  public List<Permission> getPermissionsList() {
    // Get user's individual permission (stream)
    val userPermissions =
        Optional.ofNullable(this.getUserPermissions()).orElse(new HashSet<>()).stream();

    // Get permissions from the user's groups (stream)
    val userGroupsPermissions =
        Optional.ofNullable(this.getGroups())
            .orElse(new HashSet<>())
            .stream()
            .map(Group::getPermissions)
            .flatMap(List::stream);

    // Combine individual user permissions and the user's
    // groups (if they have any) permissions
    val combinedPermissions =
        Stream.concat(userPermissions, userGroupsPermissions)
            // .collect(Collectors.groupingBy(p -> p.getPolicy()));
            .collect(Collectors.groupingBy(this::getP));
    // If we have no permissions at all return an empty list
    if (combinedPermissions.values().size() == 0) {
      return new ArrayList<>();
    }

    // If we do have permissions ... sort the grouped permissions (by PolicyIdStringWithMaskName)
    // on PolicyMask, extracting the first value of the sorted list into the final
    // permissions list
    List<Permission> finalPermissionsList = new ArrayList<>();

    combinedPermissions.forEach(
        (entity, permissions) -> {
          permissions.sort(Comparator.comparing(Permission::getAccessLevel).reversed());
          finalPermissionsList.add(permissions.get(0));
        });
    return finalPermissionsList;
  }

  private Policy getP(Permission permission) {
    val p = permission.getPolicy();
    return p;
  }

  @JsonIgnore
  public Set<Scope> getScopes() {
    List<Permission> p;
    try {
      p = this.getPermissionsList();
    } catch (NullPointerException e) {
      log.error(format("Can't get permissions for user '%s'", getName()));
      p = Collections.emptyList();
    }

    return mapToSet(p, Permission::toScope);
  }

  // Creates permissions in JWTAccessToken::context::user
  @JsonView(Views.JWTAccessToken.class)
  public List<String> getPermissions() {
    val finalPermissionsList = getPermissionsList();
    // Convert final permissions list for JSON output
    return extractPermissionStrings(finalPermissionsList);
  }

  //TODO @rtisma: test this associateWithApplication
  public void associateWithApplication(@NonNull Application app) {
    getApplications().add(app);
    app.getUsers().add(this);
  }

  //TODO @rtisma: test this associateWithGroup
  public void associateWithGroup(@NonNull Group g) {
    getGroups().add(g);
    g.getUsers().add(this);
  }

  //TODO @rtisma: test this associateWithPermission
  public void associateWithPermission(@NonNull UserPermission permission){
    getUserPermissions().add(permission);
    permission.setOwner(this);
  }

  public void update(User other) {
    this.name = other.getName();
    this.firstName = other.getFirstName();
    this.lastName = other.getLastName();
    this.role = other.getRole();
    this.status = other.getStatus();
    this.preferredLanguage = other.getPreferredLanguage();
    this.lastLogin = other.getLastLogin();

    // Don't merge the ID, CreatedAt, or LastLogin date - those are procedural.

    // Don't merge groups, applications or userPermissions if not present in other
    // This is because the PUT action for update usually does not include these fields
    // as a consequence of the GET option to retrieve a user not including these fields
    // To clear applications, groups or userPermissions, use the dedicated services
    // for deleting associations or pass in an empty Set.
    if (other.applications != null) {
      unsetSession(other.getApplications());
      this.applications = other.getApplications();
    }

    if (other.groups != null) {
      unsetSession(other.getGroups());
      this.groups = other.getGroups();
    }

    if (other.userPermissions != null) {
      unsetSession(other.getUserPermissions());
      this.userPermissions = other.getUserPermissions();
    }
  }
}
