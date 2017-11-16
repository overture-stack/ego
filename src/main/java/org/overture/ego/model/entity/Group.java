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

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Data
@Table(name = "egogroup")
@Entity
@JsonPropertyOrder({"id", "name", "description", "status","applications"})
@JsonInclude(JsonInclude.Include.ALWAYS)
@EqualsAndHashCode(of={"id"})
@NoArgsConstructor
@RequiredArgsConstructor
public class Group {

  @Id
  @Column(nullable = false, name = "id", updatable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  int id;

  @Column(nullable = false, name = "name", updatable = false)
  @NonNull
  String name;

  @Column(nullable = false, name = "description", updatable = false)
  String description;

  @Column(nullable = false, name = "status", updatable = false)
  String status;

  @ManyToMany(targetEntity = Application.class, cascade = {CascadeType.ALL})
  @JoinTable(name = "groupapplication", joinColumns = { @JoinColumn(name = "grpid") },
          inverseJoinColumns = { @JoinColumn(name = "appid") })
  @JsonIgnore List<Application> applications;

  @ManyToMany(mappedBy = "groups", cascade = CascadeType.ALL)
  @JsonIgnore
  List<User> users;

  public void removeApplication(Integer appId){
    if(this.applications == null) return;
    this.applications.removeIf(a -> a.id == appId);
  }

  public void removeUser(Integer userId){
    if(this.users == null) return;
    this.users.removeIf(u -> u.id == userId);
  }

  public void addApplication(Application app){
    if(this.applications == null) {
      this.applications = new ArrayList<Application>();
    }
    this.applications.add(app);
  }

  public void addUser(User u){
    if(this.users == null) {
     this.users = new ArrayList<User>();
    }
    this.users.add(u);
  }
}

