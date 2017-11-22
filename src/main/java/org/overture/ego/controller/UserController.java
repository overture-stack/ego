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

package org.overture.ego.controller;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.overture.ego.model.entity.Application;
import org.overture.ego.model.entity.Group;
import org.overture.ego.model.entity.User;
import org.overture.ego.security.AdminScoped;
import org.overture.ego.service.ApplicationService;
import org.overture.ego.service.GroupService;
import org.overture.ego.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

  /**
   * Dependencies
   */
  @Autowired
  private UserService userService;
  @Autowired
  private GroupService groupService;
  @Autowired
  private ApplicationService applicationService;

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "")
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Page of users", response = Page.class)
      }
  )
  public @ResponseBody
  Page<User> getUsersList(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @RequestParam(value = "query", required = false) String query,
          Pageable pageable)
  {
    if(StringUtils.isEmpty(query)) {
      return userService.listUsers(pageable);
    } else {
      return userService.findUsers(query, pageable);
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Create new user", response = User.class)
          }
  )
  public @ResponseBody
  User create(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @RequestBody(required = true) User userInfo) {
    return userService.create(userInfo);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}")
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "User Details", response = User.class)
      }
  )
  public @ResponseBody
  User getUser(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String id) {
    return  userService.get(id);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.PUT, value = "/{id}")
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Updated user info", response = User.class)
      }
  )
  public @ResponseBody
  User updateUser(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @RequestBody(required = true) User updatedUserInfo) {
    return userService.update(updatedUserInfo);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteUser(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String userId) {
    userService.delete(userId);
  }

  /*
   Groups related endpoints
    */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/groups")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Page of groups of user", response = Page.class)
          }
  )
  public @ResponseBody
  Page<Group> getUsersGroups(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String userId,
          @RequestParam(value = "query", required = false) String query,
          Pageable pageable)
  {
    if(StringUtils.isEmpty(query)) {
      return groupService.findUsersGroup(userId,pageable);
    } else {
      return groupService.findUsersGroup(userId, query, pageable);
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/groups")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Add groups to user", response = String.class)
          }
  )
  public @ResponseBody
  String addGroupsToUser(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String userId,
          @RequestBody(required = true) List<String> groupIDs) {
    userService.addUsersToGroups(userId,groupIDs);
    return "User added to : "+groupIDs.size() + " groups successfully.";
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/groups/{groupIDs}")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Delete Groups from User")
          }
  )
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteGroupFromUser(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String userId,
          @PathVariable(value = "groupIDs", required = true) List<String> groupIDs) {
    userService.deleteUserFromGroup(userId,groupIDs);
  }

  /*
  Applications related endpoints
   */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/applications")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Page of apps of user", response = Page.class)
          }
  )
  public @ResponseBody
  Page<Application> getUsersApplications(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String userId,
          @RequestParam(value = "query", required = false) String query,
          Pageable pageable)
  {
    if(StringUtils.isEmpty(query)) {
      return applicationService.findUsersApps(userId,pageable);
    } else {
      return applicationService.findUsersApps(userId, query, pageable);
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/applications")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Add applications to user", response = String.class)
          }
  )
  public @ResponseBody
  String addAppsToUser(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String userId,
          @RequestBody(required = true) List<String> appIDs) {
    userService.addUsersToApps(userId,appIDs);
    return "User added to : "+appIDs.size() + " apps successfully.";
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/applications/{appIDs}")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Delete Applications from User")
          }
  )
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteAppFromUser(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String userId,
          @PathVariable(value = "appIDs", required = true) List<String> appIDs) {
    userService.deleteUserFromApp(userId,appIDs);
  }

}
