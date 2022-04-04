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

import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.model.exceptions.RequestValidationException.checkRequestValid;
import static bio.overture.ego.model.exceptions.UniqueViolationException.checkUnique;
import static bio.overture.ego.service.AbstractPermissionService.resolveFinalPermissions;
import static bio.overture.ego.token.app.AppTokenClaims.AUTHORIZED_GRANT_TYPES;
import static bio.overture.ego.utils.CollectionUtils.*;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.EntityServices.checkEntityExistence;
import static bio.overture.ego.utils.FieldUtils.onUpdateDetected;
import static java.util.Objects.isNull;
import static org.mapstruct.factory.Mappers.getMapper;
import static org.springframework.data.jpa.domain.Specification.where;

import bio.overture.ego.model.dto.CreateApplicationRequest;
import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.dto.UpdateApplicationRequest;
import bio.overture.ego.model.entity.*;
import bio.overture.ego.model.join.GroupApplication;
import bio.overture.ego.model.join.UserApplication;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.ApplicationRepository;
import bio.overture.ego.repository.GroupRepository;
import bio.overture.ego.repository.UserRepository;
import bio.overture.ego.repository.queryspecification.ApplicationSpecification;
import bio.overture.ego.repository.queryspecification.builder.ApplicationSpecificationBuilder;
import com.google.common.collect.ImmutableList;
import java.util.*;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.TargetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApplicationService extends AbstractNamedService<Application, UUID>
    implements RegisteredClientRepository {

  /** Constants */
  public static final ApplicationConverter APPLICATION_CONVERTER =
      getMapper(ApplicationConverter.class);

  /*
   Dependencies
  */
  private final ApplicationRepository applicationRepository;
  private final PasswordEncoder passwordEncoder;
  private final GroupRepository groupRepository;
  private final UserRepository userRepository;

  @Autowired
  public ApplicationService(
      @NonNull ApplicationRepository applicationRepository,
      @NonNull GroupRepository groupRepository,
      @NonNull UserRepository userRepository,
      @NonNull PasswordEncoder passwordEncoder) {
    super(Application.class, applicationRepository);
    this.applicationRepository = applicationRepository;
    this.passwordEncoder = passwordEncoder;
    this.groupRepository = groupRepository;
    this.userRepository = userRepository;
  }

  @Override
  public void delete(@NonNull UUID groupId) {
    val application = getWithRelationships(groupId);
    disassociateAllGroupsFromApplication(application);
    disassociateAllUsersFromApplication(application);
    getRepository().delete(application);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Optional<Application> findByName(@NonNull String name) {
    return (Optional<Application>)
        getRepository()
            .findOne(
                new ApplicationSpecificationBuilder()
                    .fetchGroups(true)
                    .fetchUsers(true)
                    .fetchApplicationAndGroupPermissions(true)
                    .buildByNameIgnoreCase(name));
  }

  public Application create(@NonNull CreateApplicationRequest request) {
    validateCreateRequest(request);
    val application = APPLICATION_CONVERTER.convertToApplication(request);
    return getRepository().save(application);
  }

  public Application partialUpdate(@NonNull UUID id, @NonNull UpdateApplicationRequest request) {
    val app = getById(id);
    validateUpdateRequest(app, request);
    APPLICATION_CONVERTER.updateApplication(request, app);
    return getRepository().save(app);
  }

  @Override
  public Application getWithRelationships(@NonNull UUID id) {
    return get(id, true, true, true);
  }

  @SuppressWarnings("unchecked")
  public Page<Application> listApps(
      @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return getRepository().findAll(ApplicationSpecification.filterBy(filters), pageable);
  }

  @SuppressWarnings("unchecked")
  public Page<Application> findApps(
      @NonNull String query, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return getRepository()
        .findAll(
            where(ApplicationSpecification.containsText(query))
                .and(ApplicationSpecification.filterBy(filters)),
            pageable);
  }

  @SuppressWarnings("unchecked")
  public Page<Application> findApplicationsForUser(
      @NonNull UUID userId, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    checkEntityExistence(User.class, userRepository, userId);
    return getRepository()
        .findAll(
            where(ApplicationSpecification.usedBy(userId))
                .and(ApplicationSpecification.filterBy(filters)),
            pageable);
  }

  @SuppressWarnings("unchecked")
  public Page<Application> findApplicationsForUser(
      @NonNull UUID userId,
      @NonNull String query,
      @NonNull List<SearchFilter> filters,
      @NonNull Pageable pageable) {
    checkEntityExistence(User.class, userRepository, userId);
    return getRepository()
        .findAll(
            where(ApplicationSpecification.usedBy(userId))
                .and(ApplicationSpecification.containsText(query))
                .and(ApplicationSpecification.filterBy(filters)),
            pageable);
  }

  @SuppressWarnings("unchecked")
  public Page<Application> findApplicationsForGroup(
      @NonNull UUID groupId, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    checkEntityExistence(Group.class, groupRepository, groupId);
    return getRepository()
        .findAll(
            where(ApplicationSpecification.inGroup(groupId))
                .and(ApplicationSpecification.filterBy(filters)),
            pageable);
  }

  @SuppressWarnings("unchecked")
  public Page<Application> findApplicationsForGroup(
      @NonNull UUID groupId,
      @NonNull String query,
      @NonNull List<SearchFilter> filters,
      @NonNull Pageable pageable) {
    checkEntityExistence(Group.class, groupRepository, groupId);
    return getRepository()
        .findAll(
            where(ApplicationSpecification.inGroup(groupId))
                .and(ApplicationSpecification.containsText(query))
                .and(ApplicationSpecification.filterBy(filters)),
            pageable);
  }

  @Override
  public void save(RegisteredClient registeredClient) {}

  @Override
  public RegisteredClient findById(String s) {
    return null;
  }

  @Override
  public RegisteredClient findByClientId(String clientId) {
    val application = getByClientId(clientId);
    if (Objects.isNull(application)) {
      return null;
    }
    if (application.getStatus() != APPROVED) {
      throw new RuntimeException("Client Access is not approved.");
    }

    val approvedScopes = mapToSet(extractScopes(application), Scope::toString);

    // transform application to client details
    val clientDetails =
        RegisteredClient.withId(application.getId().toString())
            .clientSecret(passwordEncoder.encode(application.getClientSecret()));

    approvedScopes.forEach(clientDetails::scope);
    clientDetails.redirectUri(application.getRedirectUri());
    Arrays.stream(AUTHORIZED_GRANT_TYPES).forEach(clientDetails::authorizationGrantType);

    clientDetails.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
    clientDetails.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
    clientDetails.clientId(clientId);
    return clientDetails.build();
  }

  public Application getByClientId(@NonNull String clientId) {
    val result = getClientApplication(clientId);
    checkNotFound(
        result.isPresent(),
        "The '%s' entity with clientId '%s' was not found",
        Application.class.getSimpleName(),
        clientId);
    return result.get();
  }

  @SuppressWarnings("unchecked")
  public Optional<Application> getClientApplication(@NonNull String clientId) {
    return (Optional<Application>)
        getRepository()
            .findOne(
                new ApplicationSpecificationBuilder()
                    .fetchGroups(true)
                    .fetchUsers(true)
                    .fetchApplicationAndGroupPermissions(true)
                    .buildByClientIdIgnoreCase(clientId));
  }

  private static Collection<AbstractPermission> getResolvedPermissions(
      @NonNull Application application) {
    val applicationPermissions = application.getApplicationPermissions();
    Collection<ApplicationPermission> appPermissions =
        isNull(applicationPermissions) ? ImmutableList.of() : applicationPermissions;

    val groupApps = application.getGroupApplications();
    Collection<GroupPermission> groupPermissions =
        isNull(groupApps)
            ? ImmutableList.of()
            : groupApps.stream()
                .map(GroupApplication::getGroup)
                .map(Group::getPermissions)
                .flatMap(Collection::stream)
                .collect(toImmutableSet());
    return resolveFinalPermissions(appPermissions, groupPermissions);
  }

  public static Set<Scope> extractScopes(@NonNull Application application) {
    val resolvedPermissions = getResolvedPermissions(application);
    val output = mapToSet(resolvedPermissions, AbstractPermissionService::buildScope);
    if (output.isEmpty()) {
      output.add(Scope.defaultScope());
    }
    return output;
  }

  private void validateUpdateRequest(Application originalApplication, UpdateApplicationRequest r) {
    onUpdateDetected(
        originalApplication.getClientId(),
        r.getClientId(),
        () -> checkClientIdUnique(r.getClientId()));
    onUpdateDetected(
        originalApplication.getName(), r.getName(), () -> checkNameUnique(r.getName()));
  }

  private void validateCreateRequest(CreateApplicationRequest r) {
    checkRequestValid(r);
    checkNameUnique(r.getName());
    checkClientIdUnique(r.getClientId());
  }

  private void checkClientIdUnique(String clientId) {
    checkUnique(
        !applicationRepository.existsByClientIdIgnoreCase(clientId),
        "An application with the same clientId already exists");
  }

  private void checkNameUnique(String name) {
    checkUnique(
        !applicationRepository.existsByNameIgnoreCase(name),
        "An application with the same name already exists");
  }

  @SuppressWarnings("unchecked")
  private Application get(
      UUID id,
      boolean fetchUsers,
      boolean fetchGroups,
      boolean fetchApplicationAndGroupPermissions) {
    val result =
        (Optional<Application>)
            getRepository()
                .findOne(
                    new ApplicationSpecificationBuilder()
                        .fetchApplicationAndGroupPermissions(fetchApplicationAndGroupPermissions)
                        .fetchUsers(fetchUsers)
                        .fetchGroups(fetchGroups)
                        .buildById(id));
    checkNotFound(result.isPresent(), "The applicationId '%s' does not exist", id);
    return result.get();
  }

  public static void disassociateAllGroupsFromApplication(@NonNull Application a) {
    val groupApplications = a.getGroupApplications();
    disassociateGroupApplicationsFromApplication(a, groupApplications);
  }

  public static void disassociateAllUsersFromApplication(@NonNull Application a) {
    val userApplications = a.getUserApplications();
    disassociateUserApplicationsFromApplication(a, userApplications);
  }

  public static void disassociateUserApplicationsFromApplication(
      @NonNull Application application, @NonNull Collection<UserApplication> userApplications) {
    userApplications.forEach(
        ua -> {
          ua.getUser().getUserApplications().remove(ua);
          ua.setUser(null);
          ua.setApplication(null);
        });
    application.getUserApplications().removeAll(userApplications);
  }

  public static void disassociateGroupApplicationsFromApplication(
      @NonNull Application application, @NonNull Collection<GroupApplication> groupApplications) {
    groupApplications.forEach(
        ga -> {
          ga.getGroup().getGroupApplications().remove(ga);
          ga.setGroup(null);
          ga.setApplication(null);
        });
    application.getGroupApplications().removeAll(groupApplications);
  }

  @Mapper(
      nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
      unmappedTargetPolicy = ReportingPolicy.WARN)
  public abstract static class ApplicationConverter {

    public abstract Application convertToApplication(CreateApplicationRequest request);

    public abstract void updateApplication(
        Application updatingApplication, @MappingTarget Application applicationToUpdate);

    public abstract void updateApplication(
        UpdateApplicationRequest updateRequest, @MappingTarget Application applicationToUpdate);

    protected Application initApplicationEntity(@TargetType Class<Application> appClass) {
      return Application.builder().build();
    }
  }
}
