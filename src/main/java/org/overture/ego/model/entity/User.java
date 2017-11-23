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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "egouser")
@Data
@JsonPropertyOrder({"id", "name", "email", "role", "status", "groups",
    "applications", "first_name", "last_name", "created_at", "last_login", "preferred_language"})
@JsonInclude(JsonInclude.Include.ALWAYS)
@EqualsAndHashCode(of={"id"})
@NoArgsConstructor
public class User {

  @Id
  @Column(nullable = false, name = "id", updatable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  int id;

  @NonNull
  @Column(nullable = false, name = "name")
  String name;

  @NonNull
  @Column(nullable = false, name = "email")
  String email;

  @NonNull
  @Column(nullable = false, name = "role")
  String role;

  @Column(name = "status")
  String status;

  @Column(name = "firstname")
  @JsonProperty("first_name")
  String firstName;

  @Column(name = "lastname")
  @JsonProperty("last_name")
  String lastName;

  @Column(name = "createdat")
  @JsonProperty("created_at")
  String createdAt;

  @Column(name = "lastlogin")
  @JsonProperty("last_login")
  String lastLogin;

  @Column(name = "preferredlanguage")
  @JsonProperty("preferred_language")
  String preferredLanguage;

  @ManyToMany(targetEntity = Group.class, cascade = {CascadeType.ALL})
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinTable(name = "usergroup", joinColumns = { @JoinColumn(name = "userid") },
          inverseJoinColumns = { @JoinColumn(name = "grpid") })
  @JsonIgnore protected List<Group> groups;

  @ManyToMany(targetEntity = Application.class, cascade = {CascadeType.ALL})
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinTable(name = "userapplication", joinColumns = { @JoinColumn(name = "userid") },
          inverseJoinColumns = { @JoinColumn(name = "appid") })
  @JsonIgnore protected List<Application> applications;

  @JsonIgnore
  public List<String> getGroupNames(){
    if(this.groups == null) {
      return new ArrayList<String>();
    }
    return this.groups.stream().map(g -> g.getName()).collect(Collectors.toList());
  }

  @JsonIgnore
  public List<String> getApplicationNames(){
    if(this.applications == null){
      return new ArrayList<String>();
    }
    return this.applications.stream().map(a -> a.getName()).collect(Collectors.toList());
  }

  public void addNewApplication(@NonNull Application app){
    initApplications();
    this.applications.add(app);
  }

  public void addNewGroup(@NonNull Group g){
    initGroups();
    this.groups.add(g);
  }

  public void removeApplication(@NonNull Integer appId){
    if(this.applications == null) return;
    this.applications.removeIf(a -> a.id == appId);
  }

  public void removeGroup(@NonNull Integer grpId){
    if(this.groups == null) return;
    this.groups.removeIf(g -> g.id == grpId);
  }

  protected void initApplications(){
    if(this.applications == null){
      this.applications = new ArrayList<Application>();
    }
  }

  protected void initGroups(){
    if(this.groups == null) {
      this.groups = new ArrayList<Group>();
    }
  }

}
