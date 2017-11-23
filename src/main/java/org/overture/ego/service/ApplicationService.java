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
import lombok.val;
import org.overture.ego.model.entity.Application;
import org.overture.ego.repository.ApplicationRepository;
import org.overture.ego.repository.queryspecification.ApplicationSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.ClientRegistrationException;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;

import static org.springframework.data.jpa.domain.Specifications.where;


@Service
public class ApplicationService implements ClientDetailsService {

  @Autowired
  private ApplicationRepository applicationRepository;

  public Application create(@NonNull Application applicationInfo) {
    return applicationRepository.save(applicationInfo);
  }

  public Application get(@NonNull String applicationId) {
    //TODO: change id to string
    return applicationRepository.findOne(Integer.parseInt(applicationId));
  }

  public Application update(@NonNull Application updatedApplicationInfo) {
    applicationRepository.save(updatedApplicationInfo);
    return updatedApplicationInfo;
  }

  public void delete(@NonNull String applicationId) {
    //TODO: change id to string
    applicationRepository.delete(Integer.parseInt(applicationId));
  }

  public Page<Application> listApps(@NonNull Pageable pageable) {
   return applicationRepository.findAll(pageable);
  }

  public Page<Application> filterAppsByStatus(@NonNull String status, @NonNull Pageable pageable) {
    return applicationRepository.findAllByStatusIgnoreCase(status, pageable);
  }

  public Page<Application> findApps(@NonNull String query, @NonNull Pageable pageable) {
    if(StringUtils.isEmpty(query)){
      return this.listApps(pageable);
    }
    return applicationRepository.findAll(ApplicationSpecification.containsText(query), pageable);
  }

  public Page<Application> findUsersApps(@NonNull String userId, @NonNull Pageable pageable){
    return applicationRepository.findAll(
            ApplicationSpecification.usedBy(Integer.parseInt(userId)),
            pageable);
  }

  public Page<Application> findUsersApps(@NonNull String userId, @NonNull String query, @NonNull Pageable pageable){
    if(StringUtils.isEmpty(query)){
      return this.findUsersApps(userId, pageable);
    }
    return applicationRepository.findAll(
            where(ApplicationSpecification.usedBy(Integer.parseInt(userId)))
                    .and(ApplicationSpecification.containsText(query)),
            pageable);
  }

  public Page<Application> findGroupsApplications(@NonNull String groupId, @NonNull Pageable pageable){
    return applicationRepository.findAll(
            ApplicationSpecification.inGroup(Integer.parseInt(groupId)),
            pageable);
  }

  public Page<Application> findGroupsApplications(@NonNull String groupId, @NonNull String query,
                                                  @NonNull Pageable pageable){
    if(StringUtils.isEmpty(query)){
      return this.findGroupsApplications(groupId,pageable);
    }
    return applicationRepository.findAll(
            where(ApplicationSpecification.inGroup(Integer.parseInt(groupId)))
                    .and(ApplicationSpecification.containsText(query)),
            pageable);
  }

  public Application getByName(@NonNull String appName) {
   return applicationRepository.findOneByNameIgnoreCase(appName);
  }

  @Override
  public ClientDetails loadClientByClientId(@NonNull String clientId) throws ClientRegistrationException {
    // find client using clientid
    val application = applicationRepository.findOneByClientIdIgnoreCase(clientId);
    //TODO: currently ignoring status field
    // transform application to client details
    val approvedScopes = Arrays.asList("read","write");
    val clientDetails = new BaseClientDetails();
    clientDetails.setClientId(clientId);
    clientDetails.setClientSecret(application.getClientSecret());
    clientDetails.setAuthorizedGrantTypes(Arrays.asList("authorization_code","client_credentials", "password", "refresh_token") );
    clientDetails.setScope(approvedScopes);// TODO: test by omitting this
    clientDetails.setRegisteredRedirectUri(application.getURISet());
    clientDetails.setAutoApproveScopes(approvedScopes);
    val authorities = new HashSet<GrantedAuthority>();
    authorities.add(new SimpleGrantedAuthority("ROLE_CLIENT"));
    clientDetails.setAuthorities(authorities);
    return clientDetails;
  }

}
