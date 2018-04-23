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

package org.overture.ego.token.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.overture.ego.model.entity.Application;
import org.overture.ego.model.entity.Group;
import org.overture.ego.model.entity.User;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@JsonIgnoreProperties({ "id", "groupNames", "applicationNames", "role", "groups","applications" })
public class UserTokenInfo extends User {

  /*
    Variables
   */
  private List<String> roles;

  public UserTokenInfo(User u){
    this.setId(u.getId());
    this.setName(u.getName());
    this.addNewRole(u.getRole());
    this.setStatus(u.getStatus());
    this.setEmail(u.getEmail());
    this.setFirstName(u.getFirstName());
    this.setLastName(u.getLastName());
    this.setGroups(u.getGroups());
    this.setApplications(u.getApplications());
    this.setCreatedAt(u.getCreatedAt());
    this.setLastLogin(u.getLastLogin());
    this.setPreferredLanguage(u.getPreferredLanguage());
  }

  public void addNewRole(String role){
      initRoles();
      this.roles.add(role);
      this.setRole(role);
  }
  public void setGroupNames(@NonNull List<String> groupNames){
    initGroups();
    groupNames.forEach(gName -> this.groups.add(new Group(gName)));
  }

  public void setApplicationNames(@NonNull List<String> applicationNames){
    initApplications();
    applicationNames.forEach(appName -> this.applications.add(new Application(appName,"","")));
  }

  private void initRoles(){
    if(this.roles == null){
      this.roles = new ArrayList<String>();
    }
  }

}
