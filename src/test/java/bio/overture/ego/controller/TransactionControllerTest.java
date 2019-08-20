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

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.TransactionalDeleteRequest;
import bio.overture.ego.model.dto.TransactionalGroupPermissionRequest;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.enums.ApplicationType;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.utils.EntityGenerator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

import static org.junit.Assert.assertEquals;

@Slf4j
@ActiveProfiles({"test"})
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TransactionControllerTest extends AbstractControllerTest {
  @Value("${logging.test.controller.enable}")
  private boolean enableLogging;

  @Override
  protected boolean enableLogging() {
    return enableLogging;
  }

@Override
 protected void beforeTest() { }

  /** Before: Group does not exist. Policy does not exist. Permission does not exist.
   * After: Group exists, with permission "".
   * */
  @Test
  public void createGroupPermissionsSuccess() {
    val request = List.of(new TransactionalGroupPermissionRequest("group1","policy1",
      AccessLevel.WRITE));
    val response = createPermissions(request);
    assertEquals(200, response.getStatusCodeValue());
  }

  @Test
    public void deleteSuccessful() {
    val request = new TransactionalDeleteRequest(List.of("group1"),List.of("policy1"));
    val response = delete(request);
    assertEquals(200, response.getStatusCodeValue());
  }

  ResponseEntity<String> createPermissions(List<TransactionalGroupPermissionRequest> request) {
    return initStringRequest()
      .endpoint("transaction/group_permissions")
      .body(request)
      .post();
  }

  ResponseEntity<String> delete(TransactionalDeleteRequest request) {
    return initStringRequest()
      .endpoint("transaction/mass_delete")
      .body(request)
      .post();
  }

}
