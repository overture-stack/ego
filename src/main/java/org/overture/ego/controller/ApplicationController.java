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
@RequestMapping("/applications")
public class ApplicationController {

  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private GroupService groupService;
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
          @ApiResponse(code = 200, message = "Page of Applications", response = PageDTO.class)
      }
  )
  @JsonView(Views.REST.class)
  public @ResponseBody
  PageDTO<Application> getApplicationsList(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @RequestParam(value = "query", required = false) String query,
          @ApiIgnore @Filters List<SearchFilter> filters,
          Pageable pageable) {
    if(StringUtils.isEmpty(query)){
      return new PageDTO<>(applicationService.listApps(filters, pageable));
    } else {
      return new PageDTO<>(applicationService.findApps(query, filters, pageable));
    }
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.POST, value = "")
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "New Application", response = Application.class)
      }
  )
  public @ResponseBody
  Application create(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @RequestBody(required = true) Application applicationInfo) {
    return applicationService.create(applicationInfo);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}")
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Application Details", response = Application.class)
      }
  )
  @JsonView(Views.REST.class)
  public @ResponseBody
  Application get(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String applicationId) {
    return applicationService.get(applicationId);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.PUT, value = "/{id}")
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Updated application info", response = Application.class)
      }
  )
  public @ResponseBody
  Application updateApplication(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @RequestBody(required = true) Application updatedApplicationInfo) {
    return applicationService.update(updatedApplicationInfo);
  }

  @AdminScoped
  @RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
  @ResponseStatus(value = HttpStatus.OK)
  public void deleteApplication(
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
      @PathVariable(value = "id", required = true) String applicationId) {
    applicationService.delete(applicationId);
  }

  /*
   Users related endpoints
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
  PageDTO<User> getApplicationUsers(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String appId,
          @RequestParam(value = "query", required = false) String query,
          @ApiIgnore @Filters List<SearchFilter> filters,
          Pageable pageable)
  {
    if(StringUtils.isEmpty(query)){
      return new PageDTO<>(userService.findAppsUsers(appId, filters, pageable));
    } else {
      return new PageDTO<>(userService.findAppsUsers(appId, query, filters, pageable));
    }
  }

  /*
   Groups related endpoints
    */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/groups")
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
  PageDTO<Group> getApplicationsGroups(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String appId,
          @RequestParam(value = "query", required = false) String query,
          @ApiIgnore @Filters List<SearchFilter> filters,
          Pageable pageable)
  {
    if(StringUtils.isEmpty(query)) {
      return new PageDTO<>(groupService.findApplicationsGroup(appId, filters, pageable));
    } else {
      return new PageDTO<>(groupService.findApplicationsGroup(appId, query, filters, pageable));
    }
  }

  @ExceptionHandler({ EntityNotFoundException.class })
  public ResponseEntity<Object> handleEntityNotFoundException(HttpServletRequest req, EntityNotFoundException ex) {
    log.error("Application ID not found.");
    return new ResponseEntity<Object>("Invalid Application ID provided.", new HttpHeaders(),
        HttpStatus.BAD_REQUEST);
  }

}
