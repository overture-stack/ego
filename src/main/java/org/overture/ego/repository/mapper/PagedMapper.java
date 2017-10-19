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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.overture.ego.model.Page;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

@Slf4j
@RequiredArgsConstructor
@Getter
public class PagedMapper<M extends ResultSetMapper<T>, T> implements ResultSetMapper<Page<T>> {

  M entityMapper;

  public PagedMapper(M mapper){
    this.entityMapper = mapper;
  }

  @Override
  public Page<T> map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {

    // create new page
    val output = new Page<T>();
    val items = new ArrayList<T>();
    while(resultSet.next()){
        if(i == 0){
          output.setTotal(resultSet.getInt("total"));
        }
        items.add(entityMapper.map(i,resultSet,statementContext));
    }
    output.setResultSet(items);
    return output;
  }
}
