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
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "egouser")
@Data
@ToString(exclude={"wholeGroups","wholeApplications"})
@JsonPropertyOrder({"id", "name", "email", "role", "status", "wholeGroups",
    "wholeApplications", "firstName", "lastName", "createdAt", "lastLogin", "preferredLanguage"})
@JsonInclude(JsonInclude.Include.ALWAYS)
@EqualsAndHashCode(of={"id"})
@NoArgsConstructor
@JsonView(Views.REST.class)
public class User {

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
  @Column(nullable = false, name = Fields.EMAIL)
  String email;

  @NonNull
  @Column(nullable = false, name = Fields.ROLE)
  String role;

  @JsonView({Views.JWTAccessToken.class,Views.REST.class})
  @Column(name = Fields.STATUS)
  String status;

  @JsonView({Views.JWTAccessToken.class,Views.REST.class})
  @Column(name = Fields.FIRSTNAME)
  String firstName;

  @JsonView({Views.JWTAccessToken.class,Views.REST.class})
  @Column(name = Fields.LASTNAME)
  String lastName;

  @JsonView({Views.JWTAccessToken.class,Views.REST.class})
  @Column(name = Fields.CREATEDAT)
  String createdAt;

  @JsonView({Views.JWTAccessToken.class,Views.REST.class})
  @Column(name = Fields.LASTLOGIN)
  String lastLogin;

  @JsonView({Views.JWTAccessToken.class,Views.REST.class})
  @Column(name = Fields.PREFERREDLANGUAGE)
  String preferredLanguage;

  @ManyToMany(targetEntity = Group.class, cascade = {CascadeType.ALL})
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinTable(name = "usergroup", joinColumns = { @JoinColumn(name = Fields.USERID_JOIN) },
          inverseJoinColumns = { @JoinColumn(name = Fields.GROUPID_JOIN) })
  @JsonIgnore protected Set<Group> wholeGroups;

  @ManyToMany(targetEntity = Application.class, cascade = {CascadeType.ALL})
  @LazyCollection(LazyCollectionOption.FALSE)
  @JoinTable(name = "userapplication", joinColumns = { @JoinColumn(name = Fields.USERID_JOIN) },
          inverseJoinColumns = { @JoinColumn(name = Fields.APPID_JOIN) })
  @JsonIgnore protected Set<Application> wholeApplications;

  @JsonView(Views.JWTAccessToken.class)
  public List<String> getGroups(){
    if(this.wholeGroups == null) {
      return new ArrayList<String>();
    }
    return this.wholeGroups.stream().map(g -> g.getName()).collect(Collectors.toList());
  }

  @JsonIgnore
  public List<String> getApplications(){
    if(this.wholeApplications == null){
      return new ArrayList<String>();
    }
    return this.wholeApplications.stream().map(a -> a.getName()).collect(Collectors.toList());
  }

  @JsonView(Views.JWTAccessToken.class)
  public List<String> getRoles(){
    return Arrays.asList(this.getRole());
  }

  /*
    Roles is an array only in JWT but a String in Database.
    This is done for future compatibility - at the moment ego only needs one Role but this may change
     as project progresses.
     Currently, using the only role by extracting first role in the array
   */
  public void setRoles(@NonNull List<String> roles){
    if(roles.size() > 0)
      this.role = roles.get(0);
  }

  public void addNewApplication(@NonNull Application app){
    initApplications();
    this.wholeApplications.add(app);
  }

  public void addNewGroup(@NonNull Group g){
    initGroups();
    this.wholeGroups.add(g);
  }

  public void removeApplication(@NonNull Integer appId){
    if(this.wholeApplications == null) return;
    this.wholeApplications.removeIf(a -> a.id == appId);
  }

  public void removeGroup(@NonNull Integer grpId){
    if(this.wholeGroups == null) return;
    this.wholeGroups.removeIf(g -> g.id == grpId);
  }

  protected void initApplications(){
    if(this.wholeApplications == null){
      this.wholeApplications = new HashSet<Application>();
    }
  }

  protected void initGroups(){
    if(this.wholeGroups == null) {
      this.wholeGroups = new HashSet<Group>();
    }
  }

  public void update(User other) {
    this.name = other.name;
    this.firstName = other.firstName;
    this.lastName = other.lastName;
    this.role = other.role;
    this.status = other.status;
    this.preferredLanguage = other.preferredLanguage;

    // Don't merge the ID, CreatedAt, or LastLogin date - those are procedural.

    // Don't merge wholeGroups or wholeApplications if not present in other
    //  This is because the PUT action for update usually does not include these fields
    //  as a consequence of the GET option to retrieve a user not including these fields
    // To clear wholeApplications and wholeGroups, use the dedicated services for deleting associations or pass in an empty Set.
    if (other.wholeApplications != null) {
      this.wholeApplications = other.wholeApplications;
    }

    if (other.wholeGroups != null) {
      this.wholeGroups = other.wholeGroups;
    }
  }

}
