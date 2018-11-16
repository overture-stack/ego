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

package bio.overture.ego.token;

import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.params.ScopeName;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.TestData;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import bio.overture.ego.utils.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.management.InvalidApplicationException;
import java.util.*;

import static org.junit.Assert.*;
import static bio.overture.ego.utils.CollectionUtils.listOf;
import static bio.overture.ego.utils.CollectionUtils.setOf;
import static bio.overture.ego.utils.EntityGenerator.scopeNames;
@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@Transactional
@ActiveProfiles("test")
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

  public static TestData test=null;


  @Before
  public void initDb() {
    test = new TestData(entityGenerator);
  }

  @Test
  public void generateUserToken() {
    val user = entityGenerator.setupUser("foo bar");
    val group2 = entityGenerator.setupGroup("testGroup");
    val app2 = entityGenerator.setupApplication("foo");

    group2.addUser(user);
    groupService.update(group2);

    app2.setWholeUsers(Sets.newHashSet(user));
    applicationService.update(app2);

    val token = tokenService.generateUserToken(userService.get(user.getId().toString()));
    assertNotNull(token);
  }

  @Test
  public void checkTokenWithExcessiveScopes() {
    // Create a token for the situation where a user who issued the token having had the
    // full set of scopes for the token, but now no longer does.
    //
    // We should get back only those scopes that are both in the token and that
    // the user still has.
    //
    val tokenString = "491044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val scopes = test.getScopes("song.upload:WRITE",
      "id.create:WRITE", "collab.upload:WRITE", "collab.download:WRITE");
    entityGenerator.setupToken(test.user2, tokenString,1000, scopes,null);
    val result = tokenService.checkToken(test.scoreAuth, tokenString);
    System.err.printf("result='%s'", result.toString());

    System.err.println(test.user2.getPermissions());
    assertEquals(test.scoreId, result.getClient_id() );
    assertTrue(result.getExp() > 900);
    Assert.assertEquals(test.user2.getName(), result.getUser_name());
    assertEquals(setOf("song.upload:READ"), result.getScope());
  }

  @Test
  public void checkTokenWithEmptyAppsList() {
    // Check a valid token for a user, with an empty application restriction list.
    // We should get back all the scopes that we set for our token.

    val tokenString = "591044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val scopes = test.getScopes("song.upload:READ", "song.download:READ");
    entityGenerator.setupToken(test.user2, tokenString,1000, scopes,null);

    val result = tokenService.checkToken(test.songAuth, tokenString);

    assertEquals(test.songId, result.getClient_id() );
    assertTrue(result.getExp() > 900);
    assertEquals(setOf("song.upload:READ", "song.download:READ"), result.getScope());
    Assert.assertEquals(test.user2.getName(), result.getUser_name());
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
    val scopes = test.getScopes("song.upload:READ", "song.download:READ");
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

    val scopes = test.getScopes("song.upload:WRITE", "song.download:WRITE");
    val applications = Collections.singleton(test.score);
    entityGenerator.setupToken(test.user1, tokenString,1000, scopes,applications);

    val result = tokenService.checkToken(test.scoreAuth, tokenString);

    assertEquals(test.scoreId, result.getClient_id());
    assertTrue( result.getExp() > 900);

    val expected = setOf("song.upload:WRITE", "song.download:WRITE");
    Assert.assertEquals(test.user1.getName(), result.getUser_name());
    assertEquals(expected, result.getScope());

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
    val scopes = EntityGenerator.scopeNames("collab.upload:READ", "collab.download:READ");
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
    val scopes = EntityGenerator.scopeNames("collab.upload:WRITE", "collab.download:WRITE");
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
  public void checkTokenWithLimitedScope() {
    // Check that a token issued for a subset of scopes that a user has
    // returns only the scopes listed in token
    val tokenString = "891044a1-3ffd-4164-a6a0-0e1e666b28dc";

    val scopes = test.getScopes("collab.upload:READ","collab.download:READ");
    val applications = Collections.singleton(test.score);
    entityGenerator.setupToken(test.user1, tokenString,1000,scopes,applications);

    val result = tokenService.checkToken(test.scoreAuth, tokenString);

    assertEquals(test.scoreId, result.getClient_id());
    assertTrue( result.getExp() > 900);

    val expected = setOf("collab.upload:READ", "collab.download:READ");
    Assert.assertEquals(test.user1.getName(), result.getUser_name());
    assertEquals(expected, result.getScope());

  }
  @Test
  public void issueTokenForLimitedScopes() {
    // Issue a token for a subset of the scopes the user has.
    //
    // issue_token() should return a token with values we set.
    val name = test.user1.getName();
    val scopes = EntityGenerator.scopeNames("collab.upload:READ", "collab.download:READ");
    val applications = listOf();

    val token = tokenService.issueToken(name, scopes, applications);

    assertFalse(token.isRevoked());
    Assert.assertEquals(token.getOwner().getId(), test.user1.getId());

    val s = CollectionUtils.mapToSet(token.scopes(), Scope::toString);
    val t = CollectionUtils.mapToSet(scopes, ScopeName::toString);

    System.err.printf("s='%s",s);
    System.err.printf("scopes='%s'",t);
    assertTrue(s.containsAll(t));
    assertTrue(t.containsAll(s));

    //assertTrue(s.equals(scopes));
  }

  @Test
  public void issueTokenForInvalidScope() {
    // Issue a token for a scope that does not exist ("collab.offload")
    //
    // issue_token() should throw an exception

    val name = test.user1.getName();
    val scopes = EntityGenerator.scopeNames("collab.download:READ", "collab.offload:WRITE");
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
    val scopes = EntityGenerator.scopeNames("collab.download:READ");
    val applications = listOf("NotAnApplication");

    Exception ex=null;

    try {
      tokenService.issueToken(name, scopes, applications);
    } catch (Exception e) {
      ex = e;
    }
    assertNotNull(ex);
    System.err.println(ex);
    assert ex instanceof InvalidApplicationException;

  }

  @Test
  public void testGetScope() {
    val name = new ScopeName("collab.upload:READ");
    val o = tokenService.getScope(name);
    assertNotNull(o.getPolicy());
    assertNotNull(o.getPolicy().getName());
    assertEquals("collab.upload", o.getPolicy().getName());
    assertSame(o.getPolicyMask(), AccessLevel.READ);
  }

}