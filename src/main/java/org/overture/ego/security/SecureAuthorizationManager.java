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

package org.overture.ego.security;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.overture.ego.model.entity.User;
import org.overture.ego.model.enums.UserRoles;
import org.overture.ego.model.enums.UserStatus;
import org.overture.ego.token.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;


@Slf4j
@Profile("auth")
public class SecureAuthorizationManager implements AuthorizationManager {

  @Autowired
  TokenService tokenService;

  public boolean authorize(@NonNull Authentication authentication) {
    User user = (User)authentication.getPrincipal();
    return UserRoles.USER.toString().equals(user.getRole()) && isActiveUser(user);
  }

  public boolean authorizeWithAdminRole(@NonNull Authentication authentication) {
    User user = (User)authentication.getPrincipal();
    return UserRoles.ADMIN.toString().equals(user.getRole()) && isActiveUser(user);
  }

  public boolean authorizeWithGroup(@NonNull Authentication authentication, String groupName) {
    User user = (User)authentication.getPrincipal();
    return authorize(authentication) && user.getGroupNames().contains(groupName);
  }

  public boolean authorizeWithApplication(@NonNull Authentication authentication, String appName) {
    User user = (User)authentication.getPrincipal();
    return authorize(authentication) && user.getApplicationNames().contains(appName);
  }

  public boolean isActiveUser(User user){
    return UserStatus.APPROVED.toString().equals(user.getStatus());
  }
}
