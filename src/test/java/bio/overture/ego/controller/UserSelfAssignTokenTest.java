/*
 * Copyright (c) 2019. The Ontario Institute for Cancer Research. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package bio.overture.ego.controller;

import static org.junit.Assert.assertEquals;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.enums.ApplicationType;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.utils.EntityGenerator;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;

@Slf4j
@ActiveProfiles({"auth", "secure", "test"})
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserSelfAssignTokenTest extends AbstractControllerTest {

  /** Dependencies */
  @Autowired private EntityGenerator entityGenerator;

  @Autowired private TokenService tokenService;

  /** State */
  @Value("${logging.test.controller.enable}")
  private boolean enableLogging;

  private static boolean hasRunEntitySetup = false;
  private static Map<String, UUID> idMap = new HashMap<>();
  private static Map<String, String> tokenMap = new HashMap<>();
  private static Application adminApp;
  private static Application clientApp;

  @Override
  protected boolean enableLogging() {
    return enableLogging;
  }

  @Override
  protected void beforeTest() {
    // Initial setup of entities (run once
    if (!hasRunEntitySetup) {
      hasRunEntitySetup = true;

      List<User> userList = new ArrayList<>();

      userList.addAll(entityGenerator.setupPublicUsers("User SelfAssign", "User OtherAssign"));
      userList.addAll(
          entityGenerator.setupUsers("User SelfAssignAdminOne", "User SelfAssignAdminTwo"));

      val policy = entityGenerator.setupSinglePolicy("USERSELFASSIGNMENTPOLICY");
      entityGenerator.addPermissionToUsers(userList, policy, AccessLevel.WRITE);

      // No guarantee of ordering
      for (val user : userList) {
        idMap.put(user.getLastName(), user.getId());
        tokenMap.put(user.getLastName(), tokenService.generateUserToken(user));
      }

      clientApp =
          entityGenerator.setupApplication(
              "clientappfortokenassignment", "foo", ApplicationType.CLIENT);
      adminApp =
          entityGenerator.setupApplication(
              "adminappfortokenassignment", "bar", ApplicationType.ADMIN);
    }
  }

  /** Let first user self assign an access token. Should succeed. */
  @Test
  public void userNotAdmin_assignTokenToSelf_success() {
    val userId = idMap.get("SelfAssign");
    val token = tokenMap.get("SelfAssign");

    val params = new LinkedMultiValueMap<String, Object>();
    params.add("user_id", userId.toString());
    params.add("scopes", "USERSELFASSIGNMENTPOLICY.READ");
    params.add("description", "assign to myself");

    val bearerToken = "Bearer " + token;
    val headers = new LinkedMultiValueMap<String, String>();
    headers.add("Authorization", bearerToken);

    val response =
        initStringRequest()
            .endpoint("o/api_key")
            .body(params)
            .headers(new HttpHeaders(headers))
            .post();
    assertEquals(200, response.getStatusCodeValue());
  }

  /** User first user to try to assign new token for second user. Should fail. */
  @Test
  public void userNotAdmin_assignTokenToOther_failure() {
    val userId = idMap.get("OtherAssign");
    val token = tokenMap.get("SelfAssign");

    val params = new LinkedMultiValueMap<String, Object>();
    params.add("user_id", userId.toString());
    params.add("scopes", "USERSELFASSIGNMENTPOLICY.READ");
    params.add("description", "assign to other");

    val bearerToken = "Bearer " + token;
    val headers = new LinkedMultiValueMap<String, String>();
    headers.add("Authorization", bearerToken);

    val response =
        initStringRequest()
            .endpoint("o/api_key")
            .body(params)
            .headers(new HttpHeaders(headers))
            .post();
    assertEquals(403, response.getStatusCodeValue());
  }

  /** Admin users can assign to anyone */
  @Test
  public void userAdmin_assignTokenToOther_success() {
    val userId = idMap.get("OtherAssign");
    val token = tokenMap.get("SelfAssignAdminOne");

    val params = new LinkedMultiValueMap<String, Object>();
    params.add("user_id", userId.toString());
    params.add("scopes", "USERSELFASSIGNMENTPOLICY.WRITE");
    params.add("description", "assign to other");

    val bearerToken = "Bearer " + token;
    val headers = new LinkedMultiValueMap<String, String>();
    headers.add("Authorization", bearerToken);

    val response =
        initStringRequest()
            .endpoint("o/api_key")
            .body(params)
            .headers(new HttpHeaders(headers))
            .post();
    assertEquals(200, response.getStatusCodeValue());
  }

  /** Admin applications can assign to anyone */
  @Test
  public void adminApplication_assignTokenToOther_success() {
    val userId = idMap.get("SelfAssignAdminTwo");
    val token = tokenService.generateAppToken(adminApp);

    val params = new LinkedMultiValueMap<String, Object>();
    params.add("user_id", userId.toString());
    params.add("scopes", "USERSELFASSIGNMENTPOLICY.WRITE");
    params.add("description", "assign to other");

    val bearerToken = "Bearer " + token;
    val headers = new LinkedMultiValueMap<String, String>();
    headers.add("Authorization", bearerToken);

    val response =
        initStringRequest()
            .endpoint("o/api_key")
            .body(params)
            .headers(new HttpHeaders(headers))
            .post();
    assertEquals(200, response.getStatusCodeValue());
  }

  /** Client applications cannot assign */
  @Test
  public void clientApplication_assignTokenToOther_failure() {
    val userId = idMap.get("SelfAssignAdminTwo");
    val token = tokenService.generateAppToken(clientApp);

    val params = new LinkedMultiValueMap<String, Object>();
    params.add("user_id", userId.toString());
    params.add("scopes", "USERSELFASSIGNMENTPOLICY.WRITE");
    params.add("description", "assign to other");

    val bearerToken = "Bearer " + token;
    val headers = new LinkedMultiValueMap<String, String>();
    headers.add("Authorization", bearerToken);

    val response =
        initStringRequest()
            .endpoint("o/api_key")
            .body(params)
            .headers(new HttpHeaders(headers))
            .post();
    assertEquals(403, response.getStatusCodeValue());
  }
}
