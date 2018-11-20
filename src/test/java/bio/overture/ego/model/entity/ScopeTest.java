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

package bio.overture.ego.model.entity;

import bio.overture.ego.model.dto.Scope;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.TestData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static bio.overture.ego.utils.CollectionUtils.listOf;
import static bio.overture.ego.utils.CollectionUtils.mapToSet;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class ScopeTest {
  @Autowired
  private EntityGenerator entityGenerator;
  public static TestData test;

  @Before
  public void initDb() {
    if (test == null) {
      test = new TestData(entityGenerator);
    }
  }

  public void testMissing(String msg, Set<Scope> have, Set<Scope> want, Set<Scope> expected) {
    val result = Scope.missingScopes(have, want);
    assertEquals(msg, expected, result);
  }

  public void testEffective(String msg, Set<Scope> have, Set<Scope> want, Set<Scope> expected) {
    val result = Scope.effectiveScopes(have, want);
    assertEquals(msg, expected, result);
  }

  @Test
  public void testMissingSame() {
    // Test for missing exactly what we have.
    // It should return an empty set.
    val have = getScopes("song.WRITE", "collab.READ");
    val expected = new HashSet<Scope>();
    testMissing("Same set", have, have, expected);
  }

  @Test
  public void testEffectiveSame() {
    // Basic sanity check. If what we have and want are the same, that's what our effective scope should be.
    val have = getScopes("song.WRITE", "collab.READ");
    testEffective("Same set", have, have, have);
  }

  @Test
  public void testMissingSubset() {
    // Test missing
    // Test for when what we have is a subset of what we want,
    // (ie. permissions are otherwise identical).

    // We should get the set difference.
    val have = getScopes("song.WRITE", "collab.READ");
    val want = getScopes("song.WRITE", "collab.READ", "id.READ");
    val expected = getScopes("id.READ");
    testMissing("Subset", have, want, expected);
  }
  @Test
  public void testEffectiveSubset() {
    // When the permissions we have is a subset of what we want,
    // our effective permissions are limited to what we have.
    val have = getScopes("song.WRITE", "collab.READ");
    val want = getScopes("song.WRITE", "collab.READ", "id.READ");
    testEffective("Subset", have, want, have);
  }

  @Test
  public void testMissingSuperset() {
    // Test to see what happens if what we have is a superset of what we want.
    // We should get an empty set (nothing missing).
    val have = getScopes("song.WRITE", "collab.READ", "id.READ");
    val want = getScopes("song.WRITE", "collab.READ");
    val expected = new HashSet<Scope>();
    testMissing("Superset", have, want, expected);
  }

  @Test
  public void testEffectiveSuperset() {
    // When the permissions we have exceed those we want,
    // our effective permissions should be limited to those we want.
    val have = getScopes("song.WRITE", "collab.READ", "id.READ");
    val want = getScopes("song.WRITE", "collab.READ");
    testEffective("Superset", have, want, want);
  }

  @Test
  public void testMissingExcessPermissions() {
    // Test what happens if we have more permissions that we want
    // We should have an empty set (nothing missing)
    val have = getScopes("song.WRITE");
    val want = getScopes("song.READ");
    val expected = new HashSet<Scope>();
    testMissing("Excess Permission", have, want, expected);
  }

  @Test
  public void testEffectiveExcessPermissions() {
    // If we have more permissions that we want,
    // our effective permissions should be those we want.
    val have = getScopes("song.WRITE");
    val want = getScopes("song.READ");
    testEffective("Excess Permission", have, want, want);
  }

  @Test
  public void testMissingInsufficientPermissions() {
    // Test what happens if we have fewer permissions that we want
    // We should get back the scope with the permission that isn't available)
    val have = getScopes("song.READ");
    val want = getScopes("song.WRITE");
    val expected = want;
    testMissing("Insufficient Permission", have, want, expected);
  }

  @Test
  public void testEffectiveInsufficientPermissions() {
    // If we have lesser permissions than those we want,
    // our effective permission should be those we have.
    val have = getScopes("song.READ");
    val want = getScopes("song.WRITE");
    testEffective("Insufficient Permission", have, want, have);
  }

  @Test
  public void testMissingWithDeny() {
    // If we have deny in the list of permissions we have,
    // it should always be missing from our list of permissions.

  }


  @Test
  public void testEffective() {
    val have = getScopes("song.WRITE", "collab.READ");
    val want = getScopes("song.READ");

    val e = Scope.effectiveScopes(have, want);
    val expected = getScopes("song.READ");
    assertTrue(e.equals(expected));
  }

  @Test
  public void testExplicit() {
    val have = getScopes("song.READ", "collab.WRITE");

    val e = Scope.explicitScopes(have);
    val expected = getScopes("song.READ","collab.READ", "collab.WRITE");
    assertEquals(expected, e);
  }

  Set<Scope> getScopes(String... scopes) {
    return mapToSet(listOf(scopes), test::getScope);
  }
}