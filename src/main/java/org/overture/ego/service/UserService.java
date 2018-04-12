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
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.overture.ego.model.entity.User;
import org.overture.ego.model.enums.UserRole;
import org.overture.ego.model.enums.UserStatus;
import org.overture.ego.model.search.SearchFilter;
import org.overture.ego.repository.UserRepository;
import org.overture.ego.repository.queryspecification.UserSpecification;
import org.overture.ego.token.IDToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.springframework.data.jpa.domain.Specifications.where;

@Slf4j
@Service
@Transactional
public class UserService extends BaseService<User> {

  /*
    Constants
   */
  // DEFAULTS
  private final static String DEFAULT_USER_ROLE = UserRole.USER.toString();
  private final static String DEFAULT_USER_STATUS = UserStatus.PENDING.toString();

  // DEMO USER
  private final static String DEMO_USER_NAME = "Demo.User@example.com";
  private final static String DEMO_USER_EMAIL = "Demo.User@example.com";
  private final static String DEMO_FIRST_NAME = "Demo";
  private final static String DEMO_LAST_NAME = "User";
  private final static String DEMO_USER_ROLE = UserRole.ADMIN.toString();
  private final static String DEMO_USER_STATUS = UserStatus.APPROVED.toString();

  /*
    Dependencies
   */
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private GroupService groupService;
  @Autowired
  private ApplicationService applicationService;
  @Autowired
  private SimpleDateFormat formatter;

  public User create(@NonNull User userInfo) {

    // Set Created At date to Now
    userInfo.setCreatedAt(formatter.format(new Date()));

    // Set UserName to equal the email.
    userInfo.setName(userInfo.getEmail());

    return userRepository.save(userInfo);
  }

  public User createFromIDToken(IDToken idToken) {
    val userInfo = new User();
    userInfo.setName(idToken.getEmail());
    userInfo.setEmail(idToken.getEmail());
    userInfo.setFirstName(StringUtils.isEmpty(idToken.getGiven_name()) ? "" : idToken.getGiven_name());
    userInfo.setLastName(StringUtils.isEmpty(idToken.getFamily_name()) ? "" : idToken.getFamily_name());
    userInfo.setStatus(DEFAULT_USER_STATUS);
    userInfo.setCreatedAt(formatter.format(new Date()));
    userInfo.setLastLogin(null);
    userInfo.setRole(DEFAULT_USER_ROLE);
    return this.create(userInfo);
  }

  public User getOrCreateDemoUser() {
    User output = getByName(DEMO_USER_NAME);

    if (output != null) {
      // Force the demo user to be ADMIN and APPROVED to allow demo access,
      // even if these values have previously been modified for the demo user.
      output.setStatus(DEMO_USER_STATUS);
      output.setRole(DEMO_USER_ROLE);
    } else {
      val userInfo = new User();
      userInfo.setName(DEMO_USER_NAME);
      userInfo.setEmail(DEMO_USER_EMAIL);
      userInfo.setFirstName(DEMO_FIRST_NAME);
      userInfo.setLastName(DEMO_LAST_NAME);
      userInfo.setStatus(UserStatus.APPROVED.toString());
      userInfo.setCreatedAt(formatter.format(new Date()));
      userInfo.setLastLogin(null);
      userInfo.setRole(UserRole.ADMIN.toString());
      output = this.create(userInfo);
    }

    return output;
  }

  public void addUsersToGroups(@NonNull String userId, @NonNull List<String> groupIDs){
    val user = getById(userRepository, Integer.parseInt(userId));
    groupIDs.forEach(grpId -> {
      val group = groupService.get(grpId);
      user.addNewGroup(group);
    });
    userRepository.save(user);
  }

  public void addUsersToApps(@NonNull String userId, @NonNull List<String> appIDs){
    val user = getById(userRepository, Integer.parseInt(userId));
    appIDs.forEach(appId -> {
      val app = applicationService.get(appId);
      user.addNewApplication(app);
    });
    userRepository.save(user);
  }

  public User get(@NonNull String userId) {
    return getById(userRepository, Integer.parseInt(userId));
  }

  public User getByName(@NonNull String userName) {
    return userRepository.findOneByNameIgnoreCase(userName);
  }

  public User update(@NonNull User updatedUserInfo) {
    val user = getById(userRepository, updatedUserInfo.getId());
    if(UserRole.USER.toString().equals(updatedUserInfo.getRole().toUpperCase()))
      updatedUserInfo.setRole(UserRole.USER.toString());
    else if(UserRole.ADMIN.toString().equals(updatedUserInfo.getRole().toUpperCase()))
      updatedUserInfo.setRole(UserRole.ADMIN.toString());
    user.update(updatedUserInfo);
    return userRepository.save(user);
  }

  public void delete(@NonNull String userId) {
    userRepository.deleteById(Integer.parseInt(userId));
  }

  public Page<User> listUsers(@NonNull List<SearchFilter> filters,@NonNull Pageable pageable) {
    return userRepository.findAll(UserSpecification.filterBy(filters), pageable);
  }

  public Page<User> findUsers(@NonNull String query, @NonNull List<SearchFilter> filters, @NonNull Pageable pageable) {
    return userRepository.findAll(
            where(UserSpecification.containsText(query))
            .and(UserSpecification.filterBy(filters)), pageable);
  }

  public void deleteUserFromGroup(@NonNull String userId, @NonNull List<String> groupIDs) {
    val user = getById(userRepository,Integer.parseInt(userId));
    groupIDs.forEach(grpId -> {
      user.removeGroup(Integer.parseInt(grpId));
    });
    userRepository.save(user);
  }

  public void deleteUserFromApp(@NonNull String userId, @NonNull List<String> appIDs) {
    val user = getById(userRepository, Integer.parseInt(userId));
    appIDs.forEach(appId -> {
      user.removeApplication(Integer.parseInt(appId));
    });
    userRepository.save(user);
  }

  public Page<User> findGroupsUsers(@NonNull String groupId, @NonNull List<SearchFilter> filters,
                                    @NonNull Pageable pageable){
    return userRepository.findAll(
            where(UserSpecification.inGroup(Integer.parseInt(groupId)))
            .and(UserSpecification.filterBy(filters)),
            pageable);
  }

  public Page<User> findGroupsUsers(@NonNull String groupId, @NonNull String query,
                                    @NonNull List<SearchFilter> filters, @NonNull Pageable pageable){
    return userRepository.findAll(
            where(UserSpecification.inGroup(Integer.parseInt(groupId)))
                    .and(UserSpecification.containsText(query))
                    .and(UserSpecification.filterBy(filters)),
            pageable);
  }

  public Page<User> findAppsUsers(@NonNull String appId, @NonNull List<SearchFilter> filters,
                                  @NonNull Pageable pageable){
    return userRepository.findAll(
            where(UserSpecification.ofApplication(Integer.parseInt(appId)))
            .and(UserSpecification.filterBy(filters)),
            pageable);
  }

  public Page<User> findAppsUsers(@NonNull String appId, @NonNull String query,
                                  @NonNull List<SearchFilter> filters,
                                  @NonNull Pageable pageable){
    return userRepository.findAll(
            where(UserSpecification.ofApplication(Integer.parseInt(appId)))
                    .and(UserSpecification.containsText(query))
                    .and(UserSpecification.filterBy(filters)),
            pageable);
  }

}
