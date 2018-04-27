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
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;


@Slf4j
@Profile("auth")
public class SecureAuthorizationManager implements AuthorizationManager {


  public boolean authorize(@NonNull Authentication authentication) {
    User user = (User)authentication.getPrincipal();
    return "user".equals(user.getRole().toLowerCase()) && isActiveUser(user);
  }

  public boolean authorizeWithAdminRole(@NonNull Authentication authentication) {
    User user = (User)authentication.getPrincipal();
    return "admin".equals(user.getRole().toLowerCase()) && isActiveUser(user);
  }

  public boolean authorizeWithGroup(@NonNull Authentication authentication, String groupName) {
    User user = (User)authentication.getPrincipal();
    return authorize(authentication) && user.getGroups().contains(groupName);
  }

  public boolean authorizeWithApplication(@NonNull Authentication authentication, String appName) {
    User user = (User)authentication.getPrincipal();
    return authorize(authentication) && user.getApplications().contains(appName);
  }

  public boolean isActiveUser(User user){
    return "approved".equals(user.getStatus().toLowerCase());
  }

}
