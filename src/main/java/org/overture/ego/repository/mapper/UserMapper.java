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

import com.google.common.base.Splitter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.overture.ego.model.Page;
import org.overture.ego.model.entity.User;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

@Slf4j
public class UserMapper implements ResultSetMapper<User> {


  @Override
  @SneakyThrows
  public User map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {

    val user =  User.builder().id(Integer.parseInt(resultSet.getString("userid")))
        .name(resultSet.getString("userName"))
        .email(resultSet.getString("email"))
        .firstName(resultSet.getString("firstName"))
        .lastName(resultSet.getString("lastName"))
        .createdAt(resultSet.getString("createdAt"))
        .lastLogin(resultSet.getString("lastLogin"))
        .role(resultSet.getString("role"))
        .status(resultSet.getString("status"));

    if(resultSet.getString("groups") != null) {
      val groups = new ArrayList<String>();
      // add groups
      Splitter.on(",")
              .trimResults()
              .splitToList(resultSet.getString("groups"))
              .stream().forEach(group -> groups.add(group));
      user.groups(groups);
    }

    if(resultSet.getString("applications") != null) {
      val applications = new ArrayList<String>();
      // add applications
      Splitter.on(",")
              .trimResults()
              .splitToList(resultSet.getString("applications"))
              .stream().forEach(application -> applications.add(application));
      user.applications(applications);
    }
    return user.build();

  }

}
