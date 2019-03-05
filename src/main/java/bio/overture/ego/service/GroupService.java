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

import static bio.overture.ego.model.exceptions.NotFoundException.buildNotFoundException;
import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.model.exceptions.UniqueViolationException.checkUnique;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Converters.convertToUUIDList;
import static bio.overture.ego.utils.Converters.convertToUUIDSet;
import static bio.overture.ego.utils.FieldUtils.onUpdateDetected;
import static bio.overture.ego.utils.Joiners.COMMA;
import static java.lang.String.format;
import static java.util.UUID.fromString;
import static org.mapstruct.factory.Mappers.getMapper;
import static org.springframework.data.jpa.domain.Specifications.where;

import bio.overture.ego.model.dto.GroupRequest;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.ApplicationRepository;
import bio.overture.ego.repository.GroupRepository;
import bio.overture.ego.repository.UserRepository;
import bio.overture.ego.repository.queryspecification.GroupSpecification;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.NonNull;
import lombok.val;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.TargetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class GroupService extends AbstractNamedService<Group, UUID> {

  private static final GroupConverter GROUP_CONVERTER = getMapper(GroupConverter.class);

  private final GroupRepository groupRepository;
  private final UserRepository userRepository;
  private final ApplicationRepository applicationRepository;
  private final ApplicationService applicationService;

  @Autowired
  public GroupService(
      @NonNull GroupRepository groupRepository,
      @NonNull UserRepository userRepository,
      @NonNull ApplicationRepository applicationRepository,
      @NonNull ApplicationService applicationService) {
    super(Group.class, groupRepository);
    this.groupRepository = groupRepository;
    this.userRepository = userRepository;
    this.applicationRepository = applicationRepository;
    this.applicationService = applicationService;
  }

  public Group create(@NonNull GroupRequest request) {
    checkNameUnique(request.getName());
    val group = GROUP_CONVERTER.convertToGroup(request);
    return getRepository().save(group);
  }

  public Group getGroupWithRelationships(@NonNull UUID id) {
    val result = groupRepository.findGroupById(id);
    checkNotFound(result.isPresent(), "The groupId '%s' does not exist", id);
    return result.get();
  }

  public Group addAppsToGroup(@NonNull String grpId, @NonNull List<String> appIDs) {
    val group = getById(fromString(grpId));
    val apps = applicationService.getMany(convertToUUIDList(appIDs));
    associateApplications(group, apps);
    return getRepository().save(group);
  }

  // TODO: [rtisma] need to validate userIds all exist. Cannot use userService as it causes circular
  // dependency
  public Group addUsersToGroup(@NonNull String grpId, @NonNull List<String> userIds) {
    val group = getById(fromString(grpId));
    val users = userRepository.findAllByIdIn(convertToUUIDList(userIds));
    associateUsers(group, users);
    return groupRepository.save(group);
  }

  public Group get(@NonNull String groupId) {
    return getById(fromString(groupId));
  }

  public Group partialUpdate(@NonNull String id, @NonNull GroupRequest r) {
    val group = getById(fromString(id));
    validateUpdateRequest(group, r);
    GROUP_CONVERTER.updateGroup(r, group);
    return getRepository().save(group);
  }

  @Deprecated
  public Group update(@NonNull Group other) {
    val existingGroup = getById(other.getId());

    val updatedGroup =
        Group.builder()
            .id(existingGroup.getId())
            .name(other.getName())
            .description(other.getDescription())
            .status(other.getStatus())
            .applications(existingGroup.getApplications())
            .users(existingGroup.getUsers())
            .build();

    return groupRepository.save(updatedGroup);
  }

  public Page<Group> listGroups(@NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return groupRepository.findAll(GroupSpecification.filterBy(filters), pageable);
  }

  public Page<GroupPermission> getGroupPermissions(
      @NonNull String groupId, @NonNull Pageable pageable) {
    val groupPermissions = ImmutableList.copyOf(getById(fromString(groupId)).getPermissions());
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
    val group = getById(fromString(grpId));
    val appIdsToDisassociate = convertToUUIDSet(appIDs);
    checkAppsExistForGroup(group, appIdsToDisassociate);
    val appsToDisassociate =
        group
            .getApplications()
            .stream()
            .filter(a -> appIdsToDisassociate.contains(a.getId()))
            .collect(toImmutableSet());
    val apps = appIDs.stream().map(this::retrieveApplication).collect(toImmutableSet());
    disassociateGroupFromApps(group, appsToDisassociate);
    getRepository().save(group);
  }

  public void deleteUsersFromGroup(@NonNull String grpId, @NonNull List<String> userIDs) {
    val group = getById(fromString(grpId));
    val userIdsToDisassociate = convertToUUIDSet(userIDs);
    checkUsersExistForGroup(group, userIdsToDisassociate);
    val usersToDisassociate =
        group
            .getUsers()
            .stream()
            .filter(u -> userIdsToDisassociate.contains(u.getId()))
            .collect(toImmutableSet());
    disassociateGroupFromUsers(group, usersToDisassociate);
    getRepository().save(group);
  }

  public void delete(String id) {
    delete(fromString(id));
  }

  private void validateUpdateRequest(Group originalGroup, GroupRequest updateRequest) {
    onUpdateDetected(
        originalGroup.getName(),
        updateRequest.getName(),
        () -> checkNameUnique(updateRequest.getName()));
  }

  private void checkNameUnique(String name) {
    checkUnique(
        !groupRepository.existsByNameIgnoreCase(name), "A group with same name already exists");
  }

  private Application retrieveApplication(String appId) {
    // using applicationRepository since using applicationService causes cyclic dependency error
    return applicationRepository
        .findById(fromString(appId))
        .orElseThrow(() -> buildNotFoundException("Could not find Application with ID: %s", appId));
  }

  private User retrieveUser(String userId) {
    // using applicationRepository since using applicationService causes cyclic dependency error
    return userRepository
        .findById(fromString(userId))
        .orElseThrow(() -> buildNotFoundException("Could not find User with ID: %s", userId));
  }

  public static void checkUsersExistForGroup(
      @NonNull Group group, @NonNull Collection<UUID> userIds) {
    val existingUserIds = group.getUsers().stream().map(User::getId).collect(toImmutableSet());
    val nonExistentUserIds =
        userIds.stream().filter(x -> !existingUserIds.contains(x)).collect(toImmutableSet());
    if (!nonExistentUserIds.isEmpty()) {
      throw new NotFoundException(
          format(
              "The following users do not exist for group '%s': %s",
              group.getId(), COMMA.join(nonExistentUserIds)));
    }
  }

  public static void disassociateGroupFromUsers(
      @NonNull Group group, @NonNull Collection<User> users) {
    group.getUsers().removeAll(users);
    users.forEach(x -> x.getGroups().remove(group));
  }

  public static void checkAppsExistForGroup(
      @NonNull Group group, @NonNull Collection<UUID> appIds) {
    val existingAppIds =
        group.getApplications().stream().map(Application::getId).collect(toImmutableSet());
    val nonExistentAppIds =
        appIds.stream().filter(x -> !existingAppIds.contains(x)).collect(toImmutableSet());
    if (!nonExistentAppIds.isEmpty()) {
      throw new NotFoundException(
          format(
              "The following apps do not exist for group '%s': %s",
              group.getId(), COMMA.join(nonExistentAppIds)));
    }
  }

  public static void disassociateGroupFromApps(
      @NonNull Group group, @NonNull Collection<Application> apps) {
    group.getApplications().removeAll(apps);
    apps.forEach(x -> x.getGroups().remove(group));
  }

  private static void associateUsers(@NonNull Group group, @NonNull Collection<User> users) {
    group.getUsers().addAll(users);
    users.stream().map(User::getGroups).forEach(groups -> groups.add(group));
  }

  private static void associateApplications(
      @NonNull Group group, @NonNull Collection<Application> applications) {
    group.getApplications().addAll(applications);
    applications.stream().map(Application::getGroups).forEach(groups -> groups.add(group));
  }

  @Mapper(
      nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
      unmappedTargetPolicy = ReportingPolicy.WARN)
  public abstract static class GroupConverter {

    public abstract Group convertToGroup(GroupRequest request);

    public abstract void updateGroup(Group updatingGroup, @MappingTarget Group groupToUpdate);

    public Group copy(Group groupToCopy) {
      val newGroup = initGroupEntity(Group.class);
      updateGroup(groupToCopy, newGroup);
      return newGroup;
    }

    public abstract void updateGroup(GroupRequest request, @MappingTarget Group groupToUpdate);

    protected Group initGroupEntity(@TargetType Class<Group> groupClass) {
      return Group.builder().build();
    }
  }
}
