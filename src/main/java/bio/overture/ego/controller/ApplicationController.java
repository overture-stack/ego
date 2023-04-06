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
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import bio.overture.ego.model.dto.*;
import bio.overture.ego.model.entity.*;
import bio.overture.ego.model.search.Filters;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.security.AdminScoped;
import bio.overture.ego.service.ApplicationPermissionService;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.UserService;
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
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/applications")
@Tag(name = "Applications")
public class ApplicationController {

  /** Dependencies */
  private final ApplicationService applicationService;

  private final GroupService groupService;
  private final UserService userService;
  private final ApplicationPermissionService applicationPermissionService;

  @Autowired
  public ApplicationController(
      @NonNull ApplicationService applicationService,
      @NonNull GroupService groupService,
      @NonNull UserService userService,
      @NonNull ApplicationPermissionService applicationPermissionService) {
    this.applicationService = applicationService;
    this.groupService = groupService;
    this.userService = userService;
    this.applicationPermissionService = applicationPermissionService;
  }

  @AdminScoped
  @RequestMapping(method = GET, value = "")
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
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Page Applications")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<Application> listApplications(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestParam(value = "query", required = false) String query,
      @Parameter(hidden = true) @Filters List<SearchFilter> filters,
      @Parameter(hidden = true) Pageable pageable) {
    if (isEmpty(query)) {
      return new PageDTO<>(applicationService.listApps(filters, pageable));
    } else {
      return new PageDTO<>(applicationService.findApps(query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = POST, value = "")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "New Application")})
  public @ResponseBody Application createApplication(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody(required = true) CreateApplicationRequest request) {
    return applicationService.create(request);
  }

  @AdminScoped
  @RequestMapping(method = GET, value = "/{id}")
  @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Application Details")})
  @JsonView(Views.REST.class)
  public @ResponseBody Application getApplication(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    return applicationService.getById(id);
  }

  @AdminScoped
  @RequestMapping(method = PUT, value = "/{id}")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Updated application info")})
  public @ResponseBody Application updateApplication(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(name = "id", required = true) UUID id,
      @RequestBody(required = true) UpdateApplicationRequest updateRequest) {
    return applicationService.partialUpdate(id, updateRequest);
  }

  @AdminScoped
  @RequestMapping(method = DELETE, value = "/{id}")
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteApplication(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    applicationService.delete(id);
  }

  @AdminScoped
  @RequestMapping(method = GET, value = "/{id}/users")
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
      value = {@ApiResponse(responseCode = "200", description = "Page Users for an Application")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<User> getUsersForApplication(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestParam(value = "query", required = false) String query,
      @Parameter(hidden = true) @Filters List<SearchFilter> filters,
      @Parameter(hidden = true) Pageable pageable) {
    if (isEmpty(query)) {
      return new PageDTO<>(userService.findUsersForApplication(id, filters, pageable));
    } else {
      return new PageDTO<>(userService.findUsersForApplication(id, query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = GET, value = "/{id}/groups")
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
      value = {@ApiResponse(responseCode = "200", description = "Page Groups for an Application")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<Group> getGroupsForApplication(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestParam(value = "query", required = false) String query,
      @Parameter(hidden = true) @Filters List<SearchFilter> filters,
      @Parameter(hidden = true) Pageable pageable) {
    if (isEmpty(query)) {
      return new PageDTO<>(groupService.findGroupsForApplication(id, filters, pageable));
    } else {
      return new PageDTO<>(groupService.findGroupsForApplication(id, query, filters, pageable));
    }
  }

  /*
  Permissions related endpoints
   */
  @AdminScoped
  @RequestMapping(method = GET, value = "/{id}/permissions")
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
        @ApiResponse(responseCode = "200", description = "Page Permissions for an Application"),
      })
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<ApplicationPermission> getPermissionsForApplication(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @Parameter(hidden = true) Pageable pageable) {
    return new PageDTO<>(applicationPermissionService.getPermissions(id, pageable));
  }

  @AdminScoped
  @RequestMapping(method = POST, value = "/{id}/permissions")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Add application permissions")})
  public @ResponseBody Application addPermissions(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestBody(required = true) List<PermissionRequest> permissions) {
    return applicationPermissionService.addPermissions(id, permissions);
  }

  @AdminScoped
  @RequestMapping(method = DELETE, value = "/{id}/permissions/{permissionIds}")
  @ApiResponses(
      value = {@ApiResponse(responseCode = "200", description = "Delete application permissions")})
  @ResponseStatus(value = OK)
  public void deletePermissions(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @PathVariable(value = "permissionIds", required = true) List<UUID> permissionIds) {
    applicationPermissionService.deletePermissions(id, permissionIds);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/groups/permissions")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description =
                "Get effective permissions for an application with application and group permissions")
      })
  @ResponseStatus(value = HttpStatus.OK)
  public @ResponseBody Collection<ResolvedPermissionResponse> getResolvedPermissions(
      @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    return applicationPermissionService.getResolvedPermissions(id);
  }
}
