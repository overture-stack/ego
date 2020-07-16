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
import bio.overture.ego.model.enums.Fields;
import bio.overture.ego.model.search.Filters;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.security.AdminScoped;
import bio.overture.ego.service.ApplicationPermissionService;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
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
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@Slf4j
@RestController
@RequestMapping("/applications")
@Api(tags = "Applications")
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
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Page Applications")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<Application> listApplications(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestParam(value = "query", required = false) String query,
      @ApiIgnore @Filters List<SearchFilter> filters,
      @ApiIgnore Pageable pageable) {
    if (isEmpty(query)) {
      return new PageDTO<>(applicationService.listApps(filters, pageable));
    } else {
      return new PageDTO<>(applicationService.findApps(query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = POST, value = "")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "New Application", response = Application.class)})
  public @ResponseBody Application createApplication(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @RequestBody(required = true) CreateApplicationRequest request) {
    return applicationService.create(request);
  }

  @AdminScoped
  @RequestMapping(method = GET, value = "/{id}")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Application Details", response = Application.class)
      })
  @JsonView(Views.REST.class)
  public @ResponseBody Application getApplication(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    return applicationService.getById(id);
  }

  @AdminScoped
  @RequestMapping(method = PUT, value = "/{id}")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Updated application info", response = Application.class)
      })
  public @ResponseBody Application updateApplication(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(name = "id", required = true) UUID id,
      @RequestBody(required = true) UpdateApplicationRequest updateRequest) {
    return applicationService.partialUpdate(id, updateRequest);
  }

  @AdminScoped
  @RequestMapping(method = DELETE, value = "/{id}")
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteApplication(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    applicationService.delete(id);
  }

  @AdminScoped
  @RequestMapping(method = GET, value = "/{id}/users")
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
        name = Fields.ID,
        required = true,
        dataType = "string",
        paramType = "path",
        value = "Search for ids containing this text"),
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
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Page Users for an Application")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<User> getUsersForApplication(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestParam(value = "query", required = false) String query,
      @ApiIgnore @Filters List<SearchFilter> filters,
      @ApiIgnore Pageable pageable) {
    if (isEmpty(query)) {
      return new PageDTO<>(userService.findUsersForApplication(id, filters, pageable));
    } else {
      return new PageDTO<>(userService.findUsersForApplication(id, query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = GET, value = "/{id}/groups")
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
        name = Fields.ID,
        required = true,
        dataType = "string",
        paramType = "path",
        value = "Search for ids containing this text"),
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
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Page Groups for an Application")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<Group> getGroupsForApplication(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestParam(value = "query", required = false) String query,
      @ApiIgnore @Filters List<SearchFilter> filters,
      @ApiIgnore Pageable pageable) {
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
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Page Permissions for an Application"),
      })
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<ApplicationPermission> getPermissionsForApplication(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @ApiIgnore Pageable pageable) {
    return new PageDTO<>(applicationPermissionService.getPermissions(id, pageable));
  }

  @AdminScoped
  @RequestMapping(method = POST, value = "/{id}/permissions")
  @ApiResponses(
      value = {
        @ApiResponse(
            code = 200,
            message = "Add application permissions",
            response = Application.class)
      })
  public @ResponseBody Application addPermissions(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestBody(required = true) List<PermissionRequest> permissions) {
    return applicationPermissionService.addPermissions(id, permissions);
  }

  @AdminScoped
  @RequestMapping(method = DELETE, value = "/{id}/permissions/{permissionIds}")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Delete application permissions")})
  @ResponseStatus(value = OK)
  public void deletePermissions(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
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
            code = 200,
            message =
                "Get effective permissions for an application with application and group permissions")
      })
  @ResponseStatus(value = HttpStatus.OK)
  public @ResponseBody Collection<ResolvedPermissionResponse> getResolvedPermissions(
      @ApiIgnore @RequestHeader(value = "Authorization", required = true)
          final String authorization,
      @PathVariable(value = "id", required = true) UUID id) {
    return applicationPermissionService.getResolvedPermissions(id);
  }
}
