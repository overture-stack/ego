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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Slf4j
@RestController
@RequestMapping("/users")
@Tag(name = "Users")
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
  @Parameters({
    @Parameter(
        name = Fields.ID,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Search for ids containing this text"),
    @Parameter(
        name = LIMIT,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Number of results to retrieve"),
    @Parameter(
        name = OFFSET,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Index of first result to retrieve"),
    @Parameter(
        name = SORT,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Field to sort on"),
    @Parameter(
        name = SORTORDER,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Sorting order: ASC|DESC. Default order: DESC"),
  })
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Page Users")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<User> listUsers(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @Parameter(
              description =
                  "Query string compares to Users Email, First Name, Last Name, Status and ProviderType fields.",
              required = false)
          @RequestParam(value = "query", required = false)
          String query,
      @Parameter(hidden = true) @Filters List<SearchFilter> filters,
      @Parameter(hidden = true) Pageable pageable) {
    if (isEmpty(query)) {
      return new PageDTO<>(userService.listUsers(filters, pageable));
    } else {
      return new PageDTO<>(userService.findUsers(query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "User Details")})
  @JsonView(Views.REST.class)
  public @ResponseBody User getUser(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    return userService.getById(id);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.PATCH, value = "/{id}")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Partially update using non-null user info")
      })
  public @ResponseBody User updateUser(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestBody(required = true) UpdateUserRequest updateUserRequest) {
    return userService.partialUpdate(id, updateUserRequest);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteUser(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    userService.delete(id);
  }

  /*
  Permissions related endpoints
   */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/permissions")
  @Parameters({
    @Parameter(
        name = LIMIT,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Number of results to retrieve"),
    @Parameter(
        name = OFFSET,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Index of first result to retrieve"),
    @Parameter(
        name = SORT,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Field to sort on"),
    @Parameter(
        name = SORTORDER,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Sorting order: ASC|DESC. Default order: DESC"),
  })
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Page User Permissions for a User")
      })
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<UserPermission> getPermissions(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @Parameter(hidden = true) Pageable pageable) {
    return new PageDTO<>(userPermissionService.getPermissions(id, pageable));
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/permissions")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Add user permissions")})
  public @ResponseBody User addPermissions(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestBody(required = true) List<PermissionRequest> permissions) {
    return userPermissionService.addPermissions(id, permissions);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/permissions/{permissionIds}")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Delete User permissions")})
  @ResponseStatus(value = HttpStatus.OK)
  public void deletePermissions(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
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
            responseCode = "200",
            description = "Get effective permissions for a user with user and group permissions")
      })
  @ResponseStatus(value = HttpStatus.OK)
  public @ResponseBody Collection<ResolvedPermissionResponse> getResolvedPermissions(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    return userPermissionService.getResolvedPermissions(id);
  }

  /*
  Groups related endpoints
   */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/groups")
  @Parameters({
    @Parameter(
        name = Fields.ID,
        required = true,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Search for ids containing this text"),
    @Parameter(
        name = LIMIT,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Number of results to retrieve"),
    @Parameter(
        name = OFFSET,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Index of first result to retrieve"),
    @Parameter(
        name = SORT,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Field to sort on"),
    @Parameter(
        name = SORTORDER,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Sorting order: ASC|DESC. Default order: DESC"),
  })
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Page Groups for a User")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<Group> getGroupsFromUser(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestParam(value = "query", required = false) String query,
      @Parameter(hidden = true) @Filters List<SearchFilter> filters,
      @Parameter(hidden = true) Pageable pageable) {
    if (isEmpty(query)) {
      return new PageDTO<>(groupService.findGroupsForUser(id, filters, pageable));
    } else {
      return new PageDTO<>(groupService.findGroupsForUser(id, query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/groups")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Add Groups to user")})
  public @ResponseBody User addGroupsToUser(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestBody(required = true) List<UUID> groupIds) {
    return userService.associateGroupsWithUser(id, groupIds);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/groups/{groupIDs}")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Delete Groups from User")})
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteGroupsFromUser(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
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
  @Parameters({
    @Parameter(
        name = Fields.ID,
        required = true,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Search for ids containing this text"),
    @Parameter(
        name = LIMIT,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Number of results to retrieve"),
    @Parameter(
        name = OFFSET,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Index of first result to retrieve"),
    @Parameter(
        name = SORT,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Field to sort on"),
    @Parameter(
        name = SORTORDER,
        required = false,
        schema = @Schema(type = "string"),
        in = ParameterIn.QUERY,
        description = "Sorting order: ASC|DESC. Default order: DESC"),
  })
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Page Applications for a User")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<Application> getApplicationsFromUser(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestParam(value = "query", required = false) String query,
      @Parameter(hidden = true) @Filters List<SearchFilter> filters,
      @Parameter(hidden = true) Pageable pageable) {
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
      value = {@ApiResponse(responseCode = "200", description = "Add Applications to User")})
  public @ResponseBody User addApplicationsToUser(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestBody(required = true) List<UUID> applicationIds) {
    return userService.associateApplicationsWithUser(id, applicationIds);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/applications/{applicationIds}")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Delete Applications from User")})
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteApplicationsFromUser(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @PathVariable(value = "applicationIds", required = true) List<UUID> applicationIds) {
    userService.disassociateApplicationsFromUser(id, applicationIds);
  }
}
