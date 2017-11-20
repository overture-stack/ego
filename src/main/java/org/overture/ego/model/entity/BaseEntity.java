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

package org.overture.ego.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Splitter;
import lombok.Builder;
import lombok.Data;
import lombok.val;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
public abstract class BaseEntity {
  @JsonIgnore
  int total;

  public void setTotal(ResultSet resultSet){
    // TODO: Bit of a hack - implement using PagedMapper and DBI Factories/ Stored Procedures
    // Overriding the user object to get total rows in the table as an extra column in the table
    try{
      this.total = resultSet.getInt("total");
    } catch(Exception ex){
      //ignore this error
    }
  }



}
