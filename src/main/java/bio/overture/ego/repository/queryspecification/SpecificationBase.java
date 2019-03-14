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

package bio.overture.ego.repository.queryspecification;

import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.utils.QueryUtils;
import lombok.NonNull;
import lombok.val;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.Arrays;
import java.util.List;

public class SpecificationBase<T> {
  protected static <T> Predicate[] getQueryPredicates(
      @NonNull CriteriaBuilder builder,
      @NonNull Root<T> root,
      String queryText,
      @NonNull String... params) {
    return Arrays.stream(params)
        .map(p -> filterByField(builder, root, p, queryText))
        .toArray(Predicate[]::new);
  }

  public static <T> Predicate filterByField(
      @NonNull CriteriaBuilder builder,
      @NonNull Root<T> root,
      @NonNull String fieldName,
      String fieldValue) {
    val finalText = QueryUtils.prepareForQuery(fieldValue);

    // Cast "as" String so that we can search ENUM types
    return builder.like(builder.lower(root.get(fieldName).as(String.class)), finalText);
  }

  public static <T> Specification<T> filterBy(@NonNull List<SearchFilter> filters) {
    return (root, query, builder) -> {
      query.distinct(true);
      return builder.and(
          filters
              .stream()
              .map(f -> filterByField(builder, root, f.getFilterField(), f.getFilterValue()))
              .toArray(Predicate[]::new));
    };
  }
}
