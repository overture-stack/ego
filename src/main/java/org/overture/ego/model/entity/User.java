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
import java.util.stream.Collectors;

@Entity
@Table(name = "egouser")
@Data
@ToString(exclude={"groups","applications"})
@JsonPropertyOrder({"id", "name", "email", "role", "status", "groups",
    "applications", "firstName", "lastName", "createdAt", "lastLogin", "preferredLanguage"})
@JsonInclude(JsonInclude.Include.ALWAYS)
@EqualsAndHashCode(of={"id"})
@NoArgsConstructor
public class User {

  @Id
  @Column(nullable = false, name = Fields.ID, updatable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  int id;

  @NonNull
  @Column(nullable = false, name = Fields.NAME)
  String name;

  @NonNull
  @Column(nullable = false, name = Fields.EMAIL)
  String email;

  @NonNull
  @Column(nullable = false, name = Fields.ROLE)
  String role;

  @Column(name = Fields.STATUS)
  String status;

  @Column(name = Fields.FIRSTNAME)
  String firstName;

  @Column(name = Fields.LASTNAME)
  String lastName;

  @Column(name = Fields.CREATEDAT)
  String createdAt;

  @Column(name = Fields.LASTLOGIN)
  String lastLogin;

  @Column(name = Fields.PREFERREDLANGUAGE)
  String preferredLanguage;

  @ManyToMany(targetEntity = Group.class, cascade = {CascadeType.ALL})
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinTable(name = "usergroup", joinColumns = { @JoinColumn(name = Fields.USERID_JOIN) },
          inverseJoinColumns = { @JoinColumn(name = Fields.GROUPID_JOIN) })
  @JsonIgnore protected Set<Group> groups;

  @ManyToMany(targetEntity = Application.class, cascade = {CascadeType.ALL})
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinTable(name = "userapplication", joinColumns = { @JoinColumn(name = Fields.USERID_JOIN) },
          inverseJoinColumns = { @JoinColumn(name = Fields.APPID_JOIN) })
  @JsonIgnore protected Set<Application> applications;

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
      this.applications = new HashSet<Application>();
    }
  }

  protected void initGroups(){
    if(this.groups == null) {
      this.groups = new HashSet<Group>();
    }
  }

}
