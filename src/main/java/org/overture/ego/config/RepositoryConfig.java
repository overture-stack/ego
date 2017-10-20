/*
 * Copyright (c) 2017. The Ontario Institute for Cancer Research. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.overture.ego.config;

import lombok.val;
import org.overture.ego.repository.*;
import org.skife.jdbi.v2.DBI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.sql.DataSource;

@Lazy
@Configuration
public class RepositoryConfig {

  @Autowired
  DataSource dataSource;


  @Bean
  public DBI dbi() {
    val dbi =  new DBI(dataSource);
    return dbi;
  }

  @Bean
  public UserRepository userRepository(DBI dbi) {
    return dbi.open(UserRepository.class);
  }

  @Bean
  public UserGroupRepository userGroupRepository(DBI dbi) {
    return dbi.open(UserGroupRepository.class);
  }

  @Bean
  public GroupAppRepository groupAppRepository(DBI dbi) {
    return dbi.open(GroupAppRepository.class);
  }


  @Bean
  public ApplicationRepository applicationRepository(DBI dbi) {
    return dbi.open(ApplicationRepository.class);
  }

  @Bean
  public GroupRepository groupRepository(DBI dbi) {
    return dbi.open(GroupRepository.class);
  }


}
