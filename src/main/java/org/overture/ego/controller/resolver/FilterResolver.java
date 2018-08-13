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

package org.overture.ego.controller.resolver;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.overture.ego.model.search.Filters;
import org.overture.ego.model.search.SearchFilter;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FilterResolver implements HandlerMethodArgumentResolver {

  @NonNull
  private List<String> fieldValues;

  public FilterResolver(@NonNull List<String> fieldValues){
    this.fieldValues = fieldValues;
  }

  @Override
  public boolean supportsParameter(MethodParameter methodParameter) {
    return methodParameter.getParameterAnnotation(Filters.class) != null;
  }

  @Override
  public Object resolveArgument(MethodParameter methodParameter,
                                ModelAndViewContainer modelAndViewContainer,
                                NativeWebRequest nativeWebRequest,
                                WebDataBinderFactory webDataBinderFactory) throws Exception {

    val filters = new ArrayList<SearchFilter>();
    nativeWebRequest.getParameterNames().forEachRemaining(p -> {
      val matchingField = fieldValues.stream().filter(f -> f.equalsIgnoreCase(p)).findFirst();
      if(matchingField.isPresent()){
        filters.add(new SearchFilter(matchingField.get(),nativeWebRequest.getParameter(p)));
      }
    });
    return filters;
  }
}
