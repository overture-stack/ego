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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@ToString(exclude={"users","applications"})
@Table(name = "egogroup")
@Entity
@JsonPropertyOrder({"id", "name", "description", "status","applications"})
@JsonInclude(JsonInclude.Include.ALWAYS)
@EqualsAndHashCode(of={"id"})
@NoArgsConstructor
@RequiredArgsConstructor
public class Group {

  @Id
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  int id;

  @Column(nullable = false, name = Fields.NAME, updatable = false)
  @NonNull
  String name;

  @Column(nullable = false, name = Fields.DESCRIPTION, updatable = false)
  String description;

  @Column(nullable = false, name = Fields.STATUS, updatable = false)
  String status;

  @ManyToMany(targetEntity = Application.class, cascade = {CascadeType.ALL})
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinTable(name = "groupapplication", joinColumns = { @JoinColumn(name = Fields.GROUPID_JOIN) },
          inverseJoinColumns = { @JoinColumn(name = Fields.APPID_JOIN) })
  @JsonIgnore Set<Application> applications;

  @ManyToMany(mappedBy = "groups", cascade = CascadeType.ALL)
  @LazyCollection(LazyCollectionOption.FALSE)
  @JsonIgnore
  Set<User> users;

  public void addApplication(@NonNull Application app){
    initApplications();
    this.applications.add(app);
  }

  public void addUser(@NonNull User u){
    initUsers();
    this.users.add(u);
  }

  public void removeApplication(@NonNull Integer appId){
    this.applications.removeIf(a -> a.id == appId);
  }

  public void removeUser(@NonNull Integer userId){
    if(this.users == null) return;
    this.users.removeIf(u -> u.id == userId);
  }

  public void update(Group other) {
    this.name = other.name;
    this.description = other.description;
    this.status = other.status;

    // Do not update ID, that is programmatic.

    // Update Users and Applications only if provided (not null)
    if (other.applications != null) {
      this.applications = other.applications;
    }

    if (other.users != null) {
      this.users = other.users;
    }
  }

  private void initApplications(){
    if(this.applications == null){
      this.applications = new HashSet<>();
    }
  }

  private void initUsers(){
    if(this.users == null) {
      this.users = new HashSet<>();
    }
  }


}

