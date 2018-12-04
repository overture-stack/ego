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

import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import bio.overture.ego.model.enums.Fields;

import javax.persistence.*;
import java.util.*;

@Data
@Builder
@ToString(exclude = { "wholeUsers", "wholeApplications", "groupPermissions" })
@Table(name = "egogroup")
@Entity
@JsonPropertyOrder({ "id", "name", "description", "status", "wholeApplications", "groupPermissions" })
@JsonInclude()
@EqualsAndHashCode(of = { "id" })
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@JsonView(Views.REST.class)
public class Group implements PolicyOwner {
  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinColumn(name = Fields.OWNER)
  @JsonIgnore
  protected Set<Policy> policies;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinColumn(name = Fields.GROUPID_JOIN)
  @JsonIgnore
  protected List<GroupPermission> groupPermissions;

  @Id
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GenericGenerator(
    name = "group_uuid",
    strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "group_uuid")
  UUID id;
  @Column(nullable = false, name = Fields.NAME)
  @NonNull
  String name;
  @Column(nullable = false, name = Fields.DESCRIPTION)
  String description;
  @Column(nullable = false, name = Fields.STATUS)
  String status;
  @ManyToMany(targetEntity = Application.class)
  @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinTable(name = "groupapplication", joinColumns = { @JoinColumn(name = Fields.GROUPID_JOIN) },
    inverseJoinColumns = { @JoinColumn(name = Fields.APPID_JOIN) })
  @JsonIgnore
  Set<Application> wholeApplications;
  @ManyToMany()
  @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinTable(name = "usergroup", joinColumns = { @JoinColumn(name = Fields.GROUPID_JOIN) },
    inverseJoinColumns = { @JoinColumn(name = Fields.USERID_JOIN) })
  @JsonIgnore
  Set<User> wholeUsers;

  public void addApplication(@NonNull Application app) {
    initApplications();
    this.wholeApplications.add(app);
  }

  public void addUser(@NonNull User u) {
    initUsers();
    this.wholeUsers.add(u);
  }

  public void addNewPermission(@NonNull Policy policy, @NonNull AccessLevel mask) {
    initPermissions();
    val permission = GroupPermission.builder()
      .policy(policy)
      .accessLevel(mask)
      .owner(this)
      .build();
    this.groupPermissions.add(permission);
  }

  public void removeApplication(@NonNull UUID appId) {
    this.wholeApplications.removeIf(a -> a.id.equals(appId));
  }

  public void removeUser(@NonNull UUID userId) {
    if (this.wholeUsers == null)
      return;
    this.wholeUsers.removeIf(u -> u.id.equals(userId));
  }

  public void removePermission(@NonNull UUID permissionId) {
    if (this.groupPermissions == null)
      return;
    this.groupPermissions.removeIf(p -> p.id.equals(permissionId));
  }

  protected void initPermissions() {
    if (this.groupPermissions == null) {
      this.groupPermissions = new ArrayList<>();
    }
  }

  public Group update(Group other) {
    val builder = Group.builder()
      .id(other.getId())
      .name(other.getName())
      .description(other.getDescription())
      .status(other.getStatus());


    // Do not update ID, that is programmatic.

    // Update Users and Applications only if provided (not null)
    if (other.wholeApplications != null) {
      builder.wholeApplications(other.getWholeApplications());
    } else {
      builder.wholeApplications(this.getWholeApplications());
    }

    if (other.wholeUsers != null) {
      builder.wholeUsers(other.getWholeUsers());
    } else {
      builder.wholeUsers(this.getWholeUsers());
    }

    return builder.build();
  }

  private void initApplications() {
    if (this.wholeApplications == null) {
      this.wholeApplications = new HashSet<>();
    }
  }

  private void initUsers() {
    if (this.wholeUsers == null) {
      this.wholeUsers = new HashSet<>();
    }
  }

}

