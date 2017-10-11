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

import org.overture.ego.model.entity.User;
import org.overture.ego.repository.mapper.UserMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import java.util.List;

@RegisterMapper(UserMapper.class)
public interface UserRepository {

  @SqlQuery("SELECT * FROM USERS")
  List<User> getAllUsers();

  @SqlUpdate("INSERT INTO USERS (userName, email, role, status, firstName, lastName, createdAt,lastLogin,preferredLanguage) " +
      "VALUES (:userName, :email, :role, :status, :firstName, :lastName, :createdAt, :lastLogin, :preferredLanguage)")
  int create(@BindBean User user);

  @SqlQuery("SELECT * FROM USERS WHERE userName=:userName")
  List<User> read(@Bind("userName") String username);

  @SqlUpdate("UPDATE USERS SET role=:role, status=:status," +
      "firstName=:firstName, lastName=:lastName, createdAt=:createdAt , lastLogin=:lastLogin, " +
      "preferredLanguage=:preferredLanguage WHERE userName=:userName")
  int update(@BindBean User user);

  @SqlUpdate("DELETE from USERS where userName=:userName")
  int delete(@Bind("userName") String id);

}
