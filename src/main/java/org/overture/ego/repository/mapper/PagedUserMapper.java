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

package org.overture.ego.repository.mapper;

import org.overture.ego.model.Page;
import org.overture.ego.model.entity.User;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PagedUserMapper implements ResultSetMapper<Page<User>> {

  PagedMapper<UserMapper,User> pagedMapper;
  public PagedUserMapper(){
    this.pagedMapper = new PagedMapper<UserMapper,User>(new UserMapper());
  }

  @Override
  public Page<User> map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
    return pagedMapper.map(i, resultSet, statementContext);
  }
}