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

import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static bio.overture.ego.utils.HibernateSessions.*;
import static bio.overture.ego.utils.PolicyPermissionUtils.extractPermissionStrings;
import static java.lang.String.format;

import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.enums.Fields;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Slf4j
@Entity
@Table(name = "egouser")
@Data
@ToString(exclude = {"wholeGroups", "wholeApplications", "userPermissions"})
@JsonPropertyOrder({
  "id",
  "name",
  "email",
  "role",
  "status",
  "wholeGroups",
  "wholeApplications",
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
      name = "usergroup",
      joinColumns = {@JoinColumn(name = Fields.USERID_JOIN)},
      inverseJoinColumns = {@JoinColumn(name = Fields.GROUPID_JOIN)})
  @JsonIgnore
  protected Set<Group> wholeGroups;

  @ManyToMany(targetEntity = Application.class)
  @Cascade(org.hibernate.annotations.CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinTable(
      name = "userapplication",
      joinColumns = {@JoinColumn(name = Fields.USERID_JOIN)},
      inverseJoinColumns = {@JoinColumn(name = Fields.APPID_JOIN)})
  @JsonIgnore
  protected Set<Application> wholeApplications;

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

  // Creates groups in JWTAccessToken::context::user
  @JsonView(Views.JWTAccessToken.class)
  public List<String> getGroups() {
    if (this.wholeGroups == null) {
      return new ArrayList<String>();
    }
    return this.wholeGroups.stream().map(g -> g.getName()).collect(Collectors.toList());
  }

  @JsonIgnore
  public List<Permission> getPermissionsList() {
    // Get user's individual permission (stream)
    val userPermissions =
        Optional.ofNullable(this.getUserPermissions()).orElse(new ArrayList<>()).stream();

    // Get permissions from the user's groups (stream)
    val userGroupsPermissions =
        Optional.ofNullable(this.getWholeGroups())
            .orElse(new HashSet<>())
            .stream()
            .map(Group::getGroupPermissions)
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

  @JsonIgnore
  public List<String> getApplications() {
    if (this.wholeApplications == null) {
      return new ArrayList<>();
    }
    return this.wholeApplications.stream().map(a -> a.getName()).collect(Collectors.toList());
  }

  @JsonView(Views.JWTAccessToken.class)
  public List<String> getRoles() {
    return Arrays.asList(this.getRole());
  }

  /*
   Roles is an array only in JWT but a String in Database.
   This is done for future compatibility - at the moment ego only needs one Role but this may change
    as project progresses.
    Currently, using the only role by extracting first role in the array
  */
  public void setRoles(@NonNull List<String> roles) {
    if (roles.size() > 0) this.role = roles.get(0);
  }

  public void addNewApplication(@NonNull Application app) {
    initApplications();
    this.wholeApplications.add(app);
  }

  public void addNewGroup(@NonNull Group g) {
    initGroups();
    this.wholeGroups.add(g);
  }

  public void addNewPermission(@NonNull Policy policy, @NonNull AccessLevel accessLevel) {
    initPermissions();
    val permission =
        UserPermission.builder().policy(policy).accessLevel(accessLevel).owner(this).build();
    this.userPermissions.add(permission);
  }

  public void removeApplication(@NonNull UUID appId) {
    if (this.wholeApplications == null) return;
    this.wholeApplications.removeIf(a -> a.id.equals(appId));
  }

  public void removeGroup(@NonNull UUID grpId) {
    if (this.wholeGroups == null) return;
    this.wholeGroups.removeIf(g -> g.id.equals(grpId));
  }

  public void removePermission(@NonNull UUID permissionId) {
    if (this.userPermissions == null) return;
    this.userPermissions.removeIf(p -> p.id.equals(permissionId));
  }

  protected void initApplications() {
    if (this.wholeApplications == null) {
      this.wholeApplications = new HashSet<>();
    }
  }

  protected void initGroups() {
    if (this.wholeGroups == null) {
      this.wholeGroups = new HashSet<Group>();
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

    // Don't merge wholeGroups, wholeApplications or userPermissions if not present in other
    // This is because the PUT action for update usually does not include these fields
    // as a consequence of the GET option to retrieve a user not including these fields
    // To clear wholeApplications, wholeGroups or userPermissions, use the dedicated services
    // for deleting associations or pass in an empty Set.
    if (other.wholeApplications != null) {
      unsetSession(other.getWholeApplications());
      this.wholeApplications = other.getWholeApplications();
    }

    if (other.wholeGroups != null) {
      unsetSession(other.getWholeGroups());
      this.wholeGroups = other.getWholeGroups();
    }

    if (other.userPermissions != null) {
      unsetSession(other.getUserPermissions());
      this.userPermissions = other.getUserPermissions();
    }
  }
}
