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

package org.overture.ego.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.overture.ego.model.entity.User;
import org.overture.ego.service.UserService;
import org.overture.ego.token.GoogleTokenValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/login")
public class LoginController {

  @Autowired
  UserService userService;
  @Autowired
  GoogleTokenValidator tokenValidator;
  @Autowired
  SimpleDateFormat formatter;



  List<String> admins = Arrays.asList(new String[]{"ra.vrma@gmail.com"});

  @RequestMapping(method = RequestMethod.GET, value = "/google")
  @ResponseStatus(value = HttpStatus.OK)
  @SneakyThrows
  public @ResponseBody
  List<User> loginWithGoogle(@RequestHeader(value = "id_token", required = true) String idToken) {
    // validate token from google
    if (!tokenValidator.validToken(idToken))
      throw new Exception("Invalid user token:" + idToken);
    val tokenDecoded = JwtHelper.decode(idToken);
    val authInfo = new ObjectMapper().readValue(tokenDecoded.getClaims(), Map.class);
    val userName = authInfo.get("email").toString();
    val user = createNewUser(userName);
    if (userService.get(userName) == null)
      userService.create(user);
    if (isAdminUser(userName))
      return userService.listUsers();
    else return Arrays.asList(new User[]{userService.get(userName)});

  }

  private User createNewUser(String userName) {
    String role = "USER";
    String status = "Pending";
    if (isAdminUser(userName)) {
      role = "ADMIN";
      status = "Approved";
    }

    return User.builder()
        .name(userName)
        .email(userName)
        .firstName("")
        .lastName("")
        .status(status)
        .createdAt(formatter.format(new Date()))
        .lastLogin(null)
        .role(role).build();

  }

  private boolean isAdminUser(String userName) {
    return admins.contains(userName);
  }
}
