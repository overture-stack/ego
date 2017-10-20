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

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.val;
import org.overture.ego.model.entity.User;


import java.util.List;

@Data
@Builder
public class Page<E> {
  int limit;
  int offset;
  int count;
  @Singular("resultSet")  List<E> resultSet;

  public static <E> Page<E> getPageFromPageInfo(PageInfo pageInfo, List<E> results){

    val pageBuilder = (PageBuilder<E>) Page.builder().limit(pageInfo.getLimit())
            .offset(pageInfo.getOffset())
            .resultSet(results);
    return pageBuilder.build();

  }

}
