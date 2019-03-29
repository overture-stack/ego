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

import bio.overture.ego.model.dto.GroupRequest;
import bio.overture.ego.model.dto.PageDTO;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.Fields;
import bio.overture.ego.model.exceptions.PostWithIdentifierException;
import bio.overture.ego.model.search.Filters;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.security.AdminScoped;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.GroupPermissionService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.view.Views;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
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
import org.springframework.util.StringUtils;
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
@RequestMapping("/groups")
public class GroupController {

  /** Dependencies */
  private final GroupService groupService;

  private final ApplicationService applicationService;

  private final GroupPermissionService groupPermissionService;

  @Autowired
  public GroupController(
      @NonNull GroupService groupService,
      @NonNull GroupPermissionService groupPermissionService,
      @NonNull ApplicationService applicationService) {
    this.groupService = groupService;
    this.groupPermissionService = groupPermissionService;
    this.applicationService = applicationService;
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "")
  @ApiImplicitParams({
    @ApiImplicitParam(
        name = "limit",
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Number of results to retrieve"),
    @ApiImplicitParam(
        name = "offset",
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Index of first result to retrieve"),
    @ApiImplicitParam(
        name = Fields.ID,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Search for ids containing this text"),
    @ApiImplicitParam(
        name = "sort",
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Field to sort on"),
    @ApiImplicitParam(
        name = "sortOrder",
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Sorting order: ASC|DESC. Default order: DESC"),
  })
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Page Groups")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<Group> findGroups(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @RequestParam(value = "query", required = false) String query,
      @ApiIgnore @Filters List<SearchFilter> filters,
      Pageable pageable) {
    if (isEmpty(query)) {
      return new PageDTO<>(groupService.listGroups(filters, pageable));
    } else {
      return new PageDTO<>(groupService.findGroups(query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "")
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "New Group", response = Group.class),
        @ApiResponse(
            code = 400,
            message = PostWithIdentifierException.reason,
            response = Group.class)
      })
  public @ResponseBody Group createGroup(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION) final String accessToken,
      @RequestBody GroupRequest createRequest) {
    return groupService.create(createRequest);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Group Details", response = Group.class)})
  @JsonView(Views.REST.class)
  public @ResponseBody Group getGroup(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION) final String accessToken,
      @PathVariable(value = "id") UUID id) {
    return groupService.getById(id);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.PUT, value = "/{id}")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Updated group info", response = Group.class)})
  public @ResponseBody Group updateGroup(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id") UUID id,
      @RequestBody(required = true) GroupRequest updateRequest) {
    return groupService.partialUpdate(id, updateRequest);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteGroup(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id) {
    groupService.delete(id);
  }

  /*
  Permissions related endpoints
   */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/permissions")
  @ApiImplicitParams({
    @ApiImplicitParam(
        name = "limit",
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Number of results to retrieve"),
    @ApiImplicitParam(
        name = "offset",
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Index of first result to retrieve"),
    @ApiImplicitParam(
        name = "sort",
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Field to sort on"),
    @ApiImplicitParam(
        name = "sortOrder",
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Sorting order: ASC|DESC. Default order: DESC"),
  })
  @ApiResponses(
      value = {
        @ApiResponse(code = 200, message = "Page of Group Permissions for a Group"),
      })
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<GroupPermission> getGroupPermissionsForGroup(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id,
      Pageable pageable) {
    return new PageDTO<>(groupPermissionService.getPermissions(id, pageable));
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/permissions")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Add group permissions", response = Group.class)})
  public @ResponseBody Group addPermissions(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestBody(required = true) List<PermissionRequest> permissions) {
    return groupPermissionService.addPermissions(id, permissions);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/permissions/{permissionIds}")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Delete group permissions")})
  @ResponseStatus(value = HttpStatus.OK)
  public void deletePermissions(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id,
      @PathVariable(value = "permissionIds", required = true) List<UUID> permissionIds) {
    groupPermissionService.deletePermissions(id, permissionIds);
  }

  /*
  Application related endpoints
   */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/applications")
  @ApiImplicitParams({
    @ApiImplicitParam(
        name = "limit",
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Number of results to retrieve"),
    @ApiImplicitParam(
        name = "offset",
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Index of first result to retrieve"),
    @ApiImplicitParam(
        name = Fields.ID,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Search for ids containing this text"),
    @ApiImplicitParam(
        name = "sort",
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Field to sort on"),
    @ApiImplicitParam(
        name = "sortOrder",
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Sorting order: ASC|DESC. Default order: DESC"),
  })
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Page Applications for a Group")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<Application> getApplicationsForGroup(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestParam(value = "query", required = false) String query,
      @ApiIgnore @Filters List<SearchFilter> filters,
      Pageable pageable) {
    if (StringUtils.isEmpty(query)) {
      return new PageDTO<>(applicationService.findGroupApplications(id, filters, pageable));
    } else {
      return new PageDTO<>(applicationService.findGroupApplications(id, query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/applications")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Add Apps to Group", response = Group.class)})
  public @ResponseBody Group addAppsToGroups(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestBody(required = true) List<UUID> appIds) {
    return groupService.addAppsToGroup(id, appIds);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/applications/{appIds}")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Delete Apps from Group")})
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteAppsFromGroup(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id,
      @PathVariable(value = "appIds", required = true) List<UUID> appIds) {
    groupService.deleteAppsFromGroup(id, appIds);
  }

  /*
  User related endpoints
   */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/users")
  @ApiImplicitParams({
    @ApiImplicitParam(
        name = "limit",
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Number of results to retrieve"),
    @ApiImplicitParam(
        name = "offset",
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Index of first result to retrieve"),
    @ApiImplicitParam(
        name = Fields.ID,
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Search for ids containing this text"),
    @ApiImplicitParam(
        name = "sort",
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Field to sort on"),
    @ApiImplicitParam(
        name = "sortOrder",
        required = false,
        dataType = "string",
        paramType = "query",
        value = "Sorting order: ASC|DESC. Default order: DESC"),
  })
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Page Users for a Group")})
  @JsonView(Views.REST.class)
  public @ResponseBody PageDTO<User> getUsersForGroup(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestParam(value = "query", required = false) String query,
      @ApiIgnore @Filters List<SearchFilter> filters,
      Pageable pageable) {
    if (StringUtils.isEmpty(query)) {
      return new PageDTO<>(groupService.findUsersForGroup(id, filters, pageable));
    } else {
      return new PageDTO<>(groupService.findUsersForGroup(id, query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "/{id}/users")
  @ApiResponses(
      value = {@ApiResponse(code = 200, message = "Add Users to Group", response = Group.class)})
  public @ResponseBody Group addUsersToGroup(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id,
      @RequestBody(required = true) List<UUID> userIds) {
    return groupService.associateUsersWithGroup(id, userIds);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}/users/{userIds}")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Delete Users from Group")})
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteUsersFromGroup(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) UUID id,
      @PathVariable(value = "userIds", required = true) List<UUID> userIds) {
    groupService.disassociateUsersFromGroup(id, userIds);
  }

  @ExceptionHandler({EntityNotFoundException.class})
  public ResponseEntity<Object> handleEntityNotFoundException(
      HttpServletRequest req, EntityNotFoundException ex) {
    log.error("Group ID not found.");
    return new ResponseEntity<Object>(
        "Invalid Group ID provided.", new HttpHeaders(), HttpStatus.BAD_REQUEST);
  }
}
