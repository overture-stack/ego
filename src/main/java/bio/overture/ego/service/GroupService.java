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
import static bio.overture.ego.utils.CollectionUtils.difference;
import static bio.overture.ego.utils.CollectionUtils.intersection;
import static bio.overture.ego.utils.CollectionUtils.mapToImmutableSet;
import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Converters.convertToIds;
import static bio.overture.ego.utils.Converters.convertToUserGroup;
import static bio.overture.ego.utils.EntityServices.getManyEntities;
import static bio.overture.ego.utils.FieldUtils.onUpdateDetected;
import static bio.overture.ego.utils.Ids.checkDuplicates;
import static bio.overture.ego.utils.Joiners.COMMA;
import static bio.overture.ego.utils.Joiners.PRETTY_COMMA;
import static java.lang.String.format;
import static org.mapstruct.factory.Mappers.getMapper;
import static org.springframework.data.jpa.domain.Specification.where;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.FieldUtils.onUpdateDetected;
import static bio.overture.ego.utils.Joiners.COMMA;
import static java.lang.String.format;
import static java.util.UUID.fromString;
import static org.mapstruct.factory.Mappers.getMapper;
import static org.springframework.data.jpa.domain.Specifications.where;

import bio.overture.ego.event.token.TokenEventsPublisher;
import bio.overture.ego.model.dto.GroupRequest;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.model.join.UserGroup;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.GroupRepository;
import bio.overture.ego.repository.UserRepository;
import bio.overture.ego.repository.queryspecification.GroupSpecification;
import bio.overture.ego.repository.queryspecification.UserSpecification;
import bio.overture.ego.repository.queryspecification.builder.GroupSpecificationBuilder;
import bio.overture.ego.utils.EntityServices;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.transaction.Transactional;
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
@Transactional
public class GroupService extends AbstractNamedService<Group, UUID> {

  /** Constants */
  private static final GroupConverter GROUP_CONVERTER = getMapper(GroupConverter.class);

  /** Dependencies */
  private final GroupRepository groupRepository;

  private final UserRepository userRepository;
  private final ApplicationService applicationService;
  private final TokenEventsPublisher tokenEventsPublisher;

  @Autowired
  public GroupService(
      @NonNull GroupRepository groupRepository,
      @NonNull UserRepository userRepository,
      @NonNull ApplicationService applicationService,
      @NonNull TokenEventsPublisher tokenEventsPublisher) {
    super(Group.class, groupRepository);
    this.groupRepository = groupRepository;
    this.applicationService = applicationService;
    this.tokenEventsPublisher = tokenEventsPublisher;
    this.userRepository = userRepository;
  }

  @Override
  public Optional<Group> findByName(@NonNull String name) {
    return (Optional<Group>)
        getRepository()
            .findOne(
                new GroupSpecificationBuilder()
                    .fetchApplications(true)
                    .fetchUserGroups(true)
                    .fetchGroupPermissions(true)
                    .buildByNameIgnoreCase(name));
  }

  public Group create(@NonNull GroupRequest request) {
    checkNameUnique(request.getName());
    val group = GROUP_CONVERTER.convertToGroup(request);
    return getRepository().save(group);
  }

  /**
   * Decorate the delete method for group's users to also trigger a token check after group delete.
   *
   * @param groupId The ID of the group to be deleted.
   */
  public void delete(@NonNull UUID groupId) {
    val group = getWithRelationships(groupId);
    val users = mapToSet(group.getUserGroups(), UserGroup::getUser);
    disassociateAllUsersFromGroup(group);
    disassociateAllApplicationsFromGroup(group);
    tokenEventsPublisher.requestTokenCleanupByUsers(users);
    super.delete(groupId);
  }


  public Group getWithRelationships(@NonNull UUID id) {
    return get(id, true, true, true);
  }

  private Group getWithUserGroups(@NonNull UUID id) {
    return get(id, false, true, false);
  }

  public void disassociateUsersFromGroup(@NonNull UUID id, @NonNull Collection<UUID> userIds) {
    // check duplicate userIds
    checkDuplicates(User.class, userIds);

    // Get existing associated child ids with the parent
    val groupWithUserGroups = getWithUserGroups(id);
    val users = mapToImmutableSet(groupWithUserGroups.getUserGroups(), UserGroup::getUser);
    val existingAssociatedUserIds = convertToIds(users);

    // Get existing and non-existing non-associated user ids. Error out if there are existing and
    // non-existing non-associated user ids
    val nonAssociatedUserIds = difference(userIds, existingAssociatedUserIds);
    if (!nonAssociatedUserIds.isEmpty()) {
      EntityServices.checkEntityExistence(User.class, userRepository, nonAssociatedUserIds);
      throw buildNotFoundException(
          "The following existing %s ids cannot be disassociated from %s '%s' "
              + "because they are not associated with it",
          User.class.getSimpleName(), getEntityTypeName(), id);
    }

    // Since all user ids exist and are associated with the group, disassociate them from
    // eachother
    val userIdsToDisassociate = ImmutableSet.copyOf(userIds);
    val userGroupsToDisassociate =
        groupWithUserGroups.getUserGroups().stream()
            .filter(ug -> userIdsToDisassociate.contains(ug.getId().getUserId()))
            .collect(toImmutableSet());

    disassociateUserGroupsFromGroup(groupWithUserGroups, userGroupsToDisassociate);
    tokenEventsPublisher.requestTokenCleanupByUsers(users);
  }

  public Group associateUsersWithGroup(@NonNull UUID id, @NonNull Collection<UUID> userIds) {
    // check duplicate userIds
    checkDuplicates(User.class, userIds);

    // Get existing associated user ids with the group
    val groupWithUserGroups = getWithUserGroups(id);
    val users = mapToImmutableSet(groupWithUserGroups.getUserGroups(), UserGroup::getUser);
    val existingAssociatedUserIds = convertToIds(users);

    // Check there are no user ids that are already associated with the group
    val existingAlreadyAssociatedUserIds = intersection(existingAssociatedUserIds, userIds);
    checkUnique(
        existingAlreadyAssociatedUserIds.isEmpty(),
        "The following %s ids are already associated with %s '%s': [%s]",
        User.class.getSimpleName(),
        getEntityTypeName(),
        id,
        PRETTY_COMMA.join(existingAlreadyAssociatedUserIds));

    // Get all unassociated user ids. If they do not exist, an error is thrown
    val nonAssociatedUserIds = difference(userIds, existingAssociatedUserIds);
    val nonAssociatedUsers = getManyEntities(User.class, userRepository, nonAssociatedUserIds);

    // Associate the existing users with the group
    nonAssociatedUsers.stream()
        .map(u -> convertToUserGroup(u, groupWithUserGroups))
        .forEach(UserGroupService::associateSelf);
    tokenEventsPublisher.requestTokenCleanupByUsers(users);
    return groupWithUserGroups;
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

  public Group addAppsToGroup(@NonNull UUID id, @NonNull List<UUID> appIds) {
    val group = getById(id);
    val apps = applicationService.getMany(appIds);
    associateApplicationsWithGroup(group, apps);
    return getRepository().save(group);
  }

  public void deleteAppsFromGroup(@NonNull UUID id, @NonNull List<UUID> appIds) {
    val group = getById(id);
    checkAppsExistForGroup(group, appIds);
    val appsToDisassociate =
        group.getApplications().stream()
            .filter(a -> appIds.contains(a.getId()))
            .collect(toImmutableSet());
    disassociateApplicationsFromGroup(group, appsToDisassociate);
    getRepository().save(group);
  }

  public Page<User> findUsersForGroup(
      @NonNull UUID id, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    checkExistence(id);
    return userRepository.findAll(
        where(UserSpecification.inGroup(id)).and(UserSpecification.filterBy(filters)), pageable);
  }

  public Page<User> findUsersForGroup(
      @NonNull UUID id,
      @NonNull String query,
      @NonNull List<SearchFilter> filters,
      @NonNull Pageable pageable) {
    checkExistence(id);
    return userRepository.findAll(
        where(UserSpecification.inGroup(id))
            .and(UserSpecification.containsText(query))
            .and(UserSpecification.filterBy(filters)),
        pageable);
  }

  private Group get(
      UUID id, boolean fetchApplications, boolean fetchUserGroups, boolean fetchGroupPermissions) {
    val result =
        (Optional<Group>)
            getRepository()
                .findOne(
                    new GroupSpecificationBuilder()
                        .fetchGroupPermissions(fetchGroupPermissions)
                        .fetchUserGroups(fetchUserGroups)
                        .fetchApplications(fetchApplications)
                        .buildById(id));
    checkNotFound(result.isPresent(), "The groupId '%s' does not exist", id);
    return result.get();
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

  public static void disassociateUserGroupsFromGroup(
      @NonNull Group g, @NonNull Collection<UserGroup> userGroups) {
    userGroups.forEach(
        ug -> {
          ug.getUser().getUserGroups().remove(ug);
          ug.setUser(null);
          ug.setGroup(null);
        });
    g.getUserGroups().removeAll(userGroups);
  }

  public static void disassociateAllUsersFromGroup(@NonNull Group g) {
    val userGroups = g.getUserGroups();
    disassociateUserGroupsFromGroup(g, userGroups);
  }

  public static void disassociateAllApplicationsFromGroup(@NonNull Group g) {
    g.getApplications().forEach(a -> a.getGroups().remove(g));
    g.getApplications().clear();
  }

  public static void disassociateApplicationsFromGroup(
      @NonNull Group group, @NonNull Collection<Application> apps) {
    group.getApplications().removeAll(apps);
    apps.forEach(x -> x.getGroups().remove(group));
  }

  public static void associateApplicationsWithGroup(
      @NonNull Group group, @NonNull Collection<Application> applications) {
    group.getApplications().addAll(applications);
    applications.stream().map(Application::getGroups).forEach(groups -> groups.add(group));
  }

  private static void checkAppsExistForGroup(
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
