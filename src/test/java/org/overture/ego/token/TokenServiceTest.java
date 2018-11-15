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

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.overture.ego.model.entity.User;
import org.overture.ego.service.UserService;
import org.overture.ego.utils.EntityGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.util.Pair;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class TokenServiceTest {
  @Autowired
  private UserService userService;

  @Autowired
  private EntityGenerator entityGenerator;

  @Autowired
  private TokenService tokenService;

  @Test
  public void testLastloginUpdate(){
    IDToken idToken = new IDToken();
    idToken.setFamily_name("fName");
    idToken.setGiven_name("gName");
    idToken.setEmail("fNamegName@domain.com");
    User user = userService.create(entityGenerator.createOneUser(Pair.of("fName", "gName")));

    assertNull(" Verify before generatedUserToken, last login after fetching the user should be null. ",
            userService.getByName(idToken.getEmail()).getLastLogin());

    tokenService.generateUserToken(idToken);
    user = userService.getByName(idToken.getEmail());

    assertNotNull("Verify after generatedUserToken, last login is not null.", user.getLastLogin());

    // Must manually delete user. Using @Transactional will
    // trigger exception, as there are two
    // threads involved, new thread will try to find user in an empty repo which
    // will cause exception.
    userService.delete(user.getId().toString());

  }
}
