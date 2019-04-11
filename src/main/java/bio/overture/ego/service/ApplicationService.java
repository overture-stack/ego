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
import static bio.overture.ego.token.app.AppTokenClaims.AUTHORIZED_GRANTS;
import static bio.overture.ego.token.app.AppTokenClaims.ROLE;
import static bio.overture.ego.token.app.AppTokenClaims.SCOPES;
import static bio.overture.ego.utils.CollectionUtils.setOf;
import static bio.overture.ego.utils.EntityServices.checkEntityExistence;
import static bio.overture.ego.utils.FieldUtils.onUpdateDetected;
import static bio.overture.ego.utils.Splitters.COLON_SPLITTER;
import static java.lang.String.format;
import static org.mapstruct.factory.Mappers.getMapper;
import static org.springframework.data.jpa.domain.Specification.where;

import bio.overture.ego.model.dto.CreateApplicationRequest;
import bio.overture.ego.model.dto.UpdateApplicationRequest;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.join.GroupApplication;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.ApplicationRepository;
import bio.overture.ego.repository.GroupRepository;
import bio.overture.ego.repository.UserRepository;
import bio.overture.ego.repository.queryspecification.ApplicationSpecification;
import bio.overture.ego.repository.queryspecification.builder.ApplicationSpecificationBuilder;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApplicationService extends AbstractNamedService<Application, UUID>
    implements ClientDetailsService {

  /** Constants */
  public static final ApplicationConverter APPLICATION_CONVERTER =
      getMapper(ApplicationConverter.class);

  public static final String APP_TOKEN_PREFIX = "Basic ";

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
    return get(id, true, true);
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

  @SuppressWarnings("unchecked")
  public Optional<Application> findByClientId(@NonNull String clientId) {
    return (Optional<Application>)
        getRepository()
            .findOne(
                new ApplicationSpecificationBuilder()
                    .fetchGroups(true)
                    .fetchUsers(true)
                    .buildByClientIdIgnoreCase(clientId));
  }

  public Application getByClientId(@NonNull String clientId) {
    val result = findByClientId(clientId);
    checkNotFound(
        result.isPresent(),
        "The '%s' entity with clientId '%s' was not found",
        Application.class.getSimpleName(),
        clientId);
    return result.get();
  }

  public Application findByBasicToken(@NonNull String token) {
    log.info(format("Looking for token '%s'", token));
    val base64encoding = removeAppTokenPrefix(token);
    log.info(format("Decoding '%s'", base64encoding));

    val contents = new String(Base64.getDecoder().decode(base64encoding));
    log.info(format("Decoded to '%s'", contents));

    val parts = COLON_SPLITTER.splitToList(contents);
    val clientId = parts.get(0);
    log.info(format("Extracted client id '%s'", clientId));
    return getByClientId(clientId);
  }

  @Override
  public ClientDetails loadClientByClientId(@NonNull String clientId)
      throws ClientRegistrationException {
    // find client using clientid

    val application = getByClientId(clientId);

    if (application.getStatus() != APPROVED) {
      throw new ClientRegistrationException("Client Access is not approved.");
    }

    // transform application to client details
    val approvedScopes = Arrays.asList(SCOPES);
    val clientDetails = new BaseClientDetails();
    clientDetails.setClientId(clientId);
    clientDetails.setClientSecret(passwordEncoder.encode(application.getClientSecret()));
    clientDetails.setAuthorizedGrantTypes(Arrays.asList(AUTHORIZED_GRANTS));
    clientDetails.setScope(approvedScopes);
    clientDetails.setRegisteredRedirectUri(setOf(application.getRedirectUri()));
    clientDetails.setAutoApproveScopes(approvedScopes);
    val authorities = new HashSet<GrantedAuthority>();
    authorities.add(new SimpleGrantedAuthority(ROLE));
    clientDetails.setAuthorities(authorities);
    return clientDetails;
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
  private Application get(UUID id, boolean fetchUsers, boolean fetchGroups) {
    val result =
        (Optional<Application>)
            getRepository()
                .findOne(
                    new ApplicationSpecificationBuilder()
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
    val users = a.getUsers();
    disassociateUsersFromApplication(a, users);
  }

  public static void disassociateUsersFromApplication(
      @NonNull Application application, @NonNull Collection<User> users) {
    users.forEach(
        u -> {
          u.getApplications().remove(application);
          application.getUsers().remove(u);
        });
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

  private static String removeAppTokenPrefix(String token) {
    return token.replace(APP_TOKEN_PREFIX, "").trim();
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
