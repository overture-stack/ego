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
import org.overture.ego.model.entity.Application;
import org.overture.ego.model.entity.Group;
import org.overture.ego.model.entity.User;
import org.overture.ego.repository.UserRepository;
import org.overture.ego.repository.queryspecification.ApplicationSpecification;
import org.overture.ego.repository.queryspecification.GroupSpecification;
import org.overture.ego.repository.queryspecification.UserSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.jpa.domain.Specifications.where;

@Slf4j
@Service
public class UserService {

  @Autowired
  UserRepository userRepository;
  @Autowired
  GroupService groupService;
  @Autowired
  ApplicationService applicationService;

  public User create(User userInfo) {
    return userRepository.save(userInfo);
  }

  public void addUsersToGroups(String userId, List<String> groupIDs){
    //TODO: change id to string
    val user = userRepository.findOne(Integer.parseInt(userId));
    groupIDs.forEach(grpId -> {
      val group = groupService.get(grpId);
      user.addGroup(group);
    });
    userRepository.save(user);
  }

  public void addUsersToApps(String userId, List<String> appIDs){
    //TODO: change id to string
    val user = userRepository.findOne(Integer.parseInt(userId));
    appIDs.forEach(appId -> {
      val app = applicationService.get(appId);
      user.addApplication(app);
    });
    userRepository.save(user);
  }

  public User get(String userId) {
    //TODO: change id to string
    return userRepository.findOne(Integer.parseInt(userId));
  }

  public User getByName(String userName) {
    return userRepository.findOneByNameIgnoreCase(userName);
  }

  public User update(User updatedUserInfo) {
    return userRepository.save(updatedUserInfo);
  }

  public void delete(String userId) {
    userRepository.delete(Integer.parseInt(userId));
  }

  public Page<User> listUsers(Pageable pageable) {
    return userRepository.findAll(pageable);
  }

  public Page<User> findUsers(String query, Pageable pageable) {
    return userRepository.findAll(UserSpecification.containsText(query), pageable);
  }

  public void deleteUserFromGroup(String userId, List<String> groupIDs) {
    //TODO: change id to string
    val user = userRepository.findOne(Integer.parseInt(userId));
    groupIDs.forEach(grpId -> {
      user.removeGroup(Integer.parseInt(grpId));
    });
    userRepository.save(user);
  }

  public void deleteUserFromApp(String userId, List<String> appIDs) {
    //TODO: change id to string
    val user = userRepository.findOne(Integer.parseInt(userId));
    appIDs.forEach(appId -> {
      user.removeApplication(Integer.parseInt(appId));
    });
    userRepository.save(user);
  }

  public Page<User> findGroupsUsers(String groupId, Pageable pageable){
    return userRepository.findAll(
            UserSpecification.inGroup(Integer.parseInt(groupId)),
            pageable);
  }

  public Page<User> findGroupsUsers(String groupId, String query, Pageable pageable){
    return userRepository.findAll(
            where(UserSpecification.inGroup(Integer.parseInt(groupId)))
                    .and(UserSpecification.containsText(query)),
            pageable);
  }

  public Page<User> findAppsUsers(String appId, Pageable pageable){
    return userRepository.findAll(
            UserSpecification.ofApplication(Integer.parseInt(appId)),
            pageable);
  }

  public Page<User> findAppsUsers(String appId, String query, Pageable pageable){
    return userRepository.findAll(
            where(UserSpecification.ofApplication(Integer.parseInt(appId)))
                    .and(UserSpecification.containsText(query)),
            pageable);
  }




}
