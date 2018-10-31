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

package org.overture.ego.model.entity;

import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.overture.ego.model.enums.PolicyMask;
import org.overture.ego.model.params.ScopeName;
import org.overture.ego.service.ApplicationService;
import org.overture.ego.service.GroupService;
import org.overture.ego.service.UserService;
import org.overture.ego.token.TokenService;
import org.overture.ego.utils.EntityGenerator;
import org.overture.ego.utils.TestData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.data.util.Pair;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.common.exceptions.InvalidScopeException;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class ScopeTest {

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
  public void testMissing() {
    val have = entityGenerator.getScopes("song.upload:WRITE", "song.upload:READ");
    val missing = Scope.missingScopes(have, have);
    System.err.printf("missing='%s'",missing);
    assertTrue(missing.isEmpty());

    val want = entityGenerator.getScopes("song.upload:WRITE", "song.upload:READ", "id.create:READ");
    val missing2 = Scope.missingScopes(have, want);
    val expected = entityGenerator.getScopes("id.create:READ");
    System.err.printf("missing='%s'",missing2);
    assertTrue(missing2.equals(expected));
  }


  @Test
  public void testEffective() {
    val have = entityGenerator.getScopes("song.upload:WRITE", "song.download:READ");
    val want = entityGenerator.getScopes("song.upload:READ");

    val e = Scope.effectiveScopes(have, want);
    val expected = entityGenerator.getScopes("song.upload:READ");
    assertTrue(e.equals(expected));
  }
}