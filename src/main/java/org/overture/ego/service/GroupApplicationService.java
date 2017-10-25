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

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.overture.ego.model.Page;
import org.overture.ego.model.QueryInfo;
import org.overture.ego.model.entity.Application;
import org.overture.ego.model.entity.Group;
import org.overture.ego.repository.GroupAppRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GroupApplicationService {
  @Autowired
  GroupAppRepository groupAppRepository;
  @Autowired
  GroupService groupService;
  @Autowired
  ApplicationService applicationService;

  public Page<Application> getGroupsApplications(QueryInfo queryInfo, String groupId) {
    val group = groupService.get(groupId,false);
    if(group == null) return null;
    return applicationService.getAppsPage(queryInfo, groupAppRepository.getAllApps(queryInfo, group.getName()));
  }

  public Page<Group> getApplicationsGroup(QueryInfo queryInfo, String appId) {
    val app = applicationService.get(appId);
    if(app == null) return null;
    return groupService.getGroupsPage(queryInfo, groupAppRepository.getAllGroups(queryInfo, app.getName()));
  }

  public void add(String appName, String groupName) {
    groupAppRepository.add(groupName, appName);
  }

  public void delete(String appName, String groupName) {
    groupAppRepository.delete(groupName, appName);
  }
}
