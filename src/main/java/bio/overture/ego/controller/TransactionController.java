/*
 * Copyright (c) 2019. The Ontario Institute for Cancer Research. All rights reserved.
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

import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static java.lang.String.format;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.bind.annotation.RequestMethod.*;

import bio.overture.ego.model.dto.*;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.security.ApplicationScoped;
import bio.overture.ego.service.GroupPermissionService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.PolicyService;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.exceptions.InvalidRequestException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Transactional
@RequestMapping("/transaction")
public class TransactionController {
  PolicyService policyService;
  GroupService groupService;
  GroupPermissionService groupPermissionService;

  @Autowired
  TransactionController(
      PolicyService policyService,
      GroupService groupService,
      GroupPermissionService groupPermissionService) {
    this.policyService = policyService;
    this.groupService = groupService;
    this.groupPermissionService = groupPermissionService;
  }

  @ApplicationScoped()
  @RequestMapping(method = POST, value = "/group_permissions")
  @ResponseStatus(value = OK)
  @SneakyThrows
  /**
   * * Ensure that all of the specified permissions have been granted to the specified groups as a
   * single transaction, creating any creating groups, policies, and/or permissions necessary to do
   * so.
   */
  public void createGroupPermissions(
      @RequestBody() final List<TransactionalGroupPermissionRequest> requests) {

    createPermissions(requests);
  }

  List<UUID> createPermissions(List<TransactionalGroupPermissionRequest> requests) {
    return mapToList(requests, this::createPermission);
  }

  UUID createPermission(TransactionalGroupPermissionRequest request) {
    val policy = policyService.getPolicyByNameCreateIfNecessary(request.getPolicyName());
    val group = groupService.getGroupByNameCreateIfNecessary(request.getGroupName());
    val mask = request.getMask();
    val permission = getPermission(group, policy, mask);

    if (permission.isPresent()) {
      return permission.get().getId();
    }

    val newGroup =
        groupPermissionService.addPermissions(
            group.getId(), ImmutableList.of(new PermissionRequest(policy.getId(), mask)));

    val newPermission = getPermission(newGroup, policy, request.getMask());

    if (newPermission.isEmpty()) {
      throw new RuntimeException(
          format(
              "Can't create permission for group '%s', policy'%s', mask'%s'",
              request.getGroupName(), request.getPolicyName(), request.getMask()));
    }
    return newPermission.get().getId();
  }

  private Optional<GroupPermission> getPermission(Group group, Policy policy, AccessLevel mask) {
    return group.getPermissions().stream()
        .filter(permission -> samePermission(permission, policy.getId(), mask))
        .findFirst();
  }

  private boolean samePermission(GroupPermission groupPermission, UUID policyId, AccessLevel mask) {
    return groupPermission.getPolicy().getId().equals(policyId)
        && groupPermission.getAccessLevel().equals(mask);
  }

  @ApplicationScoped()
  @RequestMapping(method = POST, value = "/mass_delete")
  @ResponseStatus(value = OK)
  public void deleteGroupPermissions(@RequestBody() final TransactionalDeleteRequest request) {
    for (val name : request.getGroupNames()) {
      deleteGroupByName(name);
    }
    for (val name : request.getPolicyNames()) {
      deletePolicyByName(name);
    }
  }

  private void deleteGroupByName(String name) {
    val group = groupService.findByName(name);
    if (group.isPresent()) {
      groupService.delete(group.get().getId());
    }
  }

  private void deletePolicyByName(String name) {
    val policy = policyService.findByName(name);
    if (policy.isPresent()) {
      policyService.delete(policy.get().getId());
    }
  }

  @ExceptionHandler({InvalidRequestException.class})
  public ResponseEntity<Object> handleInvalidRequestException(
      HttpServletRequest req, InvalidRequestException ex) {
    log.error(format("Invalid request: %s", ex.getMessage()));
    return new ResponseEntity<>("{\"error\": \"%s\"}".format(ex.getMessage()), BAD_REQUEST);
  }

  private String jsonEscape(String text) {
    return text.replace("\"", "\\\"");
  }

  private ResponseEntity<Object> errorResponse(HttpStatus status, String fmt, Exception ex) {
    log.error(format(fmt, ex.getMessage()));
    val headers = new HttpHeaders();
    headers.setContentType(APPLICATION_JSON);
    val msg = format("{\"error\": \"%s\"}", jsonEscape(ex.getMessage()));
    return new ResponseEntity<>(msg, status);
  }
}
