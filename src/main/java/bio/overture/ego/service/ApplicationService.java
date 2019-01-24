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

import static bio.overture.ego.model.enums.ApplicationStatus.APPROVED;
import static bio.overture.ego.model.exceptions.NotFoundException.checkNotFound;
import static bio.overture.ego.model.exceptions.UniqueViolationException.checkUnique;
import static bio.overture.ego.token.app.AppTokenClaims.AUTHORIZED_GRANTS;
import static bio.overture.ego.token.app.AppTokenClaims.ROLE;
import static bio.overture.ego.token.app.AppTokenClaims.SCOPES;
import static bio.overture.ego.utils.CollectionUtils.setOf;
import static bio.overture.ego.utils.FieldUtils.onUpdateDetected;
import static bio.overture.ego.utils.Splitters.COLON_SPLITTER;
import static java.lang.String.format;
import static java.util.UUID.fromString;
import static org.mapstruct.factory.Mappers.getMapper;
import static org.springframework.data.jpa.domain.Specifications.where;

import bio.overture.ego.model.dto.CreateApplicationRequest;
import bio.overture.ego.model.dto.UpdateApplicationRequest;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.repository.ApplicationRepository;
import bio.overture.ego.repository.queryspecification.ApplicationSpecification;
import java.util.Arrays;
import java.util.Base64;
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

  public static final ApplicationConverter APPLICATION_CONVERTER =
      getMapper(ApplicationConverter.class);
  public static final String APP_TOKEN_PREFIX = "Basic ";

  /*
   Dependencies
  */
  private final ApplicationRepository applicationRepository;
  private final PasswordEncoder passwordEncoder;

  @Autowired
  public ApplicationService(
      @NonNull ApplicationRepository applicationRepository,
      @NonNull PasswordEncoder passwordEncoder) {
    super(Application.class, applicationRepository);
    this.applicationRepository = applicationRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public Application create(@NonNull CreateApplicationRequest request) {
    checkClientIdUnique(request.getClientId());
    val application = APPLICATION_CONVERTER.convertToApplication(request);
    return getRepository().save(application);
  }

  public Application get(@NonNull String applicationId) {
    return getById(fromString(applicationId));
  }

  public Application partialUpdate(@NonNull String id, @NonNull UpdateApplicationRequest request) {
    val app = getById(fromString(id));
    validateUpdateRequest(app, request);
    APPLICATION_CONVERTER.updateApplication(request, app);
    return getRepository().save(app);
  }

  public Page<Application> listApps(
      @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return getRepository().findAll(ApplicationSpecification.filterBy(filters), pageable);
  }

  public Page<Application> findApps(
      @NonNull String query, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return getRepository()
        .findAll(
            where(ApplicationSpecification.containsText(query))
                .and(ApplicationSpecification.filterBy(filters)),
            pageable);
  }

  public Page<Application> findUserApps(
      @NonNull String userId, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return getRepository()
        .findAll(
            where(ApplicationSpecification.usedBy(fromString(userId)))
                .and(ApplicationSpecification.filterBy(filters)),
            pageable);
  }

  public Page<Application> findUserApps(
      @NonNull String userId,
      @NonNull String query,
      @NonNull List<SearchFilter> filters,
      @NonNull Pageable pageable) {
    return getRepository()
        .findAll(
            where(ApplicationSpecification.usedBy(fromString(userId)))
                .and(ApplicationSpecification.containsText(query))
                .and(ApplicationSpecification.filterBy(filters)),
            pageable);
  }

  public Page<Application> findGroupApplications(
      @NonNull String groupId, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return getRepository()
        .findAll(
            where(ApplicationSpecification.inGroup(fromString(groupId)))
                .and(ApplicationSpecification.filterBy(filters)),
            pageable);
  }

  public Page<Application> findGroupApplications(
      @NonNull String groupId,
      @NonNull String query,
      @NonNull List<SearchFilter> filters,
      @NonNull Pageable pageable) {
    return getRepository()
        .findAll(
            where(ApplicationSpecification.inGroup(fromString(groupId)))
                .and(ApplicationSpecification.containsText(query))
                .and(ApplicationSpecification.filterBy(filters)),
            pageable);
  }

  public Optional<Application> findByClientId(@NonNull String clientId) {
    return applicationRepository.getApplicationByClientIdIgnoreCase(clientId);
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
    log.error(format("Looking for token '%s'", token));
    val base64encoding = removeAppTokenPrefix(token);
    log.error(format("Decoding '%s'", base64encoding));

    val contents = new String(Base64.getDecoder().decode(base64encoding));
    log.error(format("Decoded to '%s'", contents));

    val parts = COLON_SPLITTER.splitToList(contents);
    val clientId = parts.get(0);
    log.error(format("Extracted client id '%s'", clientId));
    return getByClientId(clientId);
  }

  // TODO: [rtisma] will not work, because if Application has associated users, the foreign key
  // contraint on the userapplication table will prevent the application record from being deleted.
  // First the appropriate rows of the userapplication join table have to be deleted (i.e
  // disassociation of users from an application), and then the application record can be deleted
  // http://docs.jboss.org/hibernate/orm/5.4/userguide/html_single/Hibernate_User_Guide.html#associations-many-to-many
  public void delete(String id) {
    delete(fromString(id));
  }

  @Override
  public ClientDetails loadClientByClientId(@NonNull String clientId)
      throws ClientRegistrationException {
    // find client using clientid

    val application = getByClientId(clientId);

    if (!application.getStatus().equals(APPROVED.toString())) {
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

  @Deprecated
  public Application create(@NonNull Application applicationInfo) {
    return getRepository().save(applicationInfo);
  }

  @Deprecated
  public Application update(@NonNull Application updatedApplicationInfo) {
    checkExistence(updatedApplicationInfo.getId());
    getRepository().save(updatedApplicationInfo);
    return updatedApplicationInfo;
  }

  private void validateUpdateRequest(Application originalApplication, UpdateApplicationRequest r) {
    onUpdateDetected(
        originalApplication.getClientId(),
        r.getClientId(),
        () -> checkClientIdUnique(r.getClientId()));
  }

  private void checkClientIdUnique(String clientId) {
    checkUnique(
        !applicationRepository.existsByClientIdIgnoreCase(clientId),
        "An application with the same clientId already exists");
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
