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
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.model.exceptions.PostWithIdentifierException;
import bio.overture.ego.model.params.PolicyIdStringWithAccessLevel;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.ApplicationRepository;
import bio.overture.ego.repository.GroupRepository;
import bio.overture.ego.repository.UserRepository;
import bio.overture.ego.repository.queryspecification.GroupSpecification;
import lombok.Builder;
import lombok.NonNull;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;

import static bio.overture.ego.model.exceptions.NotFoundException.checkExists;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Converters.convertToUUIDList;
import static bio.overture.ego.utils.Joiners.COMMA;
import static java.util.UUID.fromString;
import static org.springframework.data.jpa.domain.Specifications.where;

@Service
@Builder
public class GroupService extends BaseService<Group, UUID> {

  private final GroupRepository groupRepository;
  private final UserRepository userRepository;
  private final ApplicationRepository applicationRepository;
  private final PolicyService policyService;
  private final GroupPermissionService permissionService;

  @Autowired
  public GroupService(
      @NonNull GroupRepository groupRepository,
      @NonNull UserRepository userRepository,
      @NonNull ApplicationRepository applicationRepository,
      @NonNull PolicyService policyService,
      @NonNull GroupPermissionService permissionService) {
    this.groupRepository = groupRepository;
    this.userRepository = userRepository;
    this.applicationRepository = applicationRepository;
    this.policyService = policyService;
    this.permissionService = permissionService;
  }

  public Group create(@NonNull Group groupInfo) {
    if (Objects.nonNull(groupInfo.getId())) {
      throw new PostWithIdentifierException();
    }

    return groupRepository.save(groupInfo);
  }

  public Group addAppsToGroup(@NonNull String grpId, @NonNull List<String> appIDs) {
    val group = getById(groupRepository, fromString(grpId));
    appIDs.forEach(
            appId -> {
              val app = applicationRepository.findById(fromString(appId)).orElseThrow(() -> new NotFoundException(String.format("Could not find Application with ID: %s", appId)));
              group.getApplications().add(app);
              app.getGroups().add(group);
            });
    return groupRepository.save(group);
  }

  public Group addUsersToGroup(@NonNull String grpId, @NonNull List<String> userIds) {
    val group = getById(groupRepository, fromString(grpId));
    userIds.forEach(
            userId -> {
              val user = userRepository.findById(fromString(userId)).orElseThrow(() -> new NotFoundException(String.format("Could not find User with ID: %s", userId)));
              group.getUsers().add(user);
              user.getGroups().add(group);
            });
    return groupRepository.save(group);
  }

  public Group addGroupPermissions(
      @NonNull String groupId, @NonNull List<PolicyIdStringWithAccessLevel> permissions) {
    val group = getById(groupRepository, fromString(groupId));
    permissions.forEach(
        permission -> {
          val policy = policyService.get(permission.getPolicyId());
          val mask = AccessLevel.fromValue(permission.getMask());
          group
              .getPermissions()
              .add(GroupPermission.builder().policy(policy).accessLevel(mask).owner(group).build());
        });
    return groupRepository.save(group);
  }

  public Group get(@NonNull String groupId) {
    return getById(groupRepository, fromString(groupId));
  }

  public Set<Group> getMany(@NonNull Collection<String> groupIds) {
    val groups = groupRepository.findAllByIdIn(convertToUUIDList(groupIds));
    val nonExistingApps =
        groups
            .stream()
            .map(Group::getId)
            .filter(x -> !groupRepository.existsById(x))
            .collect(toImmutableSet());
    checkExists(
        nonExistingApps.isEmpty(),
        "The following group ids were not found: %s",
        COMMA.join(nonExistingApps));
    return groups;
  }

  public Group getByName(@NonNull String groupName) {
    return groupRepository.findOneByNameIgnoreCase(groupName);
  }

  // TODO: Check that not allowing update of relationships this way is ok for end-user
  public Group update(@NonNull Group other) {
    val existingGroup = getById(groupRepository, other.getId());

    val updatedGroup =
        Group.builder()
            .id(other.getId())
            .name(other.getName())
            .description(other.getDescription())
            .status(other.getStatus())
            .applications(existingGroup.getApplications())
            .users(existingGroup.getUsers())
        .build();;

    return groupRepository.save(updatedGroup);
  }

  // TODO - this was the original update - will use an improved version of this for the PATCH
  public Group partialUpdate(@NonNull Group other) {

    val existingGroup = getById(groupRepository, other.getId());

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

  public void delete(@NonNull String groupId) {
    groupRepository.deleteById(fromString(groupId));
  }

  public Page<Group> listGroups(@NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return groupRepository.findAll(GroupSpecification.filterBy(filters), pageable);
  }

  public Page<GroupPermission> getGroupPermissions(
      @NonNull String groupId, @NonNull Pageable pageable) {
    val groupPermissions =
        new ArrayList<>(getById(groupRepository, fromString(groupId)).getPermissions());
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
    val group = getById(groupRepository, fromString(grpId));
    // TODO - Properly handle invalid IDs here
    appIDs.forEach(
        appId -> {
          val app = applicationRepository.findById(fromString(appId)).orElseThrow(() -> new NotFoundException(String.format("Could not find Application with ID: %s", appId)));
          group.getApplications().remove(app);
          app.getGroups().remove(group);
        });
    groupRepository.save(group);
  }

  public void deleteGroupPermissions(@NonNull String userId, @NonNull List<String> permissionsIds) {
    val group = getById(groupRepository, fromString(userId));
    permissionsIds.forEach(
        permissionsId -> {
          group.getPermissions().remove(permissionService.get(permissionsId));
        });
    groupRepository.save(group);
  }
}
