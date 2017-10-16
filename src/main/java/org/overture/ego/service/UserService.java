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

import org.overture.ego.model.entity.User;
import org.overture.ego.repository.UserGroupRepository;
import org.overture.ego.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

  @Autowired
  UserRepository userRepository;
  @Autowired
  UserGroupRepository userGroupRepository;

  public User create(User userInfo) {
    userRepository.create(userInfo);
    return userRepository.getByName(userInfo.getUserName());
  }

  public void addUsersToGroups(String userId, List<String> groupIds){
    //TODO: remove casting to Int. Updated DB design will have strings for ids
    int userID = Integer.parseInt(userId);
    groupIds.forEach(grpId -> {
      int groupId = Integer.parseInt(grpId);
      userGroupRepository.add(userID,groupId);});
  }

  public User get(String userId) {
    int userID = Integer.parseInt(userId);
    if (userRepository.read(userID) == null)
      return null;
    else
      return userRepository.read(userID);
  }

  public User update(User updatedUserInfo) {
    userRepository.update(updatedUserInfo);
    return updatedUserInfo;
  }

  public void delete(String userId) {
    int userID = Integer.parseInt(userId);

    userRepository.delete(userID);
  }

  public List<User> listUsers() {
    return userRepository.getAllUsers();
  }


}
