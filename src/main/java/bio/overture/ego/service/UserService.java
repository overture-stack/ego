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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Converters.convertToUUIDSet;
import static bio.overture.ego.utils.Joiners.COMMA;
import static java.lang.String.format;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.groupingBy;
import static org.springframework.data.jpa.domain.Specifications.where;

@Slf4j
@Service
@Transactional
public class UserService extends AbstractNamedService<User> {

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

  public User create(@NonNull User userInfo) {
    // Set Created At date to Now
    userInfo.setCreatedAt(new Date());

    // Set UserName to equal the email.
    userInfo.setName(userInfo.getEmail());

    return getRepository().save(userInfo);
  }

  public User createFromIDToken(IDToken idToken) {
    val userInfo = new User();
    userInfo.setName(idToken.getEmail());
    userInfo.setEmail(idToken.getEmail());
    userInfo.setFirstName(
        StringUtils.isEmpty(idToken.getGiven_name()) ? "" : idToken.getGiven_name());
    userInfo.setLastName(
        StringUtils.isEmpty(idToken.getFamily_name()) ? "" : idToken.getFamily_name());
    userInfo.setStatus(DEFAULT_USER_STATUS);
    userInfo.setCreatedAt(new Date());
    userInfo.setLastLogin(null);
    userInfo.setRole(DEFAULT_USER_ROLE);
    return this.create(userInfo);
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
                    User.builder()
                        .name(DEMO_USER_NAME)
                        .email(DEMO_USER_EMAIL)
                        .firstName(DEMO_FIRST_NAME)
                        .lastName(DEMO_LAST_NAME)
                        .status(EntityStatus.APPROVED.toString())
                        .createdAt(new Date())
                        .lastLogin(null)
                        .role(UserRole.ADMIN.toString())
                        .build()));
  }

  public User addUserToGroups(@NonNull String userId, @NonNull List<String> groupIDs) {
    val user = getById(userId);
    val groups = groupService.getMany(groupIDs);
    groups.forEach(user::associateWithGroup);
    // TODO: @rtisma test setting groups even if there were existing groups before does not delete
    // the existing ones. Becuase the PERSIST and MERGE cascade type is used, this should work
    // correctly
    return getRepository().save(user);
  }

  public User addUserToApps(@NonNull String userId, @NonNull List<String> appIDs) {
    val user = getById(userId);
    val apps = applicationService.getMany(appIDs);
    apps.forEach(user::associateWithApplication);
    // TODO: @rtisma test setting apps even if there were existing apps before does not delete the
    // existing ones. Becuase the PERSIST and MERGE cascade type is used, this should work correctly
    return getRepository().save(user);
  }

  public User addUserPermissions(
      @NonNull String userId, @NonNull List<PolicyIdStringWithAccessLevel> permissions) {
    val policyMap =
        permissions.stream().collect(groupingBy(PolicyIdStringWithAccessLevel::getPolicyId));
    val user = getById(userId);
    policyService
        .getMany(ImmutableList.copyOf(policyMap.keySet()))
        .stream()
        .flatMap(p -> streamUserPermission(user, policyMap, p))
        .map(userPermissionService::create)
        .forEach(user::associateWithPermission);
    return getRepository().save(user);
  }

  public User get(@NonNull String userId) {
    return getById(userId);
  }

  public User update(@NonNull User updatedUserInfo) {
    val user = getById(updatedUserInfo.getId().toString());
    if (UserRole.USER.toString().equals(updatedUserInfo.getRole().toUpperCase()))
      updatedUserInfo.setRole(UserRole.USER.toString());
    else if (UserRole.ADMIN.toString().equals(updatedUserInfo.getRole().toUpperCase()))
      updatedUserInfo.setRole(UserRole.ADMIN.toString());
    user.update(updatedUserInfo);
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
    val user = getById(userId);
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
    val user = getById(userId);
    val appUUIDs = convertToUUIDSet(appIDs);
    checkApplicationsExistForUser(user, appUUIDs);
    user.getApplications().removeIf(x -> appUUIDs.contains(x.getId()));
    getRepository().save(user);
  }

  // TODO @rtisma: add test for checking user permission exists for user
  public void deleteUserPermissions(
      @NonNull String userId, @NonNull Collection<String> permissionsIds) {
    val user = getById(userId);
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
    val userPermissions = ImmutableList.copyOf(getById(userId).getUserPermissions());
    return new PageImpl<>(userPermissions, pageable, userPermissions.size());
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

  private static Stream<UserPermission> streamUserPermission(
      User u, Map<String, List<PolicyIdStringWithAccessLevel>> policyMap, Policy p) {
    val policyId = p.getId().toString();
    return policyMap
        .get(policyId)
        .stream()
        .map(PolicyIdStringWithAccessLevel::getMask)
        .map(AccessLevel::fromValue)
        .map(a -> UserPermission.builder().accessLevel(a).policy(p).owner(u).build());
  }
}
