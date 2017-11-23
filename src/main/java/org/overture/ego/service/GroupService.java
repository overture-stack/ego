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
import org.overture.ego.model.entity.Group;
import org.overture.ego.repository.GroupRepository;
import org.overture.ego.repository.queryspecification.GroupSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

import static org.springframework.data.jpa.domain.Specifications.where;

@Service
public class GroupService {

  @Autowired
  private GroupRepository groupRepository;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private UserService userService;

  public Group create(@NonNull Group groupInfo) {
    return groupRepository.save(groupInfo);
  }

  public void addAppsToGroups(@NonNull String grpId, @NonNull List<String> appIDs){
    //TODO: change id to string
    val group = groupRepository.findOne(Integer.parseInt(grpId));
    appIDs.forEach(appId -> {
      val app = applicationService.get(appId);
      group.addApplication(app);
    });
    groupRepository.save(group);
  }

  public Group get(@NonNull String groupId) {
    //TODO: change id to string
    return groupRepository.findOne(Integer.parseInt(groupId));
  }

  public Group getByName(@NonNull String groupName) {
    return groupRepository.findOneByNameIgnoreCase(groupName);
  }

  public Group update(@NonNull Group updatedGroupInfo) {
    return groupRepository.save(updatedGroupInfo);
  }

  public void delete(@NonNull String groupId) {
    //TODO: change id to string
     groupRepository.delete(Integer.parseInt(groupId));
  }

  public Page<Group> listGroups(@NonNull Pageable pageable) {
    return groupRepository.findAll(pageable);
  }

  public Page<Group> filterGroupsByStatus(@NonNull String status, @NonNull Pageable pageable) {
    return groupRepository.findAllByStatusIgnoreCase(status, pageable);
  }

  public Page<Group> findGroups(@NonNull String query, @NonNull Pageable pageable) {
    if(StringUtils.isEmpty(query)){
      return this.listGroups(pageable);
    }
    return groupRepository.findAll(GroupSpecification.containsText(query), pageable);
  }

  public Page<Group> findUsersGroup(@NonNull String userId, @NonNull Pageable pageable){
    return groupRepository.findAll(
            GroupSpecification.containsUser(Integer.parseInt(userId)),
            pageable);
  }

  public Page<Group> findUsersGroup(@NonNull String userId, @NonNull String query, @NonNull Pageable pageable){
    if(StringUtils.isEmpty(query)){
      return this.findUsersGroup(userId, pageable);
    }
    return groupRepository.findAll(
            where(GroupSpecification.containsUser(Integer.parseInt(userId)))
                    .and(GroupSpecification.containsText(query)),
            pageable);
  }

  public Page<Group> findApplicationsGroup(@NonNull String appId, @NonNull Pageable pageable){
    return groupRepository.findAll(
            GroupSpecification.containsApplication(Integer.parseInt(appId)),
            pageable);
  }

  public Page<Group> findApplicationsGroup(@NonNull String appId, @NonNull String query, @NonNull Pageable pageable){
    if(StringUtils.isEmpty(query)){
      this.findApplicationsGroup(appId, pageable);
    }
    return groupRepository.findAll(
            where(GroupSpecification.containsApplication(Integer.parseInt(appId)))
                    .and(GroupSpecification.containsText(query)),
            pageable);
  }

  public void deleteAppsFromGroup(@NonNull String grpId, @NonNull List<String> appIDs) {
    //TODO: change id to string
    val group = groupRepository.findOne(Integer.parseInt(grpId));
    appIDs.forEach(appId -> {
      group.removeApplication(Integer.parseInt(appId));
    });
    groupRepository.save(group);
  }

  public void deleteUsersFromGroup(@NonNull String grpId, @NonNull List<String> userIDs) {
    //TODO: change id to string
    val group = groupRepository.findOne(Integer.parseInt(grpId));
    userIDs.forEach(userId -> {
      group.removeUser(Integer.parseInt(userId));
    });
    groupRepository.save(group);
  }

  public void addUsersToGroup(@NonNull String grpId, @NonNull List<String> userIDs) {
    //TODO: change id to string
    val group = groupRepository.findOne(Integer.parseInt(grpId));
    userIDs.forEach(userId -> {
      val user = userService.get(userId);
      group.addUser(user);
    });
    groupRepository.save(group);
  }

}
