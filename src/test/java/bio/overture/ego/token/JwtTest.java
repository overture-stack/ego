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

import static bio.overture.ego.model.enums.AccessLevel.DENY;
import static bio.overture.ego.model.enums.AccessLevel.READ;
import static bio.overture.ego.model.enums.AccessLevel.WRITE;
import static org.junit.Assert.*;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.ApplicationType;
import bio.overture.ego.model.enums.LanguageType;
import bio.overture.ego.model.enums.StatusType;
import bio.overture.ego.service.*;
import bio.overture.ego.utils.EntityGenerator;
import java.util.*;
import java.util.stream.Collectors;
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

  @Autowired private EntityGenerator entityGenerator;
  @Autowired private ApplicationPermissionService applicationPermissionService;
  @Autowired private ApplicationService applicationService;
  @Autowired private GroupPermissionService groupPermissionService;
  @Autowired private GroupService groupService;

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

  @Test
  @SneakyThrows
  public void appJwtContainsResolvedScopes_ApplicationPermsOnly_Success() {
    val app = entityGenerator.setupApplication("TestApp", "testsecret", ApplicationType.CLIENT);
    val policies = entityGenerator.setupPolicies("SONG", "SCORE", "DACO");

    val permReq1 = PermissionRequest.builder().policyId(policies.get(0).getId()).mask(READ).build();
    val permReq2 =
        PermissionRequest.builder().policyId(policies.get(1).getId()).mask(WRITE).build();
    val permReq3 = PermissionRequest.builder().policyId(policies.get(2).getId()).mask(DENY).build();

    applicationPermissionService.addPermissions(
        app.getId(), Arrays.asList(permReq1, permReq2, permReq3));
    val appClient = applicationService.loadClientByClientId(app.getClientId());

    val appPerms = applicationService.getByClientId(app.getClientId()).getApplicationPermissions();
    assertEquals(appPerms.size(), 3);
    val permScopes =
        appPerms.stream()
            .map(AbstractPermissionService::buildScope)
            .map(Scope::toString)
            .collect(Collectors.toList());
    assertEquals(appClient.getClientId(), app.getClientId());

    List<String> scopes = new ArrayList<>(appClient.getScope());
    assertEquals(scopes.size(), permScopes.size());
    assertTrue(scopes.containsAll(permScopes) && permScopes.containsAll(scopes));
  }

  @Test
  @SneakyThrows
  public void appJwtContainsResolvedScopes_ApplicationAndGroupPerms_Success() {
    val app = entityGenerator.setupApplication("TestCombinedApp", "testsecret", ApplicationType.CLIENT);
    val group = entityGenerator.setupGroup("Test Group");
    val policies = entityGenerator.setupPolicies("SONG", "SCORE");

    val permReq1 = PermissionRequest.builder().policyId(policies.get(0).getId()).mask(READ).build();
    val permReq2 =
        PermissionRequest.builder().policyId(policies.get(0).getId()).mask(WRITE).build();
    val permReq3 = PermissionRequest.builder().policyId(policies.get(1).getId()).mask(READ).build();

    groupService.associateApplicationsWithGroup(group.getId(), Arrays.asList(app.getId()));
    applicationPermissionService.addPermissions(app.getId(), Arrays.asList(permReq1, permReq3));
    groupPermissionService.addPermissions(group.getId(), Arrays.asList(permReq2));

    val appPerms = applicationService.getByClientId(app.getClientId()).getApplicationPermissions();
    val groupPerms = groupService.getWithRelationships(group.getId()).getPermissions();

    assertEquals(appPerms.size(), 2);
    assertEquals(groupPerms.size(), 1);

    val appClient = applicationService.loadClientByClientId(app.getClientId());

    val permScopes =
        appPerms.stream()
            .map(AbstractPermissionService::buildScope)
            .map(Scope::toString)
            .collect(Collectors.toList());
    assertEquals(appClient.getClientId(), app.getClientId());

    List<String> scopes = new ArrayList<>(appClient.getScope());
    assertEquals(permScopes.size(), 2);
    assertEquals(scopes.size(), permScopes.size());
    String songWrite = Scope.createScope(policies.get(0), WRITE).toString();
    String scoreRead = Scope.createScope(policies.get(1), READ).toString();
    assertTrue(scopes.containsAll(Arrays.asList(songWrite, scoreRead)));
  }
}
