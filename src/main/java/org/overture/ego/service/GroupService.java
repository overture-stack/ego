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
import org.overture.ego.model.entity.AclEntity;
import org.overture.ego.model.entity.Group;
import org.overture.ego.model.enums.AclMask;
import org.overture.ego.model.search.SearchFilter;
import org.overture.ego.repository.GroupRepository;
import org.overture.ego.repository.queryspecification.GroupSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.jpa.domain.Specifications.where;

@Service
public class GroupService extends BaseService<Group> {

  @Autowired
  private GroupRepository groupRepository;

  @Autowired
  private ApplicationService applicationService;

  public Group create(@NonNull Group groupInfo) {
    return groupRepository.save(groupInfo);
  }

  public void addAppsToGroup(@NonNull String grpId, @NonNull List<String> appIDs){
    val group = getById(groupRepository, Integer.parseInt(grpId));
    appIDs.forEach(appId -> {
      val app = applicationService.get(appId);
      group.addApplication(app);
    });
    groupRepository.save(group);
  }

  public void addGroupPermissions(@NonNull String groupId, @NonNull List<Pair<AclEntity, AclMask>> permissions) {
    val group = getById(groupRepository, Integer.parseInt(groupId));
    permissions.forEach(permission -> {
      group.addNewPermission(permission.getFirst(), permission.getSecond());
    });
    groupRepository.save(group);
  }

  public Group get(@NonNull String groupId) {
    return getById(groupRepository, Integer.parseInt(groupId));
  }

  public Group getByName(@NonNull String groupName) {
    return groupRepository.findOneByNameIgnoreCase(groupName);
  }

  public Group update(@NonNull Group updatedGroupInfo) {
    Group group = getById(groupRepository,updatedGroupInfo.getId());
    group.update(updatedGroupInfo);
    return groupRepository.save(group);
  }

  public void delete(@NonNull String groupId) {
     groupRepository.deleteById(Integer.parseInt(groupId));
  }

  public Page<Group> listGroups(@NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return groupRepository.findAll(GroupSpecification.filterBy(filters), pageable);
  }

  public Page<Group> findGroups(@NonNull String query, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return groupRepository.findAll(where(GroupSpecification.containsText(query))
            .and(GroupSpecification.filterBy(filters)), pageable);
  }

  public Page<Group> findUserGroups(@NonNull String userId, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable){
    return groupRepository.findAll(
            where(GroupSpecification.containsUser(Integer.parseInt(userId)))
            .and(GroupSpecification.filterBy(filters)),
            pageable);
  }

  public Page<Group> findUserGroups(@NonNull String userId, @NonNull String query, @NonNull List<SearchFilter> filters,
                                    @NonNull Pageable pageable){
    return groupRepository.findAll(
            where(GroupSpecification.containsUser(Integer.parseInt(userId)))
                    .and(GroupSpecification.containsText(query))
                    .and(GroupSpecification.filterBy(filters)),
            pageable);
  }

  public Page<Group> findApplicationGroups(@NonNull String appId, @NonNull List<SearchFilter> filters,
                                           @NonNull Pageable pageable){
    return groupRepository.findAll(
            where(GroupSpecification.containsApplication(Integer.parseInt(appId)))
            .and(GroupSpecification.filterBy(filters)),
            pageable);
  }

  public Page<Group> findApplicationGroups(@NonNull String appId, @NonNull String query,
                                           @NonNull List<SearchFilter> filters, @NonNull Pageable pageable){
    return groupRepository.findAll(
            where(GroupSpecification.containsApplication(Integer.parseInt(appId)))
                    .and(GroupSpecification.containsText(query))
            .and(GroupSpecification.filterBy(filters)),
            pageable);
  }

  public void deleteAppsFromGroup(@NonNull String grpId, @NonNull List<String> appIDs) {
    val group = getById(groupRepository,Integer.parseInt(grpId));
    appIDs.forEach(appId -> {
      // TODO if app id not valid (does not exist) we need to throw EntityNotFoundException
      group.removeApplication(Integer.parseInt(appId));
    });
    groupRepository.save(group);
  }

  public void deleteGroupPermissions(@NonNull String userId, @NonNull List<String> permissionsIds) {
    val group = getById(groupRepository, Integer.parseInt(userId));
    permissionsIds.forEach(permissionsId -> {
      group.removePermission(Integer.parseInt(permissionsId));
    });
    groupRepository.save(group);
  }
}
