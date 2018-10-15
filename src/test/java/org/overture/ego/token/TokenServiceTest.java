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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.overture.ego.service.ApplicationService;
import org.overture.ego.service.GroupService;
import org.overture.ego.service.UserService;
import org.overture.ego.utils.EntityGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;

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

}
