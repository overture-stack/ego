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

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.overture.ego.model.dto.PageDTO;
import org.overture.ego.model.entity.Application;
import org.overture.ego.model.entity.Group;
import org.overture.ego.model.entity.User;
import org.overture.ego.model.search.Filters;
import org.overture.ego.model.search.SearchFilter;
import org.overture.ego.security.AdminScoped;
import org.overture.ego.service.ApplicationService;
import org.overture.ego.service.GroupService;
import org.overture.ego.service.UserService;
import org.overture.ego.view.Views;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
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
  @ApiImplicitParams({
          @ApiImplicitParam(name = "limit", dataType = "string", paramType = "query",
            value = "Results to retrieve"),
          @ApiImplicitParam(name = "offset", dataType = "string", paramType = "query",
            value = "Index of first result to retrieve"),
          @ApiImplicitParam(name = "sort", dataType = "string", paramType = "query",
                  value = "Field to sort on"),
          @ApiImplicitParam(name = "sortOrder", dataType = "string", paramType = "query",
                  value = "Sorting order: ASC|DESC. Default order: DESC"),
          @ApiImplicitParam(name = "status", dataType = "string", paramType = "query",
                  value = "Filter by status. " +
                          "You could also specify filters on any field of the entity being queried as " +
                          "query parameters in this format: name=something")

  })
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Page of Users", response = PageDTO.class)
      }
  )
  @JsonView(Views.REST.class)
  public @ResponseBody
  PageDTO<User> getUsersList(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @ApiParam(value="Query string compares to Users Name, Email, First Name, and Last Name fields.", required=false ) @RequestParam(value = "query", required = false) String query,
          @ApiIgnore @Filters List<SearchFilter> filters,
          Pageable pageable)
  {
    if(StringUtils.isEmpty(query)) {
      return new PageDTO<>(userService.listUsers(filters, pageable));
    } else {
      return new PageDTO<>(userService.findUsers(query, filters, pageable));
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
  @JsonView(Views.REST.class)
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
  @ApiImplicitParams({
          @ApiImplicitParam(name = "limit", dataType = "string", paramType = "query",
                  value = "Results to retrieve"),
          @ApiImplicitParam(name = "offset", dataType = "string", paramType = "query",
                  value = "Index of first result to retrieve"),
          @ApiImplicitParam(name = "sort", dataType = "string", paramType = "query",
                  value = "Field to sort on"),
          @ApiImplicitParam(name = "sortOrder", dataType = "string", paramType = "query",
                  value = "Sorting order: ASC|DESC. Default order: DESC"),
          @ApiImplicitParam(name = "status", dataType = "string", paramType = "query",
                  value = "Filter by status. " +
                          "You could also specify filters on any field of the entity being queried as " +
                          "query parameters in this format: name=something")

  })
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Page of Groups of user", response = PageDTO.class)
          }
  )
  @JsonView(Views.REST.class)
  public @ResponseBody
  PageDTO<Group> getUsersGroups(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String userId,
          @RequestParam(value = "query", required = false) String query,
          @ApiIgnore @Filters List<SearchFilter> filters,
          Pageable pageable)
  {
    if(StringUtils.isEmpty(query)) {
      return new PageDTO<>(groupService.findUsersGroup(userId, filters, pageable));
    } else {
      return new PageDTO<>(groupService.findUsersGroup(userId, query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/groups")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Add Groups to user", response = String.class)
          }
  )
  public @ResponseBody
  String addGroupsToUser(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String userId,
          @RequestBody(required = true) List<String> groupIDs) {
    userService.addUsersToGroups(userId,groupIDs);
    return "User added to : "+groupIDs.size() + " Group(s) successfully.";
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
  @ApiImplicitParams({
          @ApiImplicitParam(name = "limit", dataType = "string", paramType = "query",
            value = "Results to retrieve"),
          @ApiImplicitParam(name = "offset", dataType = "string", paramType = "query",
            value = "Index of first result to retrieve"),
          @ApiImplicitParam(name = "sort", dataType = "string", paramType = "query",
                  value = "Field to sort on"),
          @ApiImplicitParam(name = "sortOrder", dataType = "string", paramType = "query",
                  value = "Sorting order: ASC|DESC. Default order: DESC"),
          @ApiImplicitParam(name = "status", dataType = "string", paramType = "query",
                  value = "Filter by status. " +
                          "You could also specify filters on any field of the entity being queried as " +
                          "query parameters in this format: name=something")

  })
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Page of apps of user", response = PageDTO.class)
          }
  )
  @JsonView(Views.REST.class)
  public @ResponseBody
  PageDTO<Application> getUsersApplications(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String userId,
          @RequestParam(value = "query", required = false) String query,
          @ApiIgnore @Filters List<SearchFilter> filters,
          Pageable pageable)
  {
    if(StringUtils.isEmpty(query)) {
      return new PageDTO<>(applicationService.findUsersApps(userId, filters, pageable));
    } else {
      return new PageDTO<>(applicationService.findUsersApps(userId, query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/applications")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Add Applications to user", response = String.class)
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

  @ExceptionHandler({ EntityNotFoundException.class })
  public ResponseEntity<Object> handleEntityNotFoundException(HttpServletRequest req, EntityNotFoundException ex) {
    log.error("User ID not found.");
    return new ResponseEntity<Object>("Invalid User ID provided.", new HttpHeaders(),
        HttpStatus.BAD_REQUEST);
  }
}
