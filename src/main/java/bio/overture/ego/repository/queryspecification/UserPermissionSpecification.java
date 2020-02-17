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

import static bio.overture.ego.model.entity.AbstractPermission.Fields.accessLevel;
import static bio.overture.ego.model.entity.AbstractPermission.Fields.policy;
import static bio.overture.ego.model.enums.JavaFields.*;
import static com.google.common.base.Strings.isNullOrEmpty;

import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.utils.QueryUtils;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import javax.persistence.criteria.Predicate;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.parameters.P;

@Slf4j
public class UserPermissionSpecification {
  public static Specification<UserPermission> buildFilterSpecification(
      @NonNull UUID policyId, @NonNull List<SearchFilter> filters) {
    return buildFilterAndQuerySpecification(policyId, filters, null);
  }

  public static Specification<UserPermission> buildFilterAndQuerySpecification(
      @NonNull UUID policyId, @NonNull List<SearchFilter> filters, String text) {
    return (root, query, builder) -> {
      val scb = SimpleCriteriaBuilder.of(root, builder, query);
      // TODO: [anncatton] this might still be necessary
      //      scb.setDistinct(true);

      // Create joins
      // TODO: EGO-452 This code results in redundant selects because it is not being fetched but
      // instead only joined.
      //  When using the joinFetch methodology, hibernate was throwing cryptic errors, so this is a
      // sub performant fix until someone figures out the issue.
      // if no query text is present, Spring creates an extra query for count(userperm) which will
      // throw an exception on Fetch
      // https://coderanch.com/t/656073/frameworks/Spring-Data-fetch-join-Specification
      if (query.getResultType() == Long.class || query.getResultType() == long.class) {
        val join = scb.leftJoin(Policy.class, policy);
        return join.equalId(policyId);
      }
      val policySp = scb.leftJoinFetch(Policy.class, policy);
      val permissionSp = scb.leftJoinFetch(User.class, OWNER);

      // Create predicates for filtering by policyId AND searchFilters
      val filterPredicates = permissionSp.searchFilter(filters);
      val policyIdPredicate = policySp.equalId(policyId);
      val andPredicates = Lists.<Predicate>newArrayList();
      andPredicates.addAll(filterPredicates);
      andPredicates.add(policyIdPredicate);
      val andPredicate = builder.and(andPredicates.toArray(Predicate[]::new));

      if (!isNullOrEmpty(text)) {
        // Create query predicate
        val queryPredicates = Lists.<Predicate>newArrayList();
        val finalText = QueryUtils.prepareForQuery(text);
        // Permission.accessLevel
        queryPredicates.add(scb.filterByField(accessLevel, finalText));

        // Owner ID and NAME
        Stream.of(ID, NAME)
            .map(fieldName -> permissionSp.filterByField(fieldName, finalText))
            .forEach(queryPredicates::add);
        // Query predicates should be ORed together
        val orPredicate = builder.or(queryPredicates.toArray(Predicate[]::new));

        // Query and Filter predicate should be ANDed
        return builder.and(andPredicate, orPredicate);
      }
      return andPredicate;
    };
  }
}
