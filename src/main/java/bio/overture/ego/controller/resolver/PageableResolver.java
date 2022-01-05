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

package bio.overture.ego.controller.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class PageableResolver implements HandlerMethodArgumentResolver {

  public static final String SORT = "sort";
  public static final String SORTORDER = "sortOrder";
  public static final String OFFSET = "offset";
  public static final String LIMIT = "limit";

  @Override
  public boolean supportsParameter(MethodParameter methodParameter) {
    return methodParameter.getParameterType().equals(Pageable.class);
  }

  @Override
  public Object resolveArgument(
      MethodParameter methodParameter,
      ModelAndViewContainer modelAndViewContainer,
      NativeWebRequest nativeWebRequest,
      WebDataBinderFactory webDataBinderFactory)
      throws Exception {
    // get paging parameters
    String limit = nativeWebRequest.getParameter(LIMIT);
    String offset = nativeWebRequest.getParameter(OFFSET);
    String sort = nativeWebRequest.getParameter(SORT);
    String sortOrder = nativeWebRequest.getParameter(SORTORDER);

    return getPageable(limit, offset, sort, sortOrder);
  }

  public Pageable getPageable() {
    return getPageable(null, null, null, null);
  }

  private Pageable getPageable(String limit, String offset, String sort, String sortOrder) {
    return new Pageable() {
      private final int DEFAULT_LIMIT = 20;
      private final int DEFAULT_PAGE_NUM = 0;

      @Override
      public int getPageNumber() {
        return 0;
      }

      @Override
      public int getPageSize() {
        if (StringUtils.isEmpty(limit)) {
          return DEFAULT_LIMIT;
        } else {
          return Integer.parseInt(limit);
        }
      }

      @Override
      public long getOffset() {
        if (StringUtils.isEmpty(offset)) {
          return DEFAULT_PAGE_NUM;
        } else {
          return Integer.parseInt(offset);
        }
      }

      @Override
      public Sort getSort() {
        // set default sort direction
        Sort.Direction direction = Sort.Direction.DESC;

        if ((!StringUtils.isEmpty(sortOrder)) && "asc".equals(sortOrder.toLowerCase())) {
          direction = Sort.Direction.ASC;
        }
        // TODO: this is a hack for now to provide default sort on id field
        // ideally we should not be making assumption about field name as "id" - it will break if
        // field doesn't exist
        return Sort.by(direction, StringUtils.isEmpty(sort) ? "id" : sort);
      }

      @Override
      public Pageable next() {
        return null;
      }

      @Override
      public Pageable previousOrFirst() {
        return null;
      }

      @Override
      public Pageable first() {
        return null;
      }

      @Override
      public Pageable withPage(int pageNumber) {
        throw new RuntimeException("Not implemented");
      }

      @Override
      public boolean hasPrevious() {
        return false;
      }
    };
  }
}
