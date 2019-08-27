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

import static org.junit.Assert.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.utils.EntityGenerator;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.LinkedHashMap;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@ActiveProfiles({"test", "auth"})
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UpdateTokenTest extends AbstractControllerTest {

  /** Config */
  @Value("${logging.test.controller.enable}")
  private boolean enableLogging;

  /** Dependencies */
  @Autowired private EntityGenerator entityGenerator;

  @Autowired private TokenService tokenService;

  /** State */
  private HttpHeaders tokenHeaders = new HttpHeaders();

  private User user;
  private Policy policy;

  @Override
  protected void beforeTest() {
    val adminUser = entityGenerator.setupUser("Admin Updatetokenson");
    tokenHeaders.add(AUTHORIZATION, "Bearer " + tokenService.generateUserToken(adminUser));

    user = entityGenerator.setupPublicUsers("Update Token").get(0);
    policy = entityGenerator.setupSinglePolicy("UpdateTokenTestPolicy");
  }

  @Override
  protected boolean enableLogging() {
    return enableLogging;
  }

  @Test
  @SneakyThrows
  public void updateTokenTest() {
    val firstToken = tokenService.generateUserToken(user);
    log.debug(firstToken);

    // Add permission on policy for user
    val permissionRequest =
        ImmutableList.of(
            PermissionRequest.builder().policyId(policy.getId()).mask(AccessLevel.WRITE).build());
    val permissionResponse =
        initStringRequest()
            .endpoint("/users/%s/permissions", user.getId().toString())
            .headers(tokenHeaders)
            .body(permissionRequest)
            .post();

    assertEquals(HttpStatus.OK, permissionResponse.getStatusCode());

    // Ensure one second of epoch time has passed
    Thread.sleep(1000);

    val userHeaders = new HttpHeaders();
    userHeaders.add(AUTHORIZATION, "Bearer " + firstToken);

    val updateResponse =
        initStringRequest().endpoint("/oauth/update-ego-token").headers(userHeaders).get();
    assertEquals(HttpStatus.OK, updateResponse.getStatusCode());

    val controlToken = tokenService.generateUserToken(user);

    val updatedToken = updateResponse.getBody();
    log.debug(updatedToken);

    val firstTokenClaims = tokenService.validateAndReturn(firstToken).getBody();
    val updatedTokenClaims = tokenService.validateAndReturn(updatedToken).getBody();
    val controlTokenClaims = tokenService.validateAndReturn(controlToken).getBody();

    // First and Update should have same expiry
    assertEquals(firstTokenClaims.getExpiration(), updatedTokenClaims.getExpiration());

    // Updated and control should not have same expiry
    assertNotEquals(updatedTokenClaims.getExpiration(), controlTokenClaims.getExpiration());

    // Expiry of control should be later than updated expiry
    assertTrue(
        updatedTokenClaims.getExpiration().getTime()
            < controlTokenClaims.getExpiration().getTime());

    val firstContext = firstTokenClaims.get("context", LinkedHashMap.class);
    val updatedContext = updatedTokenClaims.get("context", LinkedHashMap.class);

    assertTrue(((Collection) firstContext.get("scope")).isEmpty()); // No scopes originally
    assertFalse(
        ((Collection) updatedContext.get("scope")).isEmpty()); // Has scopes in updated token

    assertTrue(((Collection) updatedContext.get("scope")).contains("UpdateTokenTestPolicy.WRITE"));
  }
}
