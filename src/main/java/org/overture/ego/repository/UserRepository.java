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

package org.overture.ego.repository;

import org.overture.ego.model.QueryInfo;
import org.overture.ego.model.entity.User;
import org.overture.ego.repository.mapper.UserMapper;
import org.overture.ego.repository.sql.UserQueries;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

@RegisterMapper(UserMapper.class)
public interface UserRepository  {

  @SqlQuery(UserQueries.GET_ALL)
  List<User> getAllUsers(@BindBean QueryInfo queryInfo);

  @SqlUpdate(UserQueries.INSERT_QUERY)
  int create(@BindBean User user);

  @SqlQuery(UserQueries.GET_BY_ID)
  User read(@Bind("id") int userId);

  @SqlQuery(UserQueries.GET_BY_NAME)
  User getByName(@Bind("name") String userName);

  @SqlUpdate(UserQueries.UPDATE_QUERY)
  int update(@BindBean User user);

  @SqlUpdate(UserQueries.DELETE_QUERY)
  int delete(@Bind("id") int userId);

}
