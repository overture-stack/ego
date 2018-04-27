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
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
@RequestMapping("/groups")
public class GroupController {


  /**
   * Dependencies
   */
  @Autowired
  private GroupService groupService;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private UserService userService;

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "")
  @ApiImplicitParams({
          @ApiImplicitParam(name = "limit", dataType = "string", paramType = "query",
            value = "Number of results to retrieve"),
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
          @ApiResponse(code = 200, message = "Page of Groups", response = PageDTO.class)
      }
  )
  @JsonView(Views.REST.class)
  public @ResponseBody
  PageDTO<Group> getGroupsList(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @RequestParam(value = "query", required = false) String query,
          @ApiIgnore @Filters List<SearchFilter> filters,
          Pageable pageable) {
    if(StringUtils.isEmpty(query)) {
      return new PageDTO<>(groupService.listGroups(filters, pageable));
    } else {
      return new PageDTO<>(groupService.findGroups(query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "")
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "New Group", response = Group.class)
      }
  )
  public @ResponseBody
  Group createGroup(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @RequestBody(required = true) Group groupInfo) {
    return groupService.create(groupInfo);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}")
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Group Details", response = Group.class)
      }
  )
  @JsonView(Views.REST.class)
  public @ResponseBody
  Group getGroup(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String groupId) {
    return groupService.get(groupId);
  }


  @AdminScoped
  @RequestMapping(method = RequestMethod.PUT, value = "/{id}")
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Updated group info", response = Group.class)
      }
  )
  public @ResponseBody
  Group updateGroup(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @RequestBody(required = true) Group updatedGroupInfo) {
    return groupService.update(updatedGroupInfo);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteGroup(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String groupId) {
    groupService.delete(groupId);
  }

  /*
   Application related endpoints
    */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/applications")
  @ApiImplicitParams({
          @ApiImplicitParam(name = "limit", dataType = "string", paramType = "query",
            value = "Number of results to retrieve"),
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
                  @ApiResponse(code = 200, message = "Page of Applications of group", response = PageDTO.class)
          }
  )
  @JsonView(Views.REST.class)
  public @ResponseBody
  PageDTO<Application> getGroupsApplications(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String groupId,
          @RequestParam(value = "query", required = false) String query,
          @ApiIgnore @Filters List<SearchFilter> filters,
          Pageable pageable)
  {
    if(StringUtils.isEmpty(query)) {
      return new PageDTO<>(applicationService.findGroupsApplications(groupId, filters, pageable));
    } else {
      return new PageDTO<>(applicationService.findGroupsApplications(groupId, query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/applications")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Add Apps to Group", response = String.class)
          }
  )
  public @ResponseBody
  String addAppsToGroups(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String grpId,
          @RequestBody(required = true) List<String> apps) {
    groupService.addAppsToGroups(grpId,apps);
    return apps.size() + " apps added successfully.";
  }


  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/applications/{appIDs}")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Delete Apps from Group")
          }
  )
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteAppsFromGroup(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String grpId,
          @PathVariable(value = "appIDs", required = true) List<String> appIDs) {
    groupService.deleteAppsFromGroup(grpId,appIDs);
  }

  /*
   User related endpoints
    */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/users")
  @ApiImplicitParams({
          @ApiImplicitParam(name = "limit", dataType = "string", paramType = "query",
            value = "Number of results to retrieve"),
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
                  @ApiResponse(code = 200, message = "Page of Users of group", response = PageDTO.class)
          }
  )
  @JsonView(Views.REST.class)
  public @ResponseBody
  PageDTO<User> getGroupsUsers(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String groupId,
          @RequestParam(value = "query", required = false) String query,
          @ApiIgnore @Filters List<SearchFilter> filters,
          Pageable pageable)
  {
    if(StringUtils.isEmpty(query)) {
      return new PageDTO<>(userService.findGroupsUsers(groupId, filters, pageable));
    } else {
      return new PageDTO<>(userService.findGroupsUsers(groupId, query, filters, pageable));
    }
  }

  @ExceptionHandler({ EntityNotFoundException.class })
  public ResponseEntity<Object> handleEntityNotFoundException(HttpServletRequest req, EntityNotFoundException ex) {
    log.error("Group ID not found.");
    return new ResponseEntity<Object>("Invalid Group ID provided.", new HttpHeaders(),
        HttpStatus.BAD_REQUEST);
  }
}
