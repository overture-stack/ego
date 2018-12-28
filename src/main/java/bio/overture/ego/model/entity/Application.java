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

import static bio.overture.ego.utils.Collectors.toImmutableList;
import static bio.overture.ego.utils.Converters.nullToEmptySet;

import bio.overture.ego.model.enums.Fields;
import bio.overture.ego.model.enums.Tables;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
@Builder
@Table(name = "egoapplication")
@Data
@Accessors(chain = true)
@ToString(exclude = {"groups", "users"})
@JsonPropertyOrder({
  "id",
  "name",
  "clientId",
  "clientSecret",
  "redirectUri",
  "description",
  "status"
})
@JsonInclude(JsonInclude.Include.CUSTOM)
@EqualsAndHashCode(of = {"id"})
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
@JsonView(Views.REST.class)
public class Application implements Identifiable<UUID> {

  @Id
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GenericGenerator(name = "application_uuid", strategy = "org.hibernate.id.UUIDGenerator")
  @GeneratedValue(generator = "application_uuid")
  UUID id;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @NonNull
  @Column(nullable = false, name = Fields.NAME)
  String name;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @NonNull
  @Column(nullable = false, name = Fields.CLIENTID)
  String clientId;

  @NonNull
  @Column(nullable = false, name = Fields.CLIENTSECRET)
  String clientSecret;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = Fields.REDIRECTURI)
  String redirectUri;

  @JsonView({Views.JWTAccessToken.class, Views.REST.class})
  @Column(name = Fields.DESCRIPTION)
  String description;

  @JsonView(Views.JWTAccessToken.class)
  @Column(name = Fields.STATUS)
  String status;

  @ManyToMany()
  @Cascade(org.hibernate.annotations.CascadeType.SAVE_UPDATE)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinTable(
      name = Tables.GROUP_APPLICATION,
      joinColumns = {@JoinColumn(name = Fields.APPID_JOIN)},
      inverseJoinColumns = {@JoinColumn(name = Fields.GROUPID_JOIN)})
  @JsonIgnore
  Set<Group> groups;

  @JsonIgnore
  @ManyToMany(
      mappedBy = Fields.APPLICATIONS,
      fetch = FetchType.LAZY,
      cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  Set<User> users;

  @JsonIgnore
  public HashSet<String> getURISet() {
    val output = new HashSet<String>();
    output.add(this.redirectUri);
    return output;
  }

  @JsonView(Views.JWTAccessToken.class)
  public List<String> getGroupNames() {
    return getGroups().stream().map(Group::getName).collect(toImmutableList());
  }

  public void update(Application other) {
    this.name = other.name;
    this.clientId = other.clientId;
    this.clientSecret = other.clientSecret;
    this.redirectUri = other.redirectUri;
    this.description = other.description;
    this.status = other.status;

    // Do not update ID;

    // Update Users and Groups only if provided (not null)
    if (other.users != null) {
      this.users = other.users;
    }

    if (other.groups != null) {
      this.groups = other.groups;
    }
  }

  @JsonIgnore
  public Set<User> getUsers() {
    users = nullToEmptySet(users);
    return users;
  }

  @JsonIgnore
  public Set<Group> getGroups() {
    groups = nullToEmptySet(groups);
    return groups;
  }
}
