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

package bio.overture.ego.security;

import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static bio.overture.ego.model.enums.UserType.ADMIN;
import static bio.overture.ego.model.enums.UserType.USER;

import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.ApplicationType;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;

@Slf4j
@Profile("auth")
public class SecureAuthorizationManager implements AuthorizationManager {
  public boolean authorize(@NonNull Authentication authentication) {
    log.info("Trying to authorize as user");
    User user = (User) authentication.getPrincipal();
    return user.getType() == USER && isActiveUser(user);
  }

  public boolean authorizeWithAdminRole(@NonNull Authentication authentication) {
    boolean status = false;

    if (authentication.getPrincipal() instanceof User) {
      User user = (User) authentication.getPrincipal();
      log.info("Trying to authorize user '" + user.getId() + "' as admin");
      status = user.getType() == ADMIN && isActiveUser(user);
    } else if (authentication.getPrincipal() instanceof Application) {
      Application application = (Application) authentication.getPrincipal();
      log.info("Trying to authorize application '" + application.getName() + "' as admin");
      status = application.getType() == ApplicationType.ADMIN;
    } else {
      log.info("Unknown applicationType of authentication passed to authorizeWithAdminRole");
    }
    log.info("Authorization " + (status ? "succeeded" : "failed"));
    return status;
  }

  public boolean authorizeWithApplication(@NonNull Authentication authentication) {
    if (authentication.getPrincipal() instanceof Application) {
      Application application = (Application) authentication.getPrincipal();
      log.info("Authorized '" + application.getName() + "as a valid application");
      return true;
    } else {
      log.info("Invalid type of authentication passed to authorizeWithApplication");
      log.info("Authorization failed");
      return false;
    }
  }

  public boolean isActiveUser(User user) {
    return user.getStatus() == APPROVED;
  }
}
