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
@NoArgsConstructor
@EqualsAndHashCode(of={"id"})
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
  @JoinTable(name = "usergroup", joinColumns = { @JoinColumn(name = "userid") },
          inverseJoinColumns = { @JoinColumn(name = "grpid") })
  @JsonIgnore List<Group> groups;


  @ManyToMany(targetEntity = Application.class, cascade = {CascadeType.ALL})
  @JoinTable(name = "userapplication", joinColumns = { @JoinColumn(name = "userid") },
          inverseJoinColumns = { @JoinColumn(name = "appid") })
  @JsonIgnore List<Application> applications;

  public void removeApplication(Integer appId){
    if(this.applications == null) return;
    this.applications.removeIf(a -> a.id == appId);
  }

  public void removeGroup(Integer grpId){
    if(this.groups == null) return;
    this.groups.removeIf(g -> g.id == grpId);
  }

  public void addApplication(Application app){
    if(this.applications == null) {
      this.applications = new ArrayList<Application>();
    }
    this.applications.add(app);
  }

  public void addGroup(Group g){
    if(this.groups == null) {
      this.groups = new ArrayList<Group>();
    }
    this.groups.add(g);
  }

  @JsonIgnore
  public List<String> getGroupNames(){
    if(this.groups == null) {
      return null;
    }
    return this.groups.parallelStream().map(g -> g.getName()).collect(Collectors.toList());
  }

  @JsonIgnore
  public List<String> getApplicationNames(){
    if(this.applications == null){
      return null;
    }
    return this.applications.parallelStream().map(a -> a.getName()).collect(Collectors.toList());
  }

  public void setGroupNames(List<String> groupNames){
    if(groupNames == null) return;
    if(this.groups == null) {
      this.groups = new ArrayList<Group>();
    }
    groupNames.forEach(gName -> this.groups.add(new Group(gName)));
  }

  public void setApplicationNames(List<String> applicationNames){
    if(applicationNames == null) return;
    if(this.applications == null){
      this.applications = new ArrayList<Application>();
    }
    applicationNames.forEach(appName -> this.applications.add(new Application(appName,"","")));
  }

}
