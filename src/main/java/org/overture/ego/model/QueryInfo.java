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

package org.overture.ego.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.function.Function;

@Data
@JsonPropertyOrder({"limit", "offset", "sort", "sortOrder"})
@JsonInclude(JsonInclude.Include.ALWAYS)
public class QueryInfo {
   int limit = 20; //default limit
   int offset;
   String sort = "";
   String sortOrder = "DESC"; // default is latest on top


   /*
      Un-sanitized get will return empty string
    */
   public String getSort(){
      return this.getSort(s -> "");
   }

   /*
    Using string templates with JDBI opens up the room for SQL Injection
    Field sanitation is must to avoid it
    */
   public String getSort(Function<String, String> sanitizer){
      this.sort = sanitizer.apply(this.sort);
      return this.sort;
   }

   public String getSortOrder(){
      // set default sort order if invalid sort order specified
      if(isSortOrderValid(this.sortOrder) == false){
         this.setSortOrder("DESC");
      }
      return this.sortOrder;
   }

   private boolean isSortOrderValid(String sortOrder){
      return ("asc".equals(sortOrder.toLowerCase()) || "desc".equals(sortOrder.toLowerCase()));
   }

}
