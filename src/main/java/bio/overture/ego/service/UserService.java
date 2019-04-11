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

import bio.overture.ego.config.UserDefaultsConfig;
import bio.overture.ego.event.token.TokenEventsPublisher;
import bio.overture.ego.model.dto.CreateUserRequest;
import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.dto.UpdateUserRequest;
import bio.overture.ego.model.entity.AbstractPermission;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.model.join.UserGroup;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.GroupRepository;
import bio.overture.ego.repository.UserRepository;
import bio.overture.ego.repository.queryspecification.UserSpecification;
import bio.overture.ego.repository.queryspecification.builder.UserSpecificationBuilder;
import bio.overture.ego.token.IDToken;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.TargetType;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static bio.overture.ego.model.enums.UserType.ADMIN;
import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.model.exceptions.RequestValidationException.checkRequestValid;
import static bio.overture.ego.model.exceptions.UniqueViolationException.checkUnique;
import static bio.overture.ego.service.AbstractPermissionService.resolveFinalPermissions;
import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Converters.convertToUserGroup;
import static bio.overture.ego.utils.EntityServices.checkEntityExistence;
import static bio.overture.ego.utils.FieldUtils.onUpdateDetected;
import static bio.overture.ego.utils.Joiners.COMMA;
import static java.lang.String.format;
import static java.util.Collections.reverse;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Stream.concat;
import static org.springframework.data.jpa.domain.Specification.where;

@Slf4j
@Service
@Transactional
public class UserService extends AbstractNamedService<User, UUID> {

  /** Constants */
  public static final UserConverter USER_CONVERTER = Mappers.getMapper(UserConverter.class);

  /** Dependencies */
  private final GroupRepository groupRepository;

  private final TokenEventsPublisher tokenEventsPublisher;
  private final ApplicationService applicationService;
  private final UserRepository userRepository;

  /** Configuration */
  private final UserDefaultsConfig userDefaultsConfig;

  @Autowired
  public UserService(
      @NonNull UserRepository userRepository,
      @NonNull GroupRepository groupRepository,
      @NonNull ApplicationService applicationService,
      @NonNull UserDefaultsConfig userDefaultsConfig,
      @NonNull TokenEventsPublisher tokenEventsPublisher) {
    super(User.class, userRepository);
    this.userRepository = userRepository;
    this.groupRepository = groupRepository;
    this.applicationService = applicationService;
    this.userDefaultsConfig = userDefaultsConfig;
    this.tokenEventsPublisher = tokenEventsPublisher;
  }

  @Override
  public void delete(@NonNull UUID id) {
    val user = getWithRelationships(id);
    disassociateAllGroupsFromUser(user);
    disassociateAllApplicationsFromUser(user);
    tokenEventsPublisher.requestTokenCleanupByUsers(ImmutableSet.of(user));
    super.delete(id);
  }

  public User create(@NonNull CreateUserRequest request) {
    validateCreateRequest(request);
    val user = USER_CONVERTER.convertToUser(request);
    return getRepository().save(user);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Optional<User> findByName(String name) {
    return (Optional<User>)
        getRepository()
            .findOne(
                new UserSpecificationBuilder()
                    .fetchApplications(true)
                    .fetchUserGroups(true)
                    .fetchUserPermissions(true)
                    .buildByNameIgnoreCase(name));
  }

  @SuppressWarnings("unchecked")
  public User get(
      @NonNull UUID id,
      boolean fetchUserPermissions,
      boolean fetchUserGroups,
      boolean fetchApplications) {
    val result =
        (Optional<User>)
            getRepository()
                .findOne(
                    new UserSpecificationBuilder()
                        .fetchUserPermissions(fetchUserPermissions)
                        .fetchUserGroups(fetchUserGroups)
                        .fetchApplications(fetchApplications)
                        .buildById(id));
    checkNotFound(result.isPresent(), "The userId '%s' does not exist", id);
    return result.get();
  }

  public User createFromIDToken(IDToken idToken) {
    return create(
        CreateUserRequest.builder()
            .email(idToken.getEmail())
            .firstName(idToken.getGiven_name())
            .lastName(idToken.getFamily_name())
            .status(userDefaultsConfig.getDefaultUserStatus())
            .type(userDefaultsConfig.getDefaultUserType())
            .build());
  }

  public User addUserToApps(@NonNull UUID id, @NonNull List<UUID> appIds) {
    val user = getById(id);
    val apps = applicationService.getMany(appIds);
    associateUserWithApplications(user, apps);
    // TODO: @rtisma test setting apps even if there were existing apps before does not delete the
    // existing ones. Becuase the PERSIST and MERGE cascade applicationType is used, this should
    // work correctly
    return getRepository().save(user);
  }

  @Override
  public User getWithRelationships(@NonNull UUID id) {
    return get(id, true, true, true);
  }

  /**
   * Partially updates a user using only non-null {@code UpdateUserRequest} {@param r} object
   *
   * @param r updater
   * @param id updatee
   */
  public User partialUpdate(@NonNull UUID id, @NonNull UpdateUserRequest r) {
    val user = getById(id);
    validateUpdateRequest(user, r);
    USER_CONVERTER.updateUser(r, user);
    return getRepository().save(user);
  }

  @SuppressWarnings("unchecked")
  public Page<User> listUsers(@NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return getRepository().findAll(UserSpecification.filterBy(filters), pageable);
  }

  @SuppressWarnings("unchecked")
  public Page<User> findUsers(
      @NonNull String query, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return getRepository()
        .findAll(
            where(UserSpecification.containsText(query)).and(UserSpecification.filterBy(filters)),
            pageable);
  }

  public void disassociateGroupsFromUser(@NonNull UUID id, @NonNull Collection<UUID> groupIds) {
    val userWithRelationships = get(id, false, true, false);
    val userGroupsToDisassociate =
        userWithRelationships.getUserGroups().stream()
            .filter(x -> groupIds.contains(x.getId().getGroupId()))
            .collect(toImmutableSet());
    disassociateUserGroupsFromUser(userWithRelationships, userGroupsToDisassociate);
    tokenEventsPublisher.requestTokenCleanupByUsers(ImmutableSet.of(userWithRelationships));
  }

  public User associateGroupsWithUser(@NonNull UUID id, @NonNull Collection<UUID> groupIds) {
    val user = getWithRelationships(id);
    val groups = groupRepository.findAllByIdIn(groupIds);
    groups.stream().map(g -> convertToUserGroup(user, g)).forEach(UserGroupService::associateSelf);
    tokenEventsPublisher.requestTokenCleanupByUsers(ImmutableSet.of(user));
    return user;
  }

  // TODO @rtisma: add test for all entities to ensure they implement .equals() using only the id
  // field
  // TODO @rtisma: add test for checking user exists
  // TODO @rtisma: add test for checking application exists for a user
  public void deleteUserFromApps(@NonNull UUID id, @NonNull Collection<UUID> appIds) {
    val user = getWithRelationships(id);
    checkApplicationsExistForUser(user, appIds);
    val appsToDisassociate =
        user.getApplications().stream()
            .filter(a -> appIds.contains(a.getId()))
            .collect(toImmutableSet());
    disassociateUserFromApplications(user, appsToDisassociate);
    getRepository().save(user);
  }

  @SuppressWarnings("unchecked")
  public Page<User> findUsersForGroup(
      @NonNull UUID groupId, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    checkEntityExistence(Group.class, groupRepository, groupId);
    return userRepository.findAll(
        where(UserSpecification.inGroup(groupId)).and(UserSpecification.filterBy(filters)),
        pageable);
  }

  @SuppressWarnings("unchecked")
  public Page<User> findUsersForGroup(
      @NonNull UUID groupId,
      @NonNull String query,
      @NonNull List<SearchFilter> filters,
      @NonNull Pageable pageable) {
    checkEntityExistence(Group.class, groupRepository, groupId);
    return userRepository.findAll(
        where(UserSpecification.inGroup(groupId))
            .and(UserSpecification.containsText(query))
            .and(UserSpecification.filterBy(filters)),
        pageable);
  }

  @SuppressWarnings("unchecked")
  public Page<User> findUsersForApplication(
      @NonNull UUID appId, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    applicationService.checkExistence(appId);
    return getRepository()
        .findAll(
            where(UserSpecification.ofApplication(appId)).and(UserSpecification.filterBy(filters)),
            pageable);
  }

  @SuppressWarnings("unchecked")
  public Page<User> findUsersForApplication(
      @NonNull UUID appId,
      @NonNull String query,
      @NonNull List<SearchFilter> filters,
      @NonNull Pageable pageable) {
    applicationService.checkExistence(appId);
    return getRepository()
        .findAll(
            where(UserSpecification.ofApplication(appId))
                .and(UserSpecification.containsText(query))
                .and(UserSpecification.filterBy(filters)),
            pageable);
  }

  private void validateCreateRequest(CreateUserRequest r) {
    checkRequestValid(r);
    checkEmailUnique(r.getEmail());
  }

  private void validateUpdateRequest(User originalUser, UpdateUserRequest r) {
    onUpdateDetected(originalUser.getEmail(), r.getEmail(), () -> checkEmailUnique(r.getEmail()));
  }

  private void checkEmailUnique(String email) {
    checkUnique(
        !userRepository.existsByEmailIgnoreCase(email), "A user with same email already exists");
  }

  @SuppressWarnings("unchecked")
  public static Set<AbstractPermission> resolveUsersPermissions(User user) {
    val up = user.getUserPermissions();
    Collection<UserPermission> userPermissions = isNull(up) ? ImmutableList.of() : up;

    val userGroups = user.getUserGroups();

    Collection<GroupPermission> groupPermissions =
        isNull(userGroups)
            ? ImmutableList.of()
            : userGroups.stream()
                .map(UserGroup::getGroup)
                .map(Group::getPermissions)
                .flatMap(Collection::stream)
                .collect(toImmutableSet());
    return resolveFinalPermissions(userPermissions, groupPermissions);
  }

  // TODO: [rtisma] this is the old implementation. Ensure there is a test for this, and if there
  // isnt,
  // create one, and ensure the Old and new refactored method are correct
  @Deprecated
  public static Set<AbstractPermission> getPermissionsListOld(User user) {
    // Get user's individual permission (stream)
    val userPermissions =
        Optional.ofNullable(user.getUserPermissions()).orElse(new HashSet<>()).stream();

    // Get permissions from the user's groups (stream)
    val userGroupsPermissions =
        Optional.ofNullable(user.getUserGroups()).orElse(new HashSet<>()).stream()
            .map(UserGroup::getGroup)
            .map(Group::getPermissions)
            .flatMap(Collection::stream);

    // Combine individual user permissions and the user's
    // groups (if they have any) permissions
    val combinedPermissions =
        concat(userPermissions, userGroupsPermissions)
            .collect(groupingBy(AbstractPermission::getPolicy));
    // If we have no permissions at all return an empty list
    if (combinedPermissions.values().size() == 0) {
      return new HashSet<>();
    }

    // If we do have permissions ... sort the grouped permissions (by PolicyIdStringWithMaskName)
    // on PolicyMask, extracting the first value of the sorted list into the final
    // permissions list
    HashSet<AbstractPermission> finalPermissionsList = new HashSet<>();

    combinedPermissions.forEach(
        (entity, permissions) -> {
          permissions.sort(comparing(AbstractPermission::getAccessLevel));
          reverse(permissions);
          finalPermissionsList.add(permissions.get(0));
        });
    return finalPermissionsList;
  }

  public static Set<Scope> extractScopes(@NonNull User user) {
    return mapToSet(resolveUsersPermissions(user), AbstractPermissionService::buildScope);
  }

  public static void disassociateUserFromApplications(
      @NonNull User user, @NonNull Collection<Application> applications) {
    user.getApplications().removeAll(applications);
    applications.forEach(x -> x.getUsers().remove(user));
  }

  public static void associateUserWithApplications(
      @NonNull User user, @NonNull Collection<Application> apps) {
    apps.forEach(a -> associateUserWithApplication(user, a));
  }

  public static void associateUserWithApplication(@NonNull User user, @NonNull Application app) {
    user.getApplications().add(app);
    app.getUsers().add(user);
  }

  public static void disassociateAllApplicationsFromUser(@NonNull User user) {
    user.getApplications().forEach(x -> x.getUsers().remove(user));
    user.getApplications().clear();
  }

  public static void disassociateAllGroupsFromUser(@NonNull User userWithRelationships) {
    disassociateUserGroupsFromUser(userWithRelationships, userWithRelationships.getUserGroups());
  }

  public static void disassociateUserGroupsFromUser(
      @NonNull User user, @NonNull Collection<UserGroup> userGroups) {
    userGroups.forEach(
        ug -> {
          ug.getGroup().getUserGroups().remove(ug);
          ug.setUser(null);
          ug.setGroup(null);
        });
    user.getUserGroups().removeAll(userGroups);
  }

  public static void checkApplicationsExistForUser(
      @NonNull User user, @NonNull Collection<UUID> appIds) {
    val existingAppIds =
        user.getApplications().stream().map(Application::getId).collect(toImmutableSet());
    val nonExistentAppIds =
        appIds.stream().filter(x -> !existingAppIds.contains(x)).collect(toImmutableSet());
    if (!nonExistentAppIds.isEmpty()) {
      throw new NotFoundException(
          format(
              "The following applications do not exist for user '%s': %s",
              user.getId(), COMMA.join(nonExistentAppIds)));
    }
  }

  @Mapper(
      nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
      unmappedTargetPolicy = ReportingPolicy.WARN)
  public abstract static class UserConverter {

    public abstract User convertToUser(CreateUserRequest request);

    public abstract void updateUser(User updatingUser, @MappingTarget User userToUpdate);

    public abstract UpdateUserRequest convertToUpdateRequest(User user);

    public abstract void updateUser(
        UpdateUserRequest updateRequest, @MappingTarget User userToUpdate);

    protected User initUserEntity(@TargetType Class<User> userClass) {
      return User.builder().build();
    }

    @AfterMapping
    protected void correctUserData(@MappingTarget User userToUpdate) {
      // Set UserName to equal the email.
      userToUpdate.setName(userToUpdate.getEmail());

      // Set Created At date to Now if not defined
      if (isNull(userToUpdate.getCreatedAt())) {
        userToUpdate.setCreatedAt(new Date());
      }
    }
  }

  public boolean isActiveUser(User user) {
    return isAdmin(user);
  }

  public boolean isAdmin(User user) {
    return user.getType() == ADMIN;
  }
}
