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
import static bio.overture.ego.utils.FieldUtils.onUpdateDetected;
import static bio.overture.ego.utils.Joiners.COMMA;
import static java.lang.String.format;
import static java.util.UUID.fromString;
import static org.mapstruct.factory.Mappers.getMapper;
import static org.springframework.data.jpa.domain.Specifications.where;

import bio.overture.ego.event.CleanupTokenPublisher;
import bio.overture.ego.model.dto.GroupRequest;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.ApplicationRepository;
import bio.overture.ego.repository.GroupRepository;
import bio.overture.ego.repository.UserRepository;
import bio.overture.ego.repository.queryspecification.GroupSpecification;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class GroupService extends AbstractNamedService<Group, UUID> {

  /** Constants */
  private static final GroupConverter GROUP_CONVERTER = getMapper(GroupConverter.class);

  /** Dependencies */
  private final GroupRepository groupRepository;

  private final UserRepository userRepository;
  private final ApplicationRepository applicationRepository;
  private final ApplicationService applicationService;
  private final CleanupTokenPublisher cleanupTokenPublisher;

  @Autowired
  public GroupService(
      @NonNull GroupRepository groupRepository,
      @NonNull UserRepository userRepository,
      @NonNull ApplicationRepository applicationRepository,
      @NonNull ApplicationService applicationService,
      @NonNull CleanupTokenPublisher cleanupTokenPublisher) {
    super(Group.class, groupRepository);
    this.groupRepository = groupRepository;
    this.userRepository = userRepository;
    this.applicationRepository = applicationRepository;
    this.applicationService = applicationService;
    this.cleanupTokenPublisher = cleanupTokenPublisher;
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

  public Group addAppsToGroup(@NonNull UUID id, @NonNull List<UUID> appIds) {
    val group = getById(id);
    val apps = applicationService.getMany(appIds);
    associateApplications(group, apps);
    return getRepository().save(group);
  }

  // TODO: [rtisma] need to validate userIds all exist. Cannot use userService as it causes circular
  // dependency
  public Group addUsersToGroup(@NonNull UUID id, @NonNull List<UUID> userIds) {
    val group = getById(id);
    val users = userRepository.findAllByIdIn(userIds);
    associateUsers(group, users);
    cleanupTokenPublisher.requestTokenCleanup(users);
    return groupRepository.save(group);
  }

  public Group partialUpdate(@NonNull UUID id, @NonNull GroupRequest r) {
    val group = getById(id);
    validateUpdateRequest(group, r);
    GROUP_CONVERTER.updateGroup(r, group);
    return getRepository().save(group);
  }

  public Page<Group> listGroups(@NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return groupRepository.findAll(GroupSpecification.filterBy(filters), pageable);
  }

  public Page<Group> findGroups(
      @NonNull String query, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return groupRepository.findAll(
        where(GroupSpecification.containsText(query)).and(GroupSpecification.filterBy(filters)),
        pageable);
  }

  public Page<Group> findUserGroups(
      @NonNull UUID userId, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return groupRepository.findAll(
        where(GroupSpecification.containsUser(userId)).and(GroupSpecification.filterBy(filters)),
        pageable);
  }

  public Page<Group> findUserGroups(
      @NonNull UUID userId,
      @NonNull String query,
      @NonNull List<SearchFilter> filters,
      @NonNull Pageable pageable) {
    return groupRepository.findAll(
        where(GroupSpecification.containsUser(userId))
            .and(GroupSpecification.containsText(query))
            .and(GroupSpecification.filterBy(filters)),
        pageable);
  }

  public Page<Group> findApplicationGroups(
      @NonNull UUID appId, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return groupRepository.findAll(
        where(GroupSpecification.containsApplication(appId))
            .and(GroupSpecification.filterBy(filters)),
        pageable);
  }

  public Page<Group> findApplicationGroups(
      @NonNull UUID appId,
      @NonNull String query,
      @NonNull List<SearchFilter> filters,
      @NonNull Pageable pageable) {
    return groupRepository.findAll(
        where(GroupSpecification.containsApplication(appId))
            .and(GroupSpecification.containsText(query))
            .and(GroupSpecification.filterBy(filters)),
        pageable);
  }

  public void deleteAppsFromGroup(@NonNull UUID id, @NonNull List<UUID> appIds) {
    val group = getById(id);
    checkAppsExistForGroup(group, appIds);
    val appsToDisassociate =
        group.getApplications().stream()
            .filter(a -> appIds.contains(a.getId()))
            .collect(toImmutableSet());
    val apps = appIds.stream().map(this::retrieveApplication).collect(toImmutableSet());
    disassociateGroupFromApps(group, appsToDisassociate);
    getRepository().save(group);
  }

  public void deleteUsersFromGroup(@NonNull UUID id, @NonNull List<UUID> userIds) {
    val group = getById(id);
    checkUsersExistForGroup(group, userIds);
    val usersToDisassociate =
        group.getUsers().stream()
            .filter(u -> userIds.contains(u.getId()))
            .collect(toImmutableSet());
    disassociateGroupFromUsers(group, usersToDisassociate);
    getRepository().save(group);
    cleanupTokenPublisher.requestTokenCleanup(usersToDisassociate);
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

  private Application retrieveApplication(UUID appId) {
    // using applicationRepository since using applicationService causes cyclic dependency error
    return applicationRepository
        .findById(appId)
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
