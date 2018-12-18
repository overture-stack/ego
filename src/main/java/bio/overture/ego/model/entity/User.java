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
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.enums.Fields;
import bio.overture.ego.model.enums.Tables;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static bio.overture.ego.utils.HibernateSessions.unsetSession;
import static bio.overture.ego.utils.PolicyPermissionUtils.extractPermissionStrings;
import static java.lang.String.format;

@Slf4j
@Entity
@Table(name = "egouser")
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

  @ManyToMany(targetEntity = Group.class)
  @Cascade(org.hibernate.annotations.CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinTable(
      name = Tables.GROUP_USER,
      joinColumns = {@JoinColumn(name = Fields.USERID_JOIN)},
      inverseJoinColumns = {@JoinColumn(name = Fields.GROUPID_JOIN)})
  @JsonIgnore
  protected Set<Group> groups;

  @ManyToMany(targetEntity = Application.class)
  @Cascade(org.hibernate.annotations.CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinTable(
      name = "userapplication",
      joinColumns = {@JoinColumn(name = Fields.USERID_JOIN)},
      inverseJoinColumns = {@JoinColumn(name = Fields.APPID_JOIN)})
  @JsonIgnore
  protected Set<Application> applications;

  @OneToMany(cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinColumn(name = Fields.USERID_JOIN)
  @JsonIgnore
  protected List<UserPermission> userPermissions;

  @Id
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GenericGenerator(name = "user_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "user_uuid")
  UUID id;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @NonNull
  @Column(nullable = false, name = Fields.NAME, unique = true)
  String name;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @NonNull
  @Column(nullable = false, name = Fields.EMAIL, unique = true)
  String email;

  @NonNull
  @Column(nullable = false, name = Fields.ROLE)
  String role;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = Fields.STATUS)
  String status;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = Fields.FIRSTNAME)
  String firstName;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = Fields.LASTNAME)
  String lastName;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = Fields.CREATEDAT)
  Date createdAt;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = Fields.LASTLOGIN)
  Date lastLogin;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = Fields.PREFERREDLANGUAGE)
  String preferredLanguage;

  @JsonIgnore
  public HashSet<Permission> getPermissionsList() {
    // Get user's individual permission (stream)
    val userPermissions =
        Optional.ofNullable(this.getUserPermissions()).orElse(new ArrayList<>()).stream();

    // Get permissions from the user's groups (stream)
    val userGroupsPermissions =
        Optional.ofNullable(this.getGroups())
            .orElse(new HashSet<>())
            .stream()
            .map(Group::getPermissions)
            .flatMap(Collection::stream);

    // Combine individual user permissions and the user's
    // groups (if they have any) permissions
    val combinedPermissions =
        Stream.concat(userPermissions, userGroupsPermissions)
            // .collect(Collectors.groupingBy(p -> p.getPolicy()));
            .collect(Collectors.groupingBy(this::getP));
    // If we have no permissions at all return an empty list
    if (combinedPermissions.values().size() == 0) {
      return new HashSet<>();
    }

    // If we do have permissions ... sort the grouped permissions (by PolicyIdStringWithMaskName)
    // on PolicyMask, extracting the first value of the sorted list into the final
    // permissions list
    HashSet<Permission> finalPermissionsList = new HashSet<>();

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
    HashSet<Permission> p;
    try {
      p = this.getPermissionsList();
    } catch (NullPointerException e) {
      log.error(format("Can't get permissions for user '%s'", getName()));
      p = new HashSet<>();
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

  public void addNewApplication(@NonNull Application app) {
    initApplications();
    this.applications.add(app);
  }

  public void addNewGroup(@NonNull Group g) {
    initGroups();
    this.groups.add(g);
  }

  public void addNewPermission(@NonNull Policy policy, @NonNull AccessLevel accessLevel) {
    initPermissions();
    val permission =
        UserPermission.builder().policy(policy).accessLevel(accessLevel).owner(this).build();
    this.userPermissions.add(permission);
  }

  public void removeApplication(@NonNull UUID appId) {
    if (this.applications == null) return;
    this.applications.removeIf(a -> a.id.equals(appId));
  }

  public void removeGroup(@NonNull UUID grpId) {
    if (this.groups == null) return;
    this.groups.removeIf(g -> g.getId().equals(grpId));
  }

  public void removePermission(@NonNull UUID permissionId) {
    if (this.userPermissions == null) return;
    this.userPermissions.removeIf(p -> p.id.equals(permissionId));
  }

  protected void initApplications() {
    if (this.applications == null) {
      this.applications = new HashSet<>();
    }
  }

  protected void initGroups() {
    if (this.groups == null) {
      this.groups = new HashSet<Group>();
    }
  }

  protected void initPermissions() {
    if (this.userPermissions == null) {
      this.userPermissions = new ArrayList<>();
    }
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
