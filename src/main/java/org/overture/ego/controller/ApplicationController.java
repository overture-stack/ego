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
import org.springframework.web.bind.annotation.*;

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
  @ApiResponses(
      value = {
          @ApiResponse(code = 200, message = "Page of applications", response = Page.class)
      }
  )
  public @ResponseBody
  Page<Application> getApplicationsList(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @RequestParam(value = "query", required = false) String query,
          Pageable pageable) {
    return applicationService.findApps(query, pageable);
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
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Page of users of group", response = Page.class)
          }
  )
  public @ResponseBody
  Page<User> getApplicationUsers(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String appId,
          @RequestParam(value = "query", required = false) String query,
          Pageable pageable)
  {
    return userService.findAppsUsers(appId, query, pageable);
  }

  /*
   Groups related endpoints
    */
  @AdminScoped
  @RequestMapping(method = RequestMethod.GET, value = "/{id}/groups")
  @ApiResponses(
          value = {
                  @ApiResponse(code = 200, message = "Page of applications of group", response = Page.class)
          }
  )
  public @ResponseBody
  Page<Group> getApplicationsGroups(
          @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = true) final String accessToken,
          @PathVariable(value = "id", required = true) String appId,
          @RequestParam(value = "query", required = false) String query,
          Pageable pageable)
  {
    return groupService.findApplicationsGroup(appId, query, pageable);
  }

}
