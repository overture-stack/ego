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
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.overture.ego.model.enums.Fields;
import org.overture.ego.view.Views;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "egoapplication")
@Data
@ToString(exclude={"wholeGroups","wholeUsers"})
@JsonPropertyOrder({"id", "name", "clientId", "clientSecret", "redirectUri", "description", "status"})
@JsonInclude(JsonInclude.Include.CUSTOM)
@EqualsAndHashCode(of={"id"})
@NoArgsConstructor
@RequiredArgsConstructor
@JsonView(Views.REST.class)
public class Application {

  @Id
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  int id;

  @JsonView({Views.JWTAccessToken.class,Views.REST.class})
  @NonNull
  @Column(nullable = false, name = Fields.NAME)
  String name;

  @JsonView({Views.JWTAccessToken.class,Views.REST.class})
  @NonNull
  @Column(nullable = false, name = Fields.CLIENTID)
  String clientId;

  @NonNull
  @Column(nullable = false, name = Fields.CLIENTSECRET)
  String clientSecret;

  @JsonView({Views.JWTAccessToken.class,Views.REST.class})
  @Column(name = Fields.REDIRECTURI)
  String redirectUri;

  @JsonView({Views.JWTAccessToken.class,Views.REST.class})
  @Column(name = Fields.DESCRIPTION)
  String description;

  @JsonView(Views.JWTAccessToken.class)
  @Column(name = Fields.STATUS)
  String status;

  @ManyToMany(mappedBy = "wholeApplications", cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JsonIgnore
  Set<Group> wholeGroups;

  @ManyToMany(mappedBy = "wholeApplications", cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JsonIgnore
  Set<User> wholeUsers;

  @JsonIgnore
  public HashSet<String> getURISet(){
    val output = new HashSet<String>();
    output.add(this.redirectUri);
    return output;
  }

  @JsonView(Views.JWTAccessToken.class)
  public List<String> getGroups(){
    if(this.wholeGroups == null) {
      return new ArrayList<String>();
    }
    return this.wholeGroups.stream().map(g -> g.getName()).collect(Collectors.toList());
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
    if (other.wholeUsers != null) {
      this.wholeUsers = other.wholeUsers;
    }

    if (other.wholeGroups != null) {
      this.wholeGroups = other.wholeGroups;
    }
  }


}
