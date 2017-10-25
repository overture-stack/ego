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
import org.overture.ego.model.entity.User;
import org.overture.ego.repository.UserRepository;
import org.overture.ego.repository.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Service
public class UserService {

  @Autowired
  UserRepository userRepository;
  @Autowired
  GroupService groupService;
  @Autowired
  UserGroupService userGroupService;
  @Autowired
  ApplicationService applicationService;
  @Autowired
  UserApplicationService userApplicationService;

  public User create(User userInfo) {
    userRepository.create(userInfo);
    return userRepository.getByName(userInfo.getName());
  }

  public void addUsersToGroups(String userId, List<String> groupIDs){
    //TODO: change DB schema to add id - id relationships and avoid multiple calls
    val user = userRepository.read(Integer.parseInt(userId));
    groupIDs.forEach(grpId -> {
      val group = groupService.get(grpId, false);
      userGroupService.add(user.getName(),group.getName());
    });
  }

  public void addUsersToApps(String userId, List<String> appIDs){
    //TODO: change DB schema to add id - id relationships and avoid multiple calls
    val user = userRepository.read(Integer.parseInt(userId));
    appIDs.forEach(appId -> {
      val app = applicationService.get(appId);
      userApplicationService.add(user.getName(),app.getName());
    });
  }

  public User get(String userId, boolean fullInfo) {
    int userID = Integer.parseInt(userId);
    val user = userRepository.read(userID);
    if(fullInfo){
      addGroupsAppInfo(user);
    }
    return user;
  }

  public User getByName(String userName, boolean fullInfo) {

    val user = userRepository.getByName(userName);
    if(fullInfo){
      addGroupsAppInfo(user);
    }
    return user;
  }

  public User update(User updatedUserInfo) {
    userRepository.update(updatedUserInfo);
    return updatedUserInfo;
  }

  public void delete(String userId) {
    int userID = Integer.parseInt(userId);
    userRepository.delete(userID);
  }

  public Page<User> listUsers(QueryInfo queryInfo) {
    return this.getUsersPage(queryInfo, userRepository.getAllUsers(queryInfo));
  }

  public Page<User> getUsersPage(BiFunction<QueryInfo , String, List<User>> userPageFetcher,
                                 String filter, QueryInfo queryInfo)  {
    updateQueryInfo(queryInfo);
    return getUsersPage(queryInfo, userPageFetcher.apply(queryInfo,filter));
  }

  public Page<User> getUsersPage(Function<QueryInfo,List<User>> userPageFetcher, QueryInfo queryInfo)  {
    updateQueryInfo(queryInfo);
    return getUsersPage(queryInfo, userPageFetcher.apply(queryInfo));
  }

  public void deleteUserFromGroup(String userId, List<String> groupIDs) {
    //TODO: change DB schema to add id - id relationships and avoid multiple calls
    val user = userRepository.read(Integer.parseInt(userId));
    groupIDs.forEach(grpId -> {
      val group = groupService.get(grpId, false);
      userGroupService.delete(user.getName(),group.getName());
    });
  }

  public void deleteUserFromApp(String userId, List<String> appIDs) {
    //TODO: change DB schema to add id - id relationships and avoid multiple calls
    val user = userRepository.read(Integer.parseInt(userId));
    appIDs.forEach(appId -> {
      val app = applicationService.get(appId);
      userApplicationService.delete(user.getName(),app.getName());
    });
  }

  public void addGroupsAppInfo(User user){
     user.setGroups(getUserGroups(user));
     user.setApplications(getUserApps(user));
  }

  private void updateQueryInfo(QueryInfo queryInfo){
    UserMapper.updateSortFieldName(queryInfo);
  }

  public Page<User> getUsersPage(QueryInfo queryInfo, List<User> users)  {
    return Page.getPageFromPageInfo(queryInfo,users);
  }

  private List<Group> getUserGroups(User user){
    val groups = new ArrayList<Group>();
    user.getGroupNames().forEach(groupName -> groups.add(groupService.getByName(groupName,true)));
    return groups;
  }

  private List<Application> getUserApps(User user){
    val apps = new ArrayList<Application>();
    user.getApplicationNames().forEach(appName -> apps.add(applicationService.getByName(appName)));
    return apps;
  }
}
