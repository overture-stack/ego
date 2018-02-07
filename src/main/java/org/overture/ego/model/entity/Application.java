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

package org.overture.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.overture.ego.model.enums.Fields;

import javax.persistence.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "egoapplication")
@Data
@ToString(exclude={"groups","users"})
@JsonPropertyOrder({"id", "name", "clientId", "clientSecret", "redirectUri", "description", "status"})
@JsonInclude(JsonInclude.Include.ALWAYS)
@EqualsAndHashCode(of={"id"})
@NoArgsConstructor
@RequiredArgsConstructor
public class Application {

  @Id
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  int id;

  @NonNull
  @Column(nullable = false, name = Fields.NAME)
  String name;

  @NonNull
  @Column(nullable = false, name = Fields.CLIENTID)
  String clientId;

  @NonNull
  @Column(nullable = false, name = Fields.CLIENTSECRET)
  String clientSecret;

  @Column(name = Fields.REDIRECTURI)
  String redirectUri;

  @Column(name = Fields.DESCRIPTION)
  String description;

  @Column(name = Fields.STATUS)
  String status;

  @ManyToMany(mappedBy = "applications", cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JsonIgnore
  Set<Group> groups;

  @ManyToMany(mappedBy = "applications", cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JsonIgnore
  Set<User> users;

  @JsonIgnore
  public HashSet<String> getURISet(){
    val output = new HashSet<String>();
    output.add(this.redirectUri);
    return output;
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


}
