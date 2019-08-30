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

package bio.overture.ego.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.LanguageType;
import bio.overture.ego.model.enums.StatusType;
import bio.overture.ego.service.TokenService;
import java.util.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@ActiveProfiles({"test"})
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class JwtTest {

  /** Dependencies */
  @Autowired private TokenService tokenService;

  @Test
  @SuppressWarnings("unchecked")
  @SneakyThrows
  public void jwtContainsUserGroups() {
    val groupId = UUID.randomUUID().toString();

    User user =
        Mockito.spy(
            User.builder()
                .id(UUID.randomUUID())
                .email("foobar@example.com")
                .name("foobar@example.com")
                .firstName("foo")
                .lastName("bar")
                .status(StatusType.APPROVED)
                .preferredLanguage(LanguageType.ENGLISH)
                .build());

    Mockito.when(user.getGroups()).thenReturn(List.of(groupId));

    // Generate Token String
    val token = tokenService.generateUserToken(user);

    // Parsing the token to verify groups are there
    val claims = tokenService.getTokenClaims(token);
    val userClaims = claims.get("context", LinkedHashMap.class);
    Map<String, Object> userInfo = (Map<String, Object>) userClaims.get("user");
    val groups = (Collection<String>) userInfo.get("groups");
    assertEquals(1, groups.size());
    assertTrue(groups.contains(groupId));
  }
}
