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

import static bio.overture.ego.model.enums.UserType.ADMIN;
import static bio.overture.ego.model.exceptions.NotFoundException.buildNotFoundException;
import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.model.exceptions.RequestValidationException.checkRequestValid;
import static bio.overture.ego.model.exceptions.UniqueViolationException.checkUnique;
import static bio.overture.ego.service.AbstractPermissionService.resolveFinalPermissions;
import static bio.overture.ego.utils.CollectionUtils.*;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Converters.*;
import static bio.overture.ego.utils.EntityServices.checkEntityExistence;
import static bio.overture.ego.utils.EntityServices.getManyEntities;
import static bio.overture.ego.utils.FieldUtils.onUpdateDetected;
import static bio.overture.ego.utils.Ids.checkDuplicates;
import static bio.overture.ego.utils.Joiners.PRETTY_COMMA;
import static java.util.Objects.isNull;
import static org.springframework.data.jpa.domain.Specification.where;

import bio.overture.ego.config.UserDefaultsConfig;
import bio.overture.ego.event.token.ApiKeyEventsPublisher;
import bio.overture.ego.model.dto.CreateUserRequest;
import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.dto.UpdateUserRequest;
import bio.overture.ego.model.entity.*;
import bio.overture.ego.model.enums.IdProviderType;
import bio.overture.ego.model.join.UserApplication;
import bio.overture.ego.model.join.UserGroup;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.GroupRepository;
import bio.overture.ego.repository.UserRepository;
import bio.overture.ego.repository.queryspecification.UserSpecification;
import bio.overture.ego.repository.queryspecification.builder.UserSpecificationBuilder;
import bio.overture.ego.token.IDToken;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.*;
import javax.transaction.Transactional;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
public class UserService extends AbstractNamedService<User, UUID> {

  /** Constants */
  public static final UserConverter USER_CONVERTER = Mappers.getMapper(UserConverter.class);

  /** Dependencies */
  private final GroupRepository groupRepository;

  private final ApiKeyEventsPublisher apiKeyEventsPublisher;
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
      @NonNull ApiKeyEventsPublisher apiKeyEventsPublisher) {
    super(User.class, userRepository);
    this.userRepository = userRepository;
    this.groupRepository = groupRepository;
    this.applicationService = applicationService;
    this.userDefaultsConfig = userDefaultsConfig;
    this.apiKeyEventsPublisher = apiKeyEventsPublisher;
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
                    .fetchUserAndGroupPermissions(true)
                    .fetchRefreshToken(true)
                    .buildByNameIgnoreCase(name));
  }

  @SuppressWarnings("unchecked")
  public User get(
      @NonNull UUID id,
      boolean fetchUserPermissions,
      boolean fetchUserGroups,
      boolean fetchApplications,
      boolean fetchRefreshToken) {
    val result =
        (Optional<User>)
            getRepository()
                .findOne(
                    new UserSpecificationBuilder()
                        .fetchUserAndGroupPermissions(fetchUserPermissions)
                        .fetchUserGroups(fetchUserGroups)
                        .fetchApplications(fetchApplications)
                        .fetchRefreshToken(fetchRefreshToken)
                        .buildById(id));
    checkNotFound(result.isPresent(), "The userId '%s' does not exist", id);
    return result.get();
  }

  public Collection<User> getMany(
      @NonNull Collection<UUID> ids,
      boolean fetchUserPermissions,
      boolean fetchUserGroups,
      boolean fetchApplications) {
    val spec =
        new UserSpecificationBuilder()
            .fetchUserAndGroupPermissions(fetchUserPermissions)
            .fetchUserGroups(fetchUserGroups)
            .fetchApplications(fetchApplications);
    return getMany(ids, spec);
  }

  public User createFromIDToken(IDToken idToken) {
    return create(
        CreateUserRequest.builder()
            .email(idToken.getEmail())
            .firstName(idToken.getGiven_name())
            .lastName(idToken.getFamily_name())
            .status(userDefaultsConfig.getDefaultUserStatus())
            .type(userDefaultsConfig.getDefaultUserType())
            .identityProvider(idToken.getIdentity_provider())
            .providerId(idToken.getProvider_id())
            .build());
  }

  public User getUserByToken(@NonNull IDToken idToken) {
    val provider = idToken.getIdentity_provider();
    val providerId = idToken.getProvider_id();

    val user =
        getByProviderAndProviderId(provider, providerId)
            .orElseGet(
                () -> {
                  log.info("User not found, creating.");
                  return createFromIDToken(idToken);
                });
    user.setLastLogin(new Date());
    return user;
  }

  public Optional<User> getByProviderAndProviderId(IdProviderType provider, String providerId) {
    val user = userRepository.findByIdentityProviderAndProviderId(provider, providerId);
    checkNotFound(user.isPresent(), "The user was not found");
    val name = user.get().getName();
    return findByName(name);
  }

  @Override
  public User getWithRelationships(@NonNull UUID id) {
    return get(id, true, true, true, true);
  }

  public User getWithApplications(@NonNull UUID id) {
    return get(id, false, false, true, false);
  }

  public User getWithGroups(@NonNull UUID id) {
    return get(id, false, true, false, false);
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

  public Page<User> listUsers(@NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    val spec = UserSpecification.filterBy(filters);
    return getRepository().findAll(spec, pageable);
  }

  @SuppressWarnings("unchecked")
  public Page<User> findUsers(
      @NonNull String query, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return getRepository()
        .findAll(
            where(UserSpecification.containsText(query)).and(UserSpecification.filterBy(filters)),
            pageable);
  }

  @SuppressWarnings("Duplicates")
  public User associateApplicationsWithUser(
      @NonNull UUID id, @NonNull Collection<UUID> applicationIds) {
    // check duplicate applicationIds
    checkDuplicates(Application.class, applicationIds);

    // Get existing associated application ids with the user
    val userWithUserApplications = getWithApplications(id);
    val applications =
        mapToImmutableSet(
            userWithUserApplications.getUserApplications(), UserApplication::getApplication);
    val existingAssociatedApplicationIds = convertToIds(applications);

    // Check there are no application ids that are already associated with the user
    val existingAlreadyAssociatedApplicationIds =
        intersection(existingAssociatedApplicationIds, applicationIds);
    checkUnique(
        existingAlreadyAssociatedApplicationIds.isEmpty(),
        "The following %s ids are already associated with %s '%s': [%s]",
        Application.class.getSimpleName(),
        getEntityTypeName(),
        id,
        PRETTY_COMMA.join(existingAlreadyAssociatedApplicationIds));

    // Get all unassociated application ids. If they do not exist, an error is thrown
    val nonAssociatedApplicationIds = difference(applicationIds, existingAssociatedApplicationIds);
    val nonAssociatedApplications = applicationService.getMany(nonAssociatedApplicationIds);

    // Associate the existing applications with the user
    nonAssociatedApplications.stream()
        .map(a -> convertToUserApplication(userWithUserApplications, a))
        .forEach(UserService::associateSelf);
    return userWithUserApplications;
  }

  @SuppressWarnings("Duplicates")
  public User associateGroupsWithUser(@NonNull UUID id, @NonNull Collection<UUID> groupIds) {
    // check duplicate groupIds
    checkDuplicates(Group.class, groupIds);

    // Get existing associated group ids with the user
    val userWithUserGroups = getWithGroups(id);
    val groups = mapToImmutableSet(userWithUserGroups.getUserGroups(), UserGroup::getGroup);
    val existingAssociatedGroupIds = convertToIds(groups);

    // Check there are no group ids that are already associated with the user
    val existingAlreadyAssociatedGroupIds = intersection(existingAssociatedGroupIds, groupIds);
    checkUnique(
        existingAlreadyAssociatedGroupIds.isEmpty(),
        "The following %s ids are already associated with %s '%s': [%s]",
        Group.class.getSimpleName(),
        getEntityTypeName(),
        id,
        PRETTY_COMMA.join(existingAlreadyAssociatedGroupIds));

    // Get all unassociated group ids. If they do not exist, an error is thrown
    val nonAssociatedGroupIds = difference(groupIds, existingAssociatedGroupIds);
    val nonAssociatedGroups = getManyEntities(Group.class, groupRepository, nonAssociatedGroupIds);

    // Associate the existing groups with the user
    nonAssociatedGroups.stream()
        .map(g -> convertToUserGroup(userWithUserGroups, g))
        .forEach(UserGroupService::associateSelf);
    apiKeyEventsPublisher.requestApiKeyCleanupByUsers(ImmutableSet.of(userWithUserGroups));
    return userWithUserGroups;
  }

  @SuppressWarnings("Duplicates")
  public void disassociateApplicationsFromUser(
      @NonNull UUID id, @NonNull Collection<UUID> applicationIds) {
    // check duplicate applicationIds
    checkDuplicates(Application.class, applicationIds);

    // Get existing associated child ids with the parent
    val userWithApplications = getWithApplications(id);
    val applications =
        mapToImmutableSet(
            userWithApplications.getUserApplications(), UserApplication::getApplication);
    val existingAssociatedApplicationIds = convertToIds(applications);

    // Get existing and non-existing non-associated application ids. Error out if there are existing
    // and non-existing non-associated application ids
    val nonAssociatedApplicationIds = difference(applicationIds, existingAssociatedApplicationIds);
    if (!nonAssociatedApplicationIds.isEmpty()) {
      applicationService.checkExistence(nonAssociatedApplicationIds);
      throw buildNotFoundException(
          "The following existing %s ids cannot be disassociated from %s '%s' "
              + "because they are not associated with it",
          Application.class.getSimpleName(), getEntityTypeName(), id);
    }

    // Since all application ids exist and are associated with the user, disassociate them from
    // each other
    val applicationIdsToDisassociate = ImmutableSet.copyOf(applicationIds);
    val userApplicationsToDisassociate =
        userWithApplications.getUserApplications().stream()
            .filter(ga -> applicationIdsToDisassociate.contains(ga.getId().getApplicationId()))
            .collect(toImmutableSet());

    disassociateUserApplicationsFromUser(userWithApplications, userApplicationsToDisassociate);
  }

  @SuppressWarnings("Duplicates")
  public void disassociateGroupsFromUser(@NonNull UUID id, @NonNull Collection<UUID> groupIds) {
    // check duplicate groupIds
    checkDuplicates(Group.class, groupIds);

    // Get existing associated child ids with the parent
    val userWithGroups = getWithGroups(id);
    val groups = mapToImmutableSet(userWithGroups.getUserGroups(), UserGroup::getGroup);
    val existingAssociatedGroupIds = convertToIds(groups);

    // Get existing and non-existing non-associated group ids. Error out if there are existing
    // and non-existing non-associated group ids
    val nonAssociatedGroupIds = difference(groupIds, existingAssociatedGroupIds);
    if (!nonAssociatedGroupIds.isEmpty()) {
      checkEntityExistence(Group.class, groupRepository, nonAssociatedGroupIds);
      throw buildNotFoundException(
          "The following existing %s ids cannot be disassociated from %s '%s' "
              + "because they are not associated with it",
          Group.class.getSimpleName(), getEntityTypeName(), id);
    }

    // Since all group ids exist and are associated with the user, disassociate them from
    // each other
    val groupIdsToDisassociate = ImmutableSet.copyOf(groupIds);
    val userGroupsToDisassociate =
        userWithGroups.getUserGroups().stream()
            .filter(ga -> groupIdsToDisassociate.contains(ga.getId().getGroupId()))
            .collect(toImmutableSet());

    disassociateUserGroupsFromUser(userWithGroups, userGroupsToDisassociate);
    apiKeyEventsPublisher.requestApiKeyCleanupByUsers(ImmutableSet.of(userWithGroups));
  }

  @Override
  public void delete(@NonNull UUID id) {
    val user = getWithRelationships(id);
    disassociateAllGroupsFromUser(user);
    disassociateAllApplicationsFromUser(user);
    apiKeyEventsPublisher.requestApiKeyCleanupByUsers(ImmutableSet.of(user));
    super.delete(id);
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

    val spec =
        where(UserSpecification.inGroup(groupId))
            .and(UserSpecification.containsText(query))
            .and(UserSpecification.filterBy(filters));

    return userRepository.findAll(where(spec), pageable);
  }

  @SuppressWarnings("unchecked")
  public Page<User> findUsersForGroups(
      @NonNull Collection<UUID> groupIds,
      @NonNull String query,
      @NonNull List<SearchFilter> filters,
      @NonNull Pageable pageable) {
    checkEntityExistence(Group.class, groupRepository, groupIds);

    val spec =
        where(UserSpecification.inGroups(groupIds))
            .and(UserSpecification.containsText(query))
            .and(UserSpecification.filterBy(filters));

    return userRepository.findAll(spec, pageable);
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
    checkProviderAndProviderIdUnique(r.getIdentityProvider(), r.getProviderId());
  }

  // TODO: don't use email for onUpdateDetected?
  private void validateUpdateRequest(User originalUser, UpdateUserRequest r) {
    onUpdateDetected(
        originalUser.getEmail(),
        r.getEmail(),
        () -> checkProviderAndProviderIdUnique(r.getIdentityProvider(), r.getProviderId()));
  }

  private void checkProviderAndProviderIdUnique(IdProviderType provider, String providerId) {
    checkUnique(
        !userRepository.existsDistinctByIdentityProviderAndProviderId(provider, providerId),
        "A user with the same provider info already exists");
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

  public static Set<Scope> extractScopes(@NonNull User user) {
    val resolvedPermissions = resolveUsersPermissions(user);
    val output = mapToSet(resolvedPermissions, AbstractPermissionService::buildScope);
    if (output.isEmpty()) {
      output.add(Scope.defaultScope());
    }
    return output;
  }

  public static void disassociateUserApplicationsFromUser(
      @NonNull User g, @NonNull Collection<UserApplication> userApplications) {
    userApplications.forEach(
        ua -> {
          ua.getApplication().getUserApplications().remove(ua);
          ua.setApplication(null);
          ua.setUser(null);
        });
    g.getUserApplications().removeAll(userApplications);
  }

  public static void disassociateAllApplicationsFromUser(@NonNull User u) {
    val userApplications = u.getUserApplications();
    disassociateUserApplicationsFromUser(u, userApplications);
  }

  private static void associateSelf(@NonNull UserApplication ua) {
    ua.getUser().getUserApplications().add(ua);
    ua.getApplication().getUserApplications().add(ua);
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
