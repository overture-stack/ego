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

import lombok.val;
import org.overture.ego.model.Page;
import org.overture.ego.model.QueryInfo;
import org.overture.ego.model.entity.Application;
import org.overture.ego.model.entity.Group;
import org.overture.ego.repository.GroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GroupService {

  @Autowired
  GroupRepository groupRepository;
  @Autowired
  ApplicationService applicationService;
  @Autowired
  GroupApplicationService groupApplicationService;
  @Autowired
  UserGroupService userGroupService;

  public Group create(Group groupInfo) {
    groupRepository.create(groupInfo);
    return groupRepository.getByName(groupInfo.getName());
  }

  public void addAppsToGroups(String grpId, List<String> appIDs){
    val group = groupRepository.read(Integer.parseInt(grpId));
    appIDs.forEach(appId -> {
      val app = applicationService.get(appId);
      groupApplicationService.add(group.getName(),app.getName());
    });
  }

  public Group get(String groupId, boolean fullInfo) {
    int groupID = Integer.parseInt(groupId);
    if (groupRepository.read(groupID) == null) {
      return null;
    }
    else {
      val group = groupRepository.read(groupID);
      if(fullInfo){
        addAppInfo(group);
      }
      return group;
    }
  }

  public Group getByName(String groupName, boolean fullInfo) {

    val group = groupRepository.getByName(groupName);
    if(fullInfo){
      addAppInfo(group);
    }
    return group;
  }

  public Group update(Group updatedGroupInfo) {
    groupRepository.update(updatedGroupInfo);
    return updatedGroupInfo;
  }

  public void delete(String groupId) {
    int groupID = Integer.parseInt(groupId);

    groupRepository.delete(groupID);
  }

  public Page<Group> listGroups(QueryInfo queryInfo) {
    return getGroupsPage(queryInfo, groupRepository.getAllGroups(queryInfo));
  }

  public Page<Group> getGroupsPage(QueryInfo queryInfo, List<Group> groups) {
    return Page.getPageFromPageInfo(queryInfo,groups);
  }

  public void addAppInfo(Group group){
    val apps = new ArrayList<Application>();
    group.getApplicationNames().forEach(appName -> apps.add(applicationService.getByName(appName)));
    group.setApplications(apps);
  }


  public void deleteAppsFromGroup(String grpId, List<String> appIDs) {
    //TODO: change DB schema to add id - id relationships and avoid multiple calls
    val group = groupRepository.read(Integer.parseInt(grpId));
    appIDs.forEach(appId -> {
      val app = applicationService.get(appId);
      groupApplicationService.delete(group.getName(),app.getName());
    });
  }

  public void deleteUsersFromGroup(String grpId, List<String> userIDs) {
    //TODO: change DB schema to add id - id relationships and avoid multiple calls
    val group = groupRepository.read(Integer.parseInt(grpId));
    userIDs.forEach(userId -> {
      val user = applicationService.get(userId);
      userGroupService.delete(user.getName(),group.getName());
    });
  }

  public void addUsersToGroup(String grpId, List<String> userIDs) {
    val group = groupRepository.read(Integer.parseInt(grpId));
    userIDs.forEach(userId -> {
      val user = applicationService.get(userId);
      userGroupService.add(user.getName(),group.getName());
    });
  }
}
