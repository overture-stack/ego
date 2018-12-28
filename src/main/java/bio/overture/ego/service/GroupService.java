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

package bio.overture.ego.service;

import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.exceptions.PostWithIdentifierException;
import bio.overture.ego.model.params.PolicyIdStringWithAccessLevel;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.GroupRepository;
import bio.overture.ego.repository.queryspecification.GroupSpecification;
import lombok.NonNull;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.UUID.fromString;
import static org.springframework.data.jpa.domain.Specifications.where;

@Service
public class GroupService extends AbstractNamedService<Group> {

  private final GroupRepository groupRepository;
  private final ApplicationService applicationService;
  private final PolicyService policyService;
  private final GroupPermissionService permissionService;

  @Autowired
  public GroupService(
      @NonNull GroupRepository groupRepository,
      @NonNull ApplicationService applicationService,
      @NonNull PolicyService policyService,
      @NonNull GroupPermissionService permissionService) {
    super(Group.class, groupRepository);
    this.applicationService = applicationService;
    this.policyService = policyService;
    this.permissionService = permissionService;
    this.groupRepository = groupRepository;
  }

  public Group create(@NonNull Group groupInfo) {
    if (Objects.nonNull(groupInfo.getId())) {
      throw new PostWithIdentifierException();
    }

    return getRepository().save(groupInfo);
  }

  public Group addAppsToGroup(@NonNull String grpId, @NonNull List<String> appIDs) {
    val group = getById(grpId);
    appIDs.forEach(
        appId -> {
          val app = applicationService.get(appId);
          group.getApplications().add(app);
        });
    return getRepository().save(group);
  }

  public Group addGroupPermissions(
      @NonNull String groupId, @NonNull List<PolicyIdStringWithAccessLevel> permissions) {
    val group = getById(groupId);
    permissions.forEach(
        permission -> {
          val policy = policyService.get(permission.getPolicyId());
          val mask = AccessLevel.fromValue(permission.getMask());
          group
              .getPermissions()
              .add(GroupPermission.builder().policy(policy).accessLevel(mask).owner(group).build());
        });
    return getRepository().save(group);
  }

  public Group get(@NonNull String groupId) {
    return getById(groupId);
  }

  public Group update(@NonNull Group other) {
    return groupRepository.save(other);
  }

  // TODO - this was the original update - will use an improved version of this for the PATCH
  public Group partialUpdate(@NonNull Group other) {
    val existingGroup = getById(other.getId().toString());

    val builder =
        Group.builder()
            .id(other.getId())
            .name(other.getName())
            .description(other.getDescription())
            .status(other.getStatus());

    if (other.getApplications() != null) {
      builder.applications(other.getApplications());
    } else {
      builder.applications(existingGroup.getApplications());
    }

    if (other.getUsers() != null) {
      builder.users(other.getUsers());
    } else {
      builder.users(existingGroup.getUsers());
    }

    val updatedGroup = builder.build();

    return groupRepository.save(updatedGroup);
  }

  public Page<Group> listGroups(@NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return groupRepository.findAll(GroupSpecification.filterBy(filters), pageable);
  }

  public Page<GroupPermission> getGroupPermissions(
      @NonNull String groupId, @NonNull Pageable pageable) {
    val groupPermissions = new ArrayList<>(getById(groupId).getPermissions());
    return new PageImpl<>(groupPermissions, pageable, groupPermissions.size());
  }

  public Page<Group> findGroups(
      @NonNull String query, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return groupRepository.findAll(
        where(GroupSpecification.containsText(query)).and(GroupSpecification.filterBy(filters)),
        pageable);
  }

  public Page<Group> findUserGroups(
      @NonNull String userId, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return groupRepository.findAll(
        where(GroupSpecification.containsUser(fromString(userId)))
            .and(GroupSpecification.filterBy(filters)),
        pageable);
  }

  public Page<Group> findUserGroups(
      @NonNull String userId,
      @NonNull String query,
      @NonNull List<SearchFilter> filters,
      @NonNull Pageable pageable) {
    return groupRepository.findAll(
        where(GroupSpecification.containsUser(fromString(userId)))
            .and(GroupSpecification.containsText(query))
            .and(GroupSpecification.filterBy(filters)),
        pageable);
  }

  public Page<Group> findApplicationGroups(
      @NonNull String appId, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return groupRepository.findAll(
        where(GroupSpecification.containsApplication(fromString(appId)))
            .and(GroupSpecification.filterBy(filters)),
        pageable);
  }

  public Page<Group> findApplicationGroups(
      @NonNull String appId,
      @NonNull String query,
      @NonNull List<SearchFilter> filters,
      @NonNull Pageable pageable) {
    return groupRepository.findAll(
        where(GroupSpecification.containsApplication(fromString(appId)))
            .and(GroupSpecification.containsText(query))
            .and(GroupSpecification.filterBy(filters)),
        pageable);
  }

  public void deleteAppsFromGroup(@NonNull String grpId, @NonNull List<String> appIDs) {
    val group = getById(grpId);
    // TODO - Properly handle invalid IDs here
    appIDs.forEach(
        appId -> {
          // TODO if app id not valid (does not exist) we need to throw EntityNotFoundException
          group.getApplications().remove(applicationService.get(appId));
        });
    groupRepository.save(group);
  }

  public void deleteGroupPermissions(@NonNull String userId, @NonNull List<String> permissionsIds) {
    val group = getById(userId);
    permissionsIds.forEach(
        permissionsId -> {
          group.getPermissions().remove(permissionService.get(permissionsId));
        });
    groupRepository.save(group);
  }
}
