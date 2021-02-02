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

package bio.overture.ego.controller;

import static bio.overture.ego.controller.resolver.PageableResolver.LIMIT;
import static bio.overture.ego.controller.resolver.PageableResolver.OFFSET;
import static bio.overture.ego.controller.resolver.PageableResolver.SORT;
import static bio.overture.ego.controller.resolver.PageableResolver.SORTORDER;
import static org.springframework.util.StringUtils.isEmpty;

import bio.overture.ego.model.dto.*;
import bio.overture.ego.model.entity.*;
import bio.overture.ego.model.enums.Fields;
import bio.overture.ego.model.search.Filters;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.security.AdminScoped;
import bio.overture.ego.service.*;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Slf4j
@RestController
@RequestMapping("/users")
@Api(tags = "Users")
public class UserController {

  /** Dependencies */
  private final UserService userService;

  private final GroupService groupService;
  private final ApplicationService applicationService;
  private final UserPermissionService userPermissionService;

  @Autowired
  public UserController(
      @NonNull UserService userService,
      @NonNull GroupService groupService,
      @NonNull UserPermissionService userPermissionService,
      @NonNull ApplicationService applicationService) {
    this.userService = userService;
    this.groupService = groupService;
    this.applicationService = applicationService;
    this.userPermissionService = userPermissionService;
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "")
  @ApiImplicitParams({
    @ApiImplicitParam(
        name = Fields.ID,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Search for ids containing this text"),
    @ApiImplicitParam(
        name = LIMIT,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Number of results to retrieve"),
    @ApiImplicitParam(
        name = OFFSET,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Index of first result to retrieve"),
    @ApiImplicitParam(
        name = SORT,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Field to sort on"),
    @ApiImplicitParam(
        name = SORTORDER,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Sorting order: ASC|DESC. Default order: DESC"),
  })
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Page Users")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<User> listUsers(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @ApiParam(
              value = "Query string compares to Users Email, First Name, and Last Name fields.",
              required = false)
          @RequestParam(value = "query", required = false)
          String query,
      @ApiIgnore @Filters List<SearchFilter> filters,
      @ApiIgnore Pageable pageable) {
    if (isEmpty(query)) {
      return new PageDTO<>(userService.listUsers(filters, pageable));
    } else {
      return new PageDTO<>(userService.findUsers(query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "User Details", response = User.class)})
  @JsonView(Views.REST.class)
  public @ResponseBody User getUser(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    return userService.getById(id);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.PUT, value = "/{id}")
  @ApiResponses(
      value = {
        @ApiResponse(
            code = 200,
            message = "Partially update using non-null user info",
            response = User.class)
      })
  public @ResponseBody User updateUser(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestBody(required = true) UpdateUserRequest updateUserRequest) {
    return userService.partialUpdate(id, updateUserRequest);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteUser(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    userService.delete(id);
  }

  /*
  Permissions related endpoints
   */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/permissions")
  @ApiImplicitParams({
    @ApiImplicitParam(
        name = LIMIT,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Number of results to retrieve"),
    @ApiImplicitParam(
        name = OFFSET,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Index of first result to retrieve"),
    @ApiImplicitParam(
        name = SORT,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Field to sort on"),
    @ApiImplicitParam(
        name = SORTORDER,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Sorting order: ASC|DESC. Default order: DESC"),
  })
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Page User Permissions for a User")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<UserPermission> getPermissions(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @ApiIgnore Pageable pageable) {
    return new PageDTO<>(userPermissionService.getPermissions(id, pageable));
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/permissions")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Add user permissions", response = User.class)})
  public @ResponseBody User addPermissions(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestBody(required = true) List<PermissionRequest> permissions) {
    return userPermissionService.addPermissions(id, permissions);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/permissions/{permissionIds}")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Delete User permissions")})
  @ResponseStatus(value = HttpStatus.OK)
  public void deletePermissions(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @PathVariable(value = "permissionIds", required = true) List<UUID> permissionIds) {
    userPermissionService.deletePermissions(id, permissionIds);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/groups/permissions")
  @ApiResponses(
      value = {
        @ApiResponse(
            code = 200,
            message = "Get effective permissions for a user with user and group permissions")
      })
  @ResponseStatus(value = HttpStatus.OK)
  public @ResponseBody Collection<ResolvedPermissionResponse> getResolvedPermissions(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    return userPermissionService.getResolvedPermissions(id);
  }

  /*
  Groups related endpoints
   */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/groups")
  @ApiImplicitParams({
    @ApiImplicitParam(
        name = Fields.ID,
        required = true,
        dataType = "string",
        paramType = "path",
        value = "Search for ids containing this text"),
    @ApiImplicitParam(
        name = LIMIT,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Number of results to retrieve"),
    @ApiImplicitParam(
        name = OFFSET,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Index of first result to retrieve"),
    @ApiImplicitParam(
        name = SORT,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Field to sort on"),
    @ApiImplicitParam(
        name = SORTORDER,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Sorting order: ASC|DESC. Default order: DESC"),
  })
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Page Groups for a User")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<Group> getGroupsFromUser(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestParam(value = "query", required = false) String query,
      @ApiIgnore @Filters List<SearchFilter> filters,
      @ApiIgnore Pageable pageable) {
    if (isEmpty(query)) {
      return new PageDTO<>(groupService.findGroupsForUser(id, filters, pageable));
    } else {
      return new PageDTO<>(groupService.findGroupsForUser(id, query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/groups")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Add Groups to user", response = User.class)})
  public @ResponseBody User addGroupsToUser(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestBody(required = true) List<UUID> groupIds) {
    return userService.associateGroupsWithUser(id, groupIds);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/groups/{groupIDs}")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Delete Groups from User")})
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteGroupsFromUser(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @PathVariable(value = "groupIDs", required = true) List<UUID> groupIds) {
    userService.disassociateGroupsFromUser(id, groupIds);
  }

  /*
  Applications related endpoints
   */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/applications")
  @ApiImplicitParams({
    @ApiImplicitParam(
        name = Fields.ID,
        required = true,
        dataType = "string",
        paramType = "path",
        value = "Search for ids containing this text"),
    @ApiImplicitParam(
        name = LIMIT,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Number of results to retrieve"),
    @ApiImplicitParam(
        name = OFFSET,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Index of first result to retrieve"),
    @ApiImplicitParam(
        name = SORT,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Field to sort on"),
    @ApiImplicitParam(
        name = SORTORDER,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Sorting order: ASC|DESC. Default order: DESC"),
  })
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Page Applications for a User")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<Application> getApplicationsFromUser(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestParam(value = "query", required = false) String query,
      @ApiIgnore @Filters List<SearchFilter> filters,
      @ApiIgnore Pageable pageable) {
    if (isEmpty(query)) {
      return new PageDTO<>(applicationService.findApplicationsForUser(id, filters, pageable));
    } else {
      return new PageDTO<>(
          applicationService.findApplicationsForUser(id, query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/applications")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Add Applications to User", response = User.class)
      })
  public @ResponseBody User addApplicationsToUser(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestBody(required = true) List<UUID> applicationIds) {
    return userService.associateApplicationsWithUser(id, applicationIds);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/applications/{applicationIds}")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Delete Applications from User")})
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteApplicationsFromUser(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @PathVariable(value = "applicationIds", required = true) List<UUID> applicationIds) {
    userService.disassociateApplicationsFromUser(id, applicationIds);
  }
}
