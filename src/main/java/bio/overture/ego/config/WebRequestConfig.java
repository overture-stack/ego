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

package bio.overture.ego.config;

import bio.overture.ego.model.enums.Fields;
import bio.overture.ego.utils.FieldUtils;
import bio.overture.ego.controller.resolver.FilterResolver;
import bio.overture.ego.controller.resolver.PageableResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

@Configuration
public class WebRequestConfig extends WebMvcConfigurerAdapter {

  @Bean
  public List<String> fieldValues() {
    return FieldUtils.getStaticFieldValueList(Fields.class);
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
    argumentResolvers.add(new PageableResolver());
    argumentResolvers.add(new FilterResolver(fieldValues()));
  }
}
