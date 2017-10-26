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
import org.overture.ego.model.QueryInfo;
import org.overture.ego.model.entity.Application;
import org.overture.ego.model.entity.Group;
import org.overture.ego.model.entity.User;
import org.overture.ego.repository.ApplicationRepository;
import org.overture.ego.repository.GroupRepository;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

@Slf4j
@Component
public class UserMapper implements ResultSetMapper<User>  {

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

    try {
    if(resultSet.getString("groups") != null) {
      val groups = new ArrayList<String>();
      // add groups
      Splitter.on(",")
              .trimResults()
              .splitToList(resultSet.getString("groups"))
              .stream().forEach(groupName -> groups.add(groupName));
      user.groupNames(groups);
    }
    } catch(SQLException ex){
      // ignore exception as some of the group get queries don't need applications
    }

    try {
      if (resultSet.getString("applications") != null) {
        val applications = new ArrayList<String>();
        // add applications
        Splitter.on(",")
                .trimResults()
                .splitToList(resultSet.getString("applications"))
                .stream().forEach(applicationName -> applications.add(applicationName));
        user.applicationNames(applications);
      }
    } catch(SQLException ex){
      // ignore exception as some of the group get queries don't need applications
    }
    val output = user.build();
    output.setTotal(resultSet);
    return output;

  }

  // TODO: id is being converted to name and default sort is name - switch it to id after converting id to string
  public static String sanitizeSortField(String sort){
    if(sort.isEmpty()) return "username";
    if("id".equals(sort.toLowerCase())) return "username";//TODO: change back to id after id is string
    if("name".equals(sort.toLowerCase())) return "username";
    // set default sort if sort order is not one of the field names
    if(isSortFieldValid(sort) == false){
      return "username";
    } else return sort;
  }

  private static boolean isSortFieldValid(String sortField){
    sortField = sortField.toLowerCase();
    return ("userid".equals(sortField)    ||
            "username".equals(sortField)  ||
            "email".equals(sortField)     ||
            "role".equals(sortField)      ||
            "firstname".equals(sortField) ||
            "lastname".equals(sortField)  ||
            "createdat".equals(sortField) ||
            "lastlogin".equals(sortField) ||
            "status".equals(sortField)    ||
            "preferredlanguage".equals(sortField));
  }

}
