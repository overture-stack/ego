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
import lombok.val;
import org.overture.ego.model.entity.Group;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;


public class GroupsMapper implements ResultSetMapper<Group> {
  @Override
  public Group map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
    val group =  Group.builder().id(Integer.parseInt(resultSet.getString("grpId")))
            .name(resultSet.getString("grpName"))
            .description(resultSet.getString("description"))
            .status(resultSet.getString("status"))
            .status(resultSet.getString("status"));

    try {
      if (resultSet.getString("applications") != null) {
        val applications = new ArrayList<String>();
        // add applications
        Splitter.on(",")
                .trimResults()
                .splitToList(resultSet.getString("applications"))
                .stream().forEach(application -> applications.add(application));
        group.applicationNames(applications);
      }

    } catch (SQLException ex){
      // ignore exception as some of the group get queries don't need applications
    }

    val output = group.build();
    output.setTotal(resultSet);
    return output;
  }

}