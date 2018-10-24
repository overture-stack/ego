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

package org.overture.ego.service;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.overture.ego.model.entity.Application;
import org.overture.ego.model.enums.ApplicationStatus;
import org.overture.ego.model.search.SearchFilter;
import org.overture.ego.repository.ApplicationRepository;
import org.overture.ego.repository.queryspecification.ApplicationSpecification;
import org.overture.ego.token.app.AppTokenClaims;
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

import java.util.*;

import static java.lang.String.format;
import static java.util.UUID.fromString;
import static org.springframework.data.jpa.domain.Specifications.where;

@Service
@Slf4j
public class ApplicationService extends BaseService<Application, UUID> implements ClientDetailsService {
  public final String APP_TOKEN_PREFIX = "Basic ";
  /*
    Dependencies
   */
  @Autowired
  private ApplicationRepository applicationRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  public Application create(@NonNull Application applicationInfo) {
    return applicationRepository.save(applicationInfo);
  }

  public Application get(@NonNull String applicationId) {
    return getById(applicationRepository, fromString(applicationId));
  }

  public Application update(@NonNull Application updatedApplicationInfo) {
    Application app = getById(applicationRepository, updatedApplicationInfo.getId());
    app.update(updatedApplicationInfo);
    applicationRepository.save(app);
    return updatedApplicationInfo;
  }

  public void delete(@NonNull String applicationId) {
    applicationRepository.deleteById(fromString(applicationId));
  }

  public Page<Application> listApps(@NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return applicationRepository.findAll(ApplicationSpecification.filterBy(filters), pageable);
  }

  public Page<Application> findApps(@NonNull String query, @NonNull List<SearchFilter> filters,
    @NonNull Pageable pageable) {
    return applicationRepository.findAll(where(ApplicationSpecification.containsText(query))
      .and(ApplicationSpecification.filterBy(filters)), pageable);
  }

  public Page<Application> findUserApps(@NonNull String userId, @NonNull List<SearchFilter> filters,
    @NonNull Pageable pageable) {
    return applicationRepository.findAll(
      where(ApplicationSpecification.usedBy(fromString(userId)))
        .and(ApplicationSpecification.filterBy(filters)),
      pageable);
  }

  public Page<Application> findUserApps(@NonNull String userId, @NonNull String query,
    @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return applicationRepository.findAll(
      where(ApplicationSpecification.usedBy(fromString(userId)))
        .and(ApplicationSpecification.containsText(query))
        .and(ApplicationSpecification.filterBy(filters)),
      pageable);
  }

  public Page<Application> findGroupApplications(@NonNull String groupId, @NonNull List<SearchFilter> filters,
    @NonNull Pageable pageable) {
    return applicationRepository.findAll(
      where(ApplicationSpecification.inGroup(fromString(groupId)))
        .and(ApplicationSpecification.filterBy(filters)),
      pageable);
  }

  public Page<Application> findGroupApplications(@NonNull String groupId, @NonNull String query,
    @NonNull List<SearchFilter> filters,
    @NonNull Pageable pageable) {
    return applicationRepository.findAll(
      where(ApplicationSpecification.inGroup(fromString(groupId)))
        .and(ApplicationSpecification.containsText(query))
        .and(ApplicationSpecification.filterBy(filters)),
      pageable);
  }

  public Application getByName(@NonNull String appName) {
    return applicationRepository.findOneByNameIgnoreCase(appName);
  }

  public Application getByClientId(@NonNull String clientId) {
    return applicationRepository.findOneByClientIdIgnoreCase(clientId);
  }

  private String removeAppTokenPrefix(String token) {
    return token.replace(APP_TOKEN_PREFIX, "").trim();
  }

  public Application findByBasicToken(@NonNull String token) {
    log.error(format("Looking for token '%s'", token));
    val base64encoding = removeAppTokenPrefix(token);
    log.error(format("Decoding '%s'", base64encoding));

    val contents = new String(Base64.getDecoder().decode(base64encoding));
    log.error(format("Decoded to '%s'", contents));

    val parts = contents.split(":");
    val clientId = parts[0];
    log.error(format("Extracted client id '%s'", clientId));
    return applicationRepository.findOneByClientIdIgnoreCase(clientId);
  }

  @Override
  public ClientDetails loadClientByClientId(@NonNull String clientId) throws ClientRegistrationException {
    // find client using clientid

    val application = getByClientId(clientId);

    if (application == null) {
      throw new ClientRegistrationException("Client ID not found.");
    }

    if (!application.getStatus().equals(ApplicationStatus.APPROVED.toString())) {
      throw new ClientRegistrationException
        ("Client Access is not approved.");
    }

    // transform application to client details
    val approvedScopes = Arrays.asList(AppTokenClaims.SCOPES);
    val clientDetails = new BaseClientDetails();
    clientDetails.setClientId(clientId);
    clientDetails.setClientSecret(passwordEncoder.encode(application.getClientSecret()));
    clientDetails.setAuthorizedGrantTypes(Arrays.asList(AppTokenClaims.AUTHORIZED_GRANTS));
    clientDetails.setScope(approvedScopes);
    clientDetails.setRegisteredRedirectUri(application.getURISet());
    clientDetails.setAutoApproveScopes(approvedScopes);
    val authorities = new HashSet<GrantedAuthority>();
    authorities.add(new SimpleGrantedAuthority(AppTokenClaims.ROLE));
    clientDetails.setAuthorities(authorities);
    return clientDetails;
  }

}
