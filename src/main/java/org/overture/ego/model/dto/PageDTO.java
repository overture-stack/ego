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

package org.overture.ego.model.dto;


import com.fasterxml.jackson.annotation.JsonView;
import lombok.Getter;
import lombok.NonNull;
import org.overture.ego.view.Views;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@JsonView(Views.REST.class)
public class PageDTO<T> {

  private final int limit;
  private final int offset;
  private final long count;
  private final List<T> resultSet;

  public PageDTO(@NonNull final Page<T> page) {
    this.limit      = page.getSize();
    this.offset     = page.getNumber();
    this.count      = page.getTotalElements();
    this.resultSet  = page.getContent();
  }

}
