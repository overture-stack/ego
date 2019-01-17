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
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.enums.EntityStatus;
import bio.overture.ego.model.enums.UserRole;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.model.params.PolicyIdStringWithAccessLevel;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.UserRepository;
import bio.overture.ego.repository.queryspecification.UserSpecification;
import bio.overture.ego.token.IDToken;
import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static bio.overture.ego.model.enums.UserRole.resolveUserRoleIgnoreCase;
import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Converters.convertToUUIDList;
import static bio.overture.ego.utils.Converters.convertToUUIDSet;
import static bio.overture.ego.utils.Converters.nonNullAcceptor;
import static bio.overture.ego.utils.Joiners.COMMA;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Stream.concat;
import static org.springframework.data.jpa.domain.Specifications.where;

@Slf4j
@Service
@Transactional
public class UserService extends AbstractNamedService<User, UUID> {

  // DEMO USER
  private static final String DEMO_USER_NAME = "Demo.User@example.com";
  private static final String DEMO_USER_EMAIL = "Demo.User@example.com";
  private static final String DEMO_FIRST_NAME = "Demo";
  private static final String DEMO_LAST_NAME = "User";
  private static final String DEMO_USER_ROLE = UserRole.ADMIN.toString();
  private static final String DEMO_USER_STATUS = EntityStatus.APPROVED.toString();

  /*
   Dependencies
  */
  private final GroupService groupService;
  private final ApplicationService applicationService;
  private final PolicyService policyService;
  private final UserPermissionService userPermissionService;
  private final UserRepository userRepository;

  @Autowired
  public UserService(
      @NonNull UserRepository userRepository,
      @NonNull GroupService groupService,
      @NonNull ApplicationService applicationService,
      @NonNull PolicyService policyService,
      @NonNull UserPermissionService userPermissionService) {
    super(User.class, userRepository);
    this.userRepository = userRepository;
    this.groupService = groupService;
    this.applicationService = applicationService;
    this.policyService = policyService;
    this.userPermissionService = userPermissionService;
  }

  /*
   Constants
  */
  // DEFAULTS
  @Value("${default.user.role}")
  private String DEFAULT_USER_ROLE;

  @Value("${default.user.status}")
  private String DEFAULT_USER_STATUS;

  public User create(@NonNull CreateUserRequest request) {
    return getRepository().save(convertToUser(request));
  }

  public User createFromIDToken(IDToken idToken) {
    return create(
        CreateUserRequest.builder()
            .email(idToken.getEmail())
            .firstName(StringUtils.isEmpty(idToken.getGiven_name()) ? "" : idToken.getGiven_name())
            .lastName(StringUtils.isEmpty(idToken.getFamily_name()) ? "" : idToken.getFamily_name())
            .status(DEFAULT_USER_STATUS)
            .role(DEFAULT_USER_ROLE)
            .build());
  }

  public User getOrCreateDemoUser() {
    return userRepository
        .getUserByNameIgnoreCase(DEMO_USER_NAME)
        .map(
            u -> {
              u.setStatus(DEMO_USER_STATUS);
              u.setRole(DEMO_USER_ROLE);
              return getRepository().save(u);
            })
        .orElseGet(
            () ->
                create(
                    CreateUserRequest.builder()
                        .email(DEMO_USER_EMAIL)
                        .firstName(DEMO_FIRST_NAME)
                        .lastName(DEMO_LAST_NAME)
                        .status(EntityStatus.APPROVED.toString())
                        .role(UserRole.ADMIN.toString())
                        .build()));
  }

  public User addUserToGroups(@NonNull String userId, @NonNull List<String> groupIDs) {
    val user = getById(fromString(userId));
    val groups = groupService.getMany(convertToUUIDList(groupIDs));
    associateUserWithGroups(user, groups);
    // TODO: @rtisma test setting groups even if there were existing groups before does not delete
    // the existing ones. Becuase the PERSIST and MERGE cascade type is used, this should work
    // correctly
    return getRepository().save(user);
  }


  public User addUserToApps(@NonNull String userId, @NonNull List<String> appIDs) {
    val user = getById(fromString(userId));
    val apps = applicationService.getMany(convertToUUIDList(appIDs));
    associateUserWithApplications(user, apps);
    // TODO: @rtisma test setting apps even if there were existing apps before does not delete the
    // existing ones. Becuase the PERSIST and MERGE cascade type is used, this should work correctly
    return getRepository().save(user);
  }

  public User addUserPermission(String userId, @NonNull PolicyIdStringWithAccessLevel policy) {
    return addUserPermissions(userId, newArrayList(policy));
  }

  public User addUserPermissions(
      @NonNull String userId, @NonNull List<PolicyIdStringWithAccessLevel> permissions) {
    val policyMap = permissions.stream().collect(groupingBy(x -> fromString(x.getPolicyId())));
    val user = getById(fromString(userId));
    policyService
        .getMany(ImmutableList.copyOf(policyMap.keySet()))
        .stream()
        .flatMap(p -> streamUserPermission(user, policyMap, p))
        .map(userPermissionService::create)
        .forEach(p -> associateUserWithPermission(user, p));
    return getRepository().save(user);
  }

  @Deprecated
  public User get(@NonNull String userId) {
    return getById(fromString(userId));
  }

  // TODO: [rtisma] remove this method once reactor is removed (EGO-209
  @Deprecated
  public User update(@NonNull User data) {
    val user = getById(data.getId());
    user.setRole(resolveUserRoleIgnoreCase(data.getRole()).toString());
    return getRepository().save(user);
  }

  public User partialUpdate(@NonNull String id, UpdateUserRequest r) {
    return partialUpdate(fromString(id), r);
  }

  public User partialUpdate(@NonNull UUID id, @NonNull UpdateUserRequest r) {
    val user = getById(id);
    partialUpdateUser(user, r);
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
    val user = getById(fromString(userId));
    val groupUUIDs = convertToUUIDSet(groupIds);
    checkGroupsExistForUser(user, groupUUIDs);
    user.getGroups().removeIf(x -> groupUUIDs.contains(x.getId()));
    getRepository().save(user);
  }

  // TODO @rtisma: add test for all entities to ensure they implement .equals() using only the id
  // field
  // TODO @rtisma: add test for checking user exists
  // TODO @rtisma: add test for checking application exists for a user
  public void deleteUserFromApps(@NonNull String userId, @NonNull Collection<String> appIDs) {
    val user = getById(fromString(userId));
    val appUUIDs = convertToUUIDSet(appIDs);
    checkApplicationsExistForUser(user, appUUIDs);
    user.getApplications().removeIf(x -> appUUIDs.contains(x.getId()));
    getRepository().save(user);
  }

  // TODO @rtisma: add test for checking user permission exists for user
  public void deleteUserPermissions(
      @NonNull String userId, @NonNull Collection<String> permissionsIds) {
    val user = getById(fromString(userId));
    val permUUIDs = convertToUUIDSet(permissionsIds);
    checkPermissionsExistForUser(user, permUUIDs);
    user.getUserPermissions().removeIf(x -> permUUIDs.contains(x.getId()));
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

  public Page<UserPermission> getUserPermissions(
      @NonNull String userId, @NonNull Pageable pageable) {
    val userPermissions = ImmutableList.copyOf(getById(fromString(userId)).getUserPermissions());
    return new PageImpl<>(userPermissions, pageable, userPermissions.size());
  }

  public void delete(String id) {
    delete(fromString(id));
  }

  public static Set<AbstractPermission> getPermissionsList(User user) {
    val upStream = user.getUserPermissions().stream();
    val gpStream = user.getGroups().stream().map(Group::getPermissions).flatMap(Collection::stream);
    val combinedPermissions = concat(upStream, gpStream).collect(groupingBy(AbstractPermission::getPolicy));

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
        concat(userPermissions, userGroupsPermissions).collect(groupingBy(AbstractPermission::getPolicy));
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
          permissions.sort(comparing(AbstractPermission::getAccessLevel).reversed());
          finalPermissionsList.add(permissions.get(0));
        });
    return finalPermissionsList;
  }

  public static Set<Scope> extractScopes(@NonNull User user) {
    return mapToSet(getPermissionsList(user), AbstractPermissionService::buildScope);
  }

  public static void associateUserWithPermissions(
      User user, @NonNull Collection<UserPermission> permissions) {
    permissions.forEach(p -> associateUserWithPermission(user, p));
  }

  public static void associateUserWithPermission(
      @NonNull User user, @NonNull UserPermission permission) {
    user.getUserPermissions().add(permission);
    permission.setOwner(user);
  }

  public static void associateUserWithGroups(User user, @NonNull Collection<Group> groups) {
    groups.forEach(g -> associateUserWithGroup(user, g));
  }

  public static void associateUserWithGroup(@NonNull User user, @NonNull Group group) {
    user.getGroups().add(group);
    group.getUsers().add(user);
  }

  public static void associateUserWithApplications(
      User user, @NonNull Collection<Application> apps) {
    apps.forEach(a -> associateUserWithApplication(user, a));
  }

  public static void associateUserWithApplication(@NonNull User user, @NonNull Application app) {
    user.getApplications().add(app);
    app.getUsers().add(user);
  }

  /**
   * Partially updates the {@param user} using only non-null {@code UpdateUserRequest} object
   *
   * @param user updatee
   * @param r updater
   */
  public static void partialUpdateUser(@NonNull User user, @NonNull UpdateUserRequest r) {
    nonNullAcceptor(r.getRole(), x -> user.setRole(resolveUserRoleIgnoreCase(x).toString()));
    nonNullAcceptor(r.getFirstName(), user::setFirstName);
    nonNullAcceptor(r.getLastLogin(), user::setLastLogin);
    nonNullAcceptor(r.getLastName(), user::setLastName);
    nonNullAcceptor(r.getEmail(), user::setEmail);
    nonNullAcceptor(r.getPreferredLanguage(), user::setPreferredLanguage);
    nonNullAcceptor(r.getStatus(), user::setStatus);
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

  public static void checkPermissionsExistForUser(
      @NonNull User user, @NonNull Collection<UUID> permissionIds) {
    val existingPermIds =
        user.getUserPermissions().stream().map(UserPermission::getId).collect(toImmutableSet());
    val nonExistentPermIds =
        permissionIds.stream().filter(x -> !existingPermIds.contains(x)).collect(toImmutableSet());
    if (!nonExistentPermIds.isEmpty()) {
      throw new NotFoundException(
          format(
              "The following user permissions do not exist for user '%s': %s",
              user.getId(), COMMA.join(nonExistentPermIds)));
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

  private static <T extends AbstractPermission> T resolvePermissions(List<T> permissions) {
    checkState(!permissions.isEmpty(), "Input permissions list cannot be empty");
    permissions.sort(comparing(AbstractPermission::getAccessLevel).reversed());
    return permissions.get(0);
  }

  private static Stream<UserPermission> streamUserPermission(
      User u, Map<UUID, List<PolicyIdStringWithAccessLevel>> policyMap, Policy p) {
    val policyId = p.getId();
    return policyMap
        .get(policyId)
        .stream()
        .map(PolicyIdStringWithAccessLevel::getMask)
        .map(AccessLevel::fromValue)
        .map(a -> {
          val up = new UserPermission();
          up.setAccessLevel(a);
          up.setPolicy(p);
          up.setOwner(u);
          return up;
        });
  }

  private static User convertToUser(CreateUserRequest request) {
    return User.builder()
        .preferredLanguage(request.getPreferredLanguage())
        .email(request.getEmail())
        // Set UserName to equal the email.
        .name(request.getEmail())
        // Set Created At date to Now
        .createdAt(new Date())
        .firstName(request.getFirstName())
        .lastName(request.getLastName())
        .role(request.getRole())
        .status(request.getStatus())
        .build();
  }

}
