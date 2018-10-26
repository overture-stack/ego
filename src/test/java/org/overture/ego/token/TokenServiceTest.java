/*
 * Copyright (c) 2018. The Ontario Institute for Cancer Research. All rights reserved.
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

package org.overture.ego.token;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.overture.ego.service.ApplicationService;
import org.overture.ego.service.GroupService;
import org.overture.ego.service.UserService;
import org.overture.ego.utils.EntityGenerator;
import org.overture.ego.utils.TestData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.management.InvalidApplicationException;
import java.util.*;

import static org.junit.Assert.*;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Ignore
public class TokenServiceTest {
  @Autowired
  private ApplicationService applicationService;

  @Autowired
  private UserService userService;

  @Autowired
  private GroupService groupService;

  @Autowired
  private EntityGenerator entityGenerator;

  @Autowired
  private TokenService tokenService;

  public static TestData test;


  @Before
  public void initDb() {
    if (test == null) {
      test = new TestData(entityGenerator);
    }
  }

  @Test
  public void generateUserToken() {
    val user = userService.create(entityGenerator.createOneUser(Pair.of("foo", "bar")));
    groupService.create(entityGenerator.createOneGroup("testGroup"));
    applicationService.create(entityGenerator.createOneApplication("foo"));

    val group2 = groupService.getByName("testGroup");
    group2.addUser(user);
    groupService.update(group2);

    val app2 = applicationService.getByClientId("foo");
    app2.setWholeUsers(Sets.newHashSet(user));
    applicationService.update(app2);

    val token = tokenService.generateUserToken(userService.get(user.getId().toString()));
    assertNotNull(token);
  }

  @Test
  public void checkTokenWithExcessiveScopes() {
    // Create a token for a user who issued the token having had the
    // full set of scopes for the token, but now no longer does.
    //
    // We should get back only those scopes that are both in the token and that
    // the user still has.
    //
    val tokenString = "491044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val scopes = test.policies("song.upload", "id.create", "collab.upload", "collab.download");
    entityGenerator.setupToken(test.user2, tokenString,1000, scopes,null);
    val result = tokenService.checkToken(test.scoreAuth, tokenString);

    System.err.println(test.user2.getPermissions());
    assertEquals(test.scoreId, result.getClient_id() );
    assertTrue(result.getExp() > 900);
    assertEquals(setOf("song.upload"), result.getScope());
    assertEquals(test.user2.getName(), result.getUser_name());
  }

  @Test
  public void checkTokenWithEmptyAppsList() {
    // Check a valid token for a user, with an empty application restriction list.
    // We should get back all the scopes that we set for our token.

    val tokenString = "591044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val scopes = test.policies("song.upload", "song.download");
    entityGenerator.setupToken(test.user1, tokenString,1000, scopes,null);

    val result = tokenService.checkToken(test.songAuth, tokenString);

    assertEquals(test.songId, result.getClient_id() );
    assertTrue(result.getExp() > 900);
    assertEquals(setOf("song.upload", "song.download"), result.getScope());
    assertEquals(test.user1.getName(), result.getUser_name());
  }

  @Test
  public void checkTokenWithWrongAuthToken() {
    // Create a token with an application restriction list
    // ("score"), and then try to check it with an authentication
    // token for an application("song") that isn't on the token's
    // application list.
    //
    // check_token should fail with an InvalidToken exception.
    val tokenString = "691044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val scopes = test.policies("song.upload", "song.download");
    val applications = Collections.singleton(test.score);
    entityGenerator.setupToken(test.user1, tokenString,1000, scopes,applications);

    InvalidTokenException ex=null;
    try {
      tokenService.checkToken(test.songAuth, tokenString);
    } catch (InvalidTokenException e) {
      ex = e;
    }
    assertNotNull(ex);
  }

  @Test
  public void checkTokenWithRightAuthToken() {
    // Create a token with an application restriction list
    // ("score"), and then try to check it with the same
    // auth token.
    //
    // We should get back the values we sent.
    val tokenString = "791044a1-3ffd-4164-a6a0-0e1e666b28dc";

    val scopes = test.policies("song.upload", "song.download");
    val applications = Collections.singleton(test.score);
    entityGenerator.setupToken(test.user1, tokenString,1000, scopes,applications);

    val result = tokenService.checkToken(test.scoreAuth, tokenString);

    assertEquals(test.scoreId, result.getClient_id());
    assertTrue( result.getExp() > 900);
    assertEquals(setOf("song.upload", "song.download"), result.getScope());
    assertEquals(test.user1.getName(), result.getUser_name());
  }

  @Test
  public void checkTokenNullToken() {
    // check_token() should fail with an InvalidTokenException
    // if we pass it a null value for a token.

    InvalidTokenException ex=null;
    try {
      tokenService.checkToken(test.songAuth, null);
    } catch (InvalidTokenException e) {
      ex = e;
    }
    assertNotNull(ex);
  }

  @Test
  public void checkTokenDoesNotExist() {
    // check_token() should fail if we pass it a value for a
    // token that we can't find.
    InvalidTokenException ex=null;
    try {
      tokenService.checkToken(test.songAuth, "fakeToken");
    } catch (InvalidTokenException e) {
      ex = e;
    }
    assertNotNull(ex);
  }

  @Test
  public void issueTokenForInvalidUser() {
    // Try to issue a token for a user that does not exist
    val name="Invalid";
    val scopes = listOf("collab.upload", "collab.download");
    val applications = listOf("song", "score");

    UsernameNotFoundException ex=null;
    try {
      tokenService.issueToken(name, scopes, applications);
    } catch (UsernameNotFoundException e) {
      ex = e;
    }

    assertNotNull(ex);
  }

  @Test
  public void issueTokenWithExcessiveScope() {
    // Try to issue a token for a user that exists, but with scopes that the user
    // does not have access to.
    //
    // issueToken() should throw an InvalidScope exception
    val name = test.user2.getName();
    val scopes = listOf("collab.upload", "collab.download");
    val applications = listOf();

    InvalidScopeException ex=null;

    try {
      tokenService.issueToken(name, scopes, applications);
    } catch (InvalidScopeException e) {
      ex = e;
    }
    assertNotNull(ex);

  }
  @Test
  public void issueTokenForLimitedScopes() {
    // Issue a token for a subset of the scopes the user has.
    //
    // issue_token() should return a token with values we set.
    val name = test.user1.getName();
    val scopes = listOf("collab.upload", "collab.download");
    val applications = listOf();

    val token = tokenService.issueToken(name, scopes, applications);

    assertFalse(token.isRevoked());
    assertEquals(token.getOwner().getId(), test.user1.getId());
    assertTrue(token.getScopes().equals(scopes));
  }

  @Test
  public void issueTokenForInvalidScope() {
    // Issue a token for a scope that does not exist.
    //
    // issue_token() should throw an exception

    val name = test.user1.getName();
    val scopes = listOf("collab.download", "collab.offload");
    val applications = listOf();

    InvalidScopeException ex=null;

    try {
      tokenService.issueToken(name, scopes, applications);
    } catch (InvalidScopeException e) {
      ex = e;
    }
    assertNotNull(ex);
  }

  @Test
  public void issueTokenForInvalidApp() {
    // Issue a token for an application that does not exist.
    //
    // issue_token() should throw an exception

    val name = test.user1.getName();
    val scopes = listOf("collab.download", "id.create");
    val applications = listOf("NotAnApplication");

    Exception ex=null;

    try {
      tokenService.issueToken(name, scopes, applications);
    } catch (Exception e) {
      ex = e;
    }
    assertNotNull(ex);
    assertTrue(ex instanceof InvalidApplicationException);
  }

  private static Set<String> setOf(String... strings) {
    return new HashSet<>(Arrays.asList(strings));
  }

  private static List<String> listOf(String... strings) { return Arrays.asList(strings);}
}