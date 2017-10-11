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
import org.overture.ego.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

  @Autowired
  UserRepository userRepository;

  public User create(User userInfo) {
    if (userInfo.getId() == null || userInfo.getId().isEmpty())
      userInfo.setId(userInfo.getUserName());
    if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty())
      userInfo.setEmail(userInfo.getUserName());
    userRepository.create(userInfo);
    return userInfo;
  }

  public User get(String userId) {
    if (userRepository.read(userId) == null || userRepository.read(userId).size() == 0)
      return null;
    else
      return userRepository.read(userId).get(0);
  }

  public User update(User updatedUserInfo) {
    userRepository.update(updatedUserInfo);
    return updatedUserInfo;
  }

  public void delete(String userId) {
    userRepository.delete(userId);
  }

  public List<User> listUsers() {
    return userRepository.getAllUsers();
  }
}
