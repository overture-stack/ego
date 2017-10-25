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
import org.overture.ego.model.entity.Group;
import org.overture.ego.model.entity.User;
import org.overture.ego.repository.UserGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserGroupService {
  @Autowired
  UserGroupRepository userGroupRepository;
  @Autowired
  GroupService groupService;
  @Autowired
  UserService userService;

  public Page<User> getGroupsUsers(QueryInfo queryInfo, String groupId) {
    val group = groupService.get(groupId,false);
    if(group == null) return null;
    return userService.getUsersPage(queryInfo, userGroupRepository.getAllUsers(queryInfo, group.getName()));
  }

  public Page<Group> getUsersGroup(QueryInfo queryInfo, String userId) {
    val user = userService.get(userId,false);
    if(user == null) return null;
    return groupService.getGroupsPage(queryInfo, userGroupRepository.getAllGroups(queryInfo, user.getName()));
  }

  public void add(String userName, String groupName) {
    userGroupRepository.add(userName,groupName);
  }

  public void delete(String userName, String groupName) {
    userGroupRepository.delete(userName,groupName);
  }
}
