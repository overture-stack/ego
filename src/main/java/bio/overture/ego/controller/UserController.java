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

import static org.springframework.util.StringUtils.isEmpty;

import bio.overture.ego.model.dto.CreateUserRequest;
import bio.overture.ego.model.dto.PageDTO;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.dto.UpdateUserRequest;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.model.exceptions.PostWithIdentifierException;
import bio.overture.ego.model.search.Filters;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.security.AdminScoped;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.UserPermissionService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.UUID;
import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
        name = "limit",
        dataType = "string",
        paramType = "query",
        value = "Results to retrieve"),
    @ApiImplicitParam(
        name = "offset",
        dataType = "string",
        paramType = "query",
        value = "Index of first result to retrieve"),
    @ApiImplicitParam(
        name = "sort",
        dataType = "string",
        paramType = "query",
        value = "Field to sort on"),
    @ApiImplicitParam(
        name = "sortOrder",
        dataType = "string",
        paramType = "query",
        value = "Sorting order: ASC|DESC. Default order: DESC"),
    @ApiImplicitParam(
        name = "status",
        dataType = "string",
        paramType = "query",
        value =
            "Filter by status. "
                + "You could also specify filters on any field of the policy being queried as "
                + "query parameters in this format: name=something")
  })
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Page of Users", response = PageDTO.class)})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<User> getUsersList(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @ApiParam(
              value =
                  "Query string compares to Users Name, Email, First Name, and Last Name fields.",
              required = false)
          @RequestParam(value = "query", required = false)
          String query,
      @ApiIgnore @Filters List<SearchFilter> filters,
      Pageable pageable) {
    // TODO: [rtisma] create tests for this controller logic. This logic should remain in
    // controller.
    if (isEmpty(query)) {
      return new PageDTO<>(userService.listUsers(filters, pageable));
    } else {
      return new PageDTO<>(userService.findUsers(query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Create new user", response = User.class),
        @ApiResponse(
            code = 400,
            message = PostWithIdentifierException.reason,
            response = User.class)
      })
  public @ResponseBody User create(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @RequestBody(required = true) CreateUserRequest request) {
    return userService.create(request);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "User Details", response = User.class)})
  @JsonView(Views.REST.class)
  public @ResponseBody User getUser(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String id) {
    return userService.get(id);
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
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String id,
      @RequestBody(required = true) UpdateUserRequest updateUserRequest) {
    return userService.partialUpdate(id, updateUserRequest);
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
  Permissions related endpoints
   */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/permissions")
  @ApiImplicitParams({
    @ApiImplicitParam(
        name = "limit",
        dataType = "string",
        paramType = "query",
        value = "Results to retrieve"),
    @ApiImplicitParam(
        name = "offset",
        dataType = "string",
        paramType = "query",
        value = "Index of first result to retrieve"),
    @ApiImplicitParam(
        name = "sort",
        dataType = "string",
        paramType = "query",
        value = "Field to sort on"),
    @ApiImplicitParam(
        name = "sortOrder",
        dataType = "string",
        paramType = "query",
        value = "Sorting order: ASC|DESC. Default order: DESC")
  })
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Page of user permissions", response = PageDTO.class)
      })
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<UserPermission> getPermissions(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id,
      Pageable pageable) {
    return new PageDTO<>(userPermissionService.getPermissions(id, pageable));
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/permissions")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Add user permissions", response = User.class)})
  public @ResponseBody User addPermissions(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestBody(required = true) List<PermissionRequest> permissions) {
    return userPermissionService.addPermissions(id, permissions);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/permissions/{permissionIds}")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Delete user permissions")})
  @ResponseStatus(value = HttpStatus.OK)
  public void deletePermissions(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id,
      @PathVariable(value = "permissionIds", required = true) List<UUID> permissionIds) {
    userPermissionService.deletePermissions(id, permissionIds);
  }

  /*
  Groups related endpoints
   */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/groups")
  @ApiImplicitParams({
    @ApiImplicitParam(
        name = "limit",
        dataType = "string",
        paramType = "query",
        value = "Results to retrieve"),
    @ApiImplicitParam(
        name = "offset",
        dataType = "string",
        paramType = "query",
        value = "Index of first result to retrieve"),
    @ApiImplicitParam(
        name = "sort",
        dataType = "string",
        paramType = "query",
        value = "Field to sort on"),
    @ApiImplicitParam(
        name = "sortOrder",
        dataType = "string",
        paramType = "query",
        value = "Sorting order: ASC|DESC. Default order: DESC"),
    @ApiImplicitParam(
        name = "status",
        dataType = "string",
        paramType = "query",
        value =
            "Filter by status. "
                + "You could also specify filters on any field of the policy being queried as "
                + "query parameters in this format: name=something")
  })
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Page of Groups of user", response = PageDTO.class)
      })
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<Group> getUsersGroups(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String userId,
      @RequestParam(value = "query", required = false) String query,
      @ApiIgnore @Filters List<SearchFilter> filters,
      Pageable pageable) {
    // TODO: [rtisma] create tests for this controller logic. This logic should remain in
    // controller.
    if (isEmpty(query)) {
      return new PageDTO<>(groupService.findUserGroups(userId, filters, pageable));
    } else {
      return new PageDTO<>(groupService.findUserGroups(userId, query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/groups")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Add Groups to user", response = User.class)})
  public @ResponseBody User addGroupsToUser(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String userId,
      @RequestBody(required = true) List<String> groupIDs) {

    return userService.addUserToGroups(userId, groupIDs);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/groups/{groupIDs}")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Delete Groups from User")})
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteGroupFromUser(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String userId,
      @PathVariable(value = "groupIDs", required = true) List<String> groupIDs) {
    userService.deleteUserFromGroups(userId, groupIDs);
  }

  /*
  Applications related endpoints
   */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/applications")
  @ApiImplicitParams({
    @ApiImplicitParam(
        name = "limit",
        dataType = "string",
        paramType = "query",
        value = "Results to retrieve"),
    @ApiImplicitParam(
        name = "offset",
        dataType = "string",
        paramType = "query",
        value = "Index of first result to retrieve"),
    @ApiImplicitParam(
        name = "sort",
        dataType = "string",
        paramType = "query",
        value = "Field to sort on"),
    @ApiImplicitParam(
        name = "sortOrder",
        dataType = "string",
        paramType = "query",
        value = "Sorting order: ASC|DESC. Default order: DESC"),
    @ApiImplicitParam(
        name = "status",
        dataType = "string",
        paramType = "query",
        value =
            "Filter by status. "
                + "You could also specify filters on any field of the policy being queried as "
                + "query parameters in this format: name=something")
  })
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Page of apps of user", response = PageDTO.class)
      })
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<Application> getUsersApplications(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String userId,
      @RequestParam(value = "query", required = false) String query,
      @ApiIgnore @Filters List<SearchFilter> filters,
      Pageable pageable) {
    // TODO: [rtisma] create tests for this controller logic. This logic should remain in
    // controller.
    if (isEmpty(query)) {
      return new PageDTO<>(applicationService.findUserApps(userId, filters, pageable));
    } else {
      return new PageDTO<>(applicationService.findUserApps(userId, query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/applications")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Add Applications to user", response = User.class)
      })
  public @ResponseBody User addAppsToUser(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String userId,
      @RequestBody(required = true) List<String> appIDs) {
    return userService.addUserToApps(userId, appIDs);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/applications/{appIDs}")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Delete Applications from User")})
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteAppFromUser(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String userId,
      @PathVariable(value = "appIDs", required = true) List<String> appIDs) {
    userService.deleteUserFromApps(userId, appIDs);
  }

  @ExceptionHandler({EntityNotFoundException.class})
  public ResponseEntity<Object> handleEntityNotFoundException(
      HttpServletRequest req, EntityNotFoundException ex) {
    log.error("User ID not found.");
    return new ResponseEntity<Object>(
        "Invalid User ID provided.", new HttpHeaders(), HttpStatus.BAD_REQUEST);
  }
}
