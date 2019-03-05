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
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.UserRepository;
import bio.overture.ego.repository.queryspecification.UserSpecification;
import bio.overture.ego.token.IDToken;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static bio.overture.ego.model.enums.UserType.ADMIN;
import static bio.overture.ego.model.enums.UserType.resolveUserTypeIgnoreCase;
import static bio.overture.ego.model.exceptions.NotFoundException.buildNotFoundException;
import static bio.overture.ego.model.exceptions.UniqueViolationException.checkUnique;
import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Converters.convertToUUIDList;
import static bio.overture.ego.utils.Converters.convertToUUIDSet;
import static bio.overture.ego.utils.FieldUtils.onUpdateDetected;
import static bio.overture.ego.utils.Joiners.COMMA;
import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Collections.reverse;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Stream.concat;
import static org.springframework.data.jpa.domain.Specifications.where;

@Slf4j
@Service
@Transactional
public class UserService extends AbstractNamedService<User, UUID> {

  /** Constants */
  public static final UserConverter USER_CONVERTER = Mappers.getMapper(UserConverter.class);

  /** Dependencies */
  private final GroupService groupService;

  private final ApplicationService applicationService;
  private final UserRepository userRepository;

  @Autowired
  public UserService(
      @NonNull UserRepository userRepository,
      @NonNull GroupService groupService,
      @NonNull ApplicationService applicationService) {
    super(User.class, userRepository);
    this.userRepository = userRepository;
    this.groupService = groupService;
    this.applicationService = applicationService;
  }

  // DEFAULTS
  @Value("${default.user.type}")
  private String DEFAULT_USER_TYPE;

  @Value("${default.user.status}")
  private String DEFAULT_USER_STATUS;

  public User create(@NonNull CreateUserRequest request) {
    checkEmailUnique(request.getEmail());
    val user = USER_CONVERTER.convertToUser(request);
    return getRepository().save(user);
  }

  public User createFromIDToken(IDToken idToken) {
    return create(
        CreateUserRequest.builder()
            .email(idToken.getEmail())
            .firstName(idToken.getGiven_name())
            .lastName(idToken.getFamily_name())
            .status(DEFAULT_USER_STATUS)
            .userType(DEFAULT_USER_TYPE)
            .build());
  }

  public User addUserToGroups(@NonNull String userId, @NonNull List<String> groupIDs) {
    val user = getById(fromString(userId));
    val groups = groupService.getMany(convertToUUIDList(groupIDs));
    associateUserWithGroups(user, groups);
    // TODO: @rtisma test setting groups even if there were existing groups before does not delete
    // the existing ones. Becuase the PERSIST and MERGE cascade type is used, this should
    // work
    // correctly
    return getRepository().save(user);
  }

  public User addUserToApps(@NonNull String userId, @NonNull List<String> appIDs) {
    val user = getById(fromString(userId));
    val apps = applicationService.getMany(convertToUUIDList(appIDs));
    associateUserWithApplications(user, apps);
    // TODO: @rtisma test setting apps even if there were existing apps before does not delete the
    // existing ones. Becuase the PERSIST and MERGE cascade applicationType is used, this should
    // work correctly
    return getRepository().save(user);
  }

  private User getUserWithRelationshipsById(@NonNull String id) {
    return userRepository
        .getUserById(fromString(id))
        .orElseThrow(() -> buildNotFoundException("The user could not be found"));
  }

  public User get(@NonNull String userId) {
    return getById(fromString(userId));
  }

  // TODO: [rtisma] remove this method once reactor is removed (EGO-209
  @Deprecated
  public User update(@NonNull User data) {
    val user = getById(data.getId());
    user.setUserType(resolveUserTypeIgnoreCase(data.getUserType()).toString());
    return getRepository().save(user);
  }

  /**
   * Partially updates a user using only non-null {@code UpdateUserRequest} {@param r} object
   *
   * @param r updater
   * @param id updatee
   */
  public User partialUpdate(@NonNull String id, @NonNull UpdateUserRequest r) {
    val user = getById(fromString(id));
    validateUpdateRequest(user, r);
    USER_CONVERTER.updateUser(r, user);
    return getRepository().save(user);
  }

  public Page<User> listUsers(@NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return getRepository().findAll(UserSpecification.filterBy(filters), pageable);
  }

  public Page<User> findUsers(
      @NonNull String query, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return getRepository()
        .findAll(
            where(UserSpecification.containsText(query)).and(UserSpecification.filterBy(filters)),
            pageable);
  }

  // TODO @rtisma: add test for checking group exists for user
  public void deleteUserFromGroups(@NonNull String userId, @NonNull Collection<String> groupIds) {
    val user = getUserWithRelationshipsById(userId);
    val groupIdsToDisassociate = convertToUUIDSet(groupIds);
    checkGroupsExistForUser(user, groupIdsToDisassociate);
    val groupsToDisassociate =
        user.getGroups()
            .stream()
            .filter(g -> groupIdsToDisassociate.contains(g.getId()))
            .collect(toImmutableSet());
    disassociateUserFromGroups(user, groupsToDisassociate);
    getRepository().save(user);
  }

  // TODO @rtisma: add test for all entities to ensure they implement .equals() using only the id
  // field
  // TODO @rtisma: add test for checking user exists
  // TODO @rtisma: add test for checking application exists for a user
  public void deleteUserFromApps(@NonNull String userId, @NonNull Collection<String> appIDs) {
    val user = getUserWithRelationshipsById(userId);
    val appIdsToDisassociate = convertToUUIDSet(appIDs);
    checkApplicationsExistForUser(user, appIdsToDisassociate);
    val appsToDisassociate =
        user.getApplications()
            .stream()
            .filter(a -> appIdsToDisassociate.contains(a.getId()))
            .collect(toImmutableSet());
    disassociateUserFromApplications(user, appsToDisassociate);
    getRepository().save(user);
  }

  public Page<User> findGroupUsers(
      @NonNull String groupId, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return getRepository()
        .findAll(
            where(UserSpecification.inGroup(fromString(groupId)))
                .and(UserSpecification.filterBy(filters)),
            pageable);
  }

  public Page<User> findGroupUsers(
      @NonNull String groupId,
      @NonNull String query,
      @NonNull List<SearchFilter> filters,
      @NonNull Pageable pageable) {
    return getRepository()
        .findAll(
            where(UserSpecification.inGroup(fromString(groupId)))
                .and(UserSpecification.containsText(query))
                .and(UserSpecification.filterBy(filters)),
            pageable);
  }

  public Page<User> findAppUsers(
      @NonNull String appId, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return getRepository()
        .findAll(
            where(UserSpecification.ofApplication(fromString(appId)))
                .and(UserSpecification.filterBy(filters)),
            pageable);
  }

  public Page<User> findAppUsers(
      @NonNull String appId,
      @NonNull String query,
      @NonNull List<SearchFilter> filters,
      @NonNull Pageable pageable) {
    return getRepository()
        .findAll(
            where(UserSpecification.ofApplication(fromString(appId)))
                .and(UserSpecification.containsText(query))
                .and(UserSpecification.filterBy(filters)),
            pageable);
  }

  public void delete(String id) {
    delete(fromString(id));
  }

  // TODO [rtisma]: ensure that the user contains all its relationships
  public static Set<AbstractPermission> resolveFinalPermissions(User user) {
    val up = user.getUserPermissions();
    val upStream = up == null ? Stream.<UserPermission>empty() : up.stream();

    val gp = user.getGroups();
    val gpStream =
        gp == null
            ? Stream.<GroupPermission>empty()
            : gp.stream().map(Group::getPermissions).flatMap(Collection::stream);

    val combinedPermissions =
        concat(upStream, gpStream)
            .filter(a -> a.getPolicy() != null)
            .collect(groupingBy(AbstractPermission::getPolicy));

    return combinedPermissions
        .values()
        .stream()
        .map(UserService::resolvePermissions)
        .collect(toImmutableSet());
  }

  // TODO: [rtisma] this is the old implementation. Ensure there is a test for this, and if there
  // isnt,
  // create one, and ensure the Old and new refactored method are correct
  public static Set<AbstractPermission> getPermissionsListOld(User user) {
    // Get user's individual permission (stream)
    val userPermissions =
        Optional.ofNullable(user.getUserPermissions()).orElse(new HashSet<>()).stream();

    // Get permissions from the user's groups (stream)
    val userGroupsPermissions =
        Optional.ofNullable(user.getGroups())
            .orElse(new HashSet<>())
            .stream()
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
    return mapToSet(resolveFinalPermissions(user), AbstractPermissionService::buildScope);
  }

  public static void associateUserWithGroups(User user, @NonNull Collection<Group> groups) {
    groups.forEach(g -> associateUserWithGroup(user, g));
  }

  public static void associateUserWithGroup(@NonNull User user, @NonNull Group group) {
    user.getGroups().add(group);
    group.getUsers().add(user);
  }

  public static void disassociateUserFromGroups(
      @NonNull User user, @NonNull Collection<Group> groups) {
    user.getGroups().removeAll(groups);
    groups.forEach(x -> x.getUsers().remove(user));
  }

  public static void disassociateUserFromApplications(
      @NonNull User user, @NonNull Collection<Application> applications) {
    user.getApplications().removeAll(applications);
    applications.forEach(x -> x.getUsers().remove(user));
  }

  public static void associateUserWithApplications(
      User user, @NonNull Collection<Application> apps) {
    apps.forEach(a -> associateUserWithApplication(user, a));
  }

  public static void associateUserWithApplication(@NonNull User user, @NonNull Application app) {
    user.getApplications().add(app);
    app.getUsers().add(user);
  }

  public static void checkGroupsExistForUser(
      @NonNull User user, @NonNull Collection<UUID> groupIds) {
    val existingGroupIds = user.getGroups().stream().map(Group::getId).collect(toImmutableSet());
    val nonExistentGroupIds =
        groupIds.stream().filter(x -> !existingGroupIds.contains(x)).collect(toImmutableSet());
    if (!nonExistentGroupIds.isEmpty()) {
      throw new NotFoundException(
          format(
              "The following groups do not exist for user '%s': %s",
              user.getId(), COMMA.join(nonExistentGroupIds)));
    }
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

  private void validateUpdateRequest(User originalUser, UpdateUserRequest r) {
    onUpdateDetected(originalUser.getEmail(), r.getEmail(), () -> checkEmailUnique(r.getEmail()));
    // Ensure type is the right value. This should be removed once Enums are properly
    // used
    onUpdateDetected(
        originalUser.getUserType(),
        r.getUserType(),
        () -> resolveUserTypeIgnoreCase(r.getUserType()));
  }

  private void checkEmailUnique(String email) {
    checkUnique(
        !userRepository.existsByEmailIgnoreCase(email), "A user with same email already exists");
  }

  private static <T extends AbstractPermission> T resolvePermissions(
      List<T> permissions) {
    checkState(!permissions.isEmpty(), "Input permissions list cannot be empty");
    permissions.sort(comparing(AbstractPermission::getAccessLevel));
    reverse(permissions);
    return permissions.get(0);
  }

  @Mapper(
      nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
      unmappedTargetPolicy = ReportingPolicy.WARN)
  public abstract static class UserConverter {

    public abstract User convertToUser(CreateUserRequest request);

    public abstract void updateUser(User updatingUser, @MappingTarget User userToUpdate);

    public abstract void updateUser(
        UpdateUserRequest updateRequest, @MappingTarget User userToUpdate);

    protected User initUserEntity(@TargetType Class<User> userClass) {
      return User.builder().build();
    }

    @AfterMapping
    protected void correctUserData(@MappingTarget User userToUpdate) {
      // Ensure UserType is a correct value
      if (!isNull(userToUpdate.getUserType())) {
        userToUpdate.setUserType(resolveUserTypeIgnoreCase(userToUpdate.getUserType()).toString());
      }

      // Set UserName to equal the email.
      userToUpdate.setName(userToUpdate.getEmail());

      // Set Created At date to Now if not defined
      if (isNull(userToUpdate.getCreatedAt())) {
        userToUpdate.setCreatedAt(new Date());
      }
    }
  }

  public boolean isActiveUser(User user) {
    return resolveUserTypeIgnoreCase(user.getUserType()) == ADMIN;
  }

  public boolean isAdmin(User user) {
    return "admin".equals((user.getUserType().toLowerCase()));
  }
}
