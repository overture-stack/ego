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

import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.utils.QueryUtils;
import com.google.common.collect.Lists;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.Predicate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static bio.overture.ego.model.entity.AbstractPermission.Fields.accessLevel;
import static bio.overture.ego.model.entity.AbstractPermission.Fields.policy;
import static bio.overture.ego.model.entity.User.Fields.id;
import static bio.overture.ego.model.entity.User.Fields.name;
import static bio.overture.ego.model.entity.UserPermission.Fields.owner;
import static com.google.common.base.Strings.isNullOrEmpty;

@Slf4j
public class UserPermissionSpecification extends SpecificationBase<UserPermission> {

  public static Specification<UserPermission> buildFilterSpecification(
      @NonNull UUID policyId, @NonNull List<SearchFilter> filters) {
    return buildFilterAndQuerySpecification(policyId, filters, null);
  }

  public static Specification<UserPermission> buildFilterAndQuerySpecification(
      @NonNull UUID policyId, @NonNull List<SearchFilter> filters, String text) {
    return (root, query, builder) -> {
      val sp = SimpleCriteriaBuilder.of(root, builder, query);
      sp.setDistinct(true);
      // Create joins
      val policySp = sp.leftJoinFetch(Policy.class, policy);
      val userSp = sp.leftJoinFetch(User.class, owner);

      // Create predicates for filtering by policyId AND searchFilters
      val filterPredicates = userSp.searchFilter(filters);
      val policyIdPredicate = policySp.equalId(policyId);
      val andPredicates= Lists.<Predicate>newArrayList();
      andPredicates.addAll(filterPredicates);
      andPredicates.add(policyIdPredicate);
      val andPredicate = builder.and(andPredicates.toArray(Predicate[]::new));

      if (!isNullOrEmpty(text)) {
        // Create query predicate
        val queryPredicates = Lists.<Predicate>newArrayList();
        val finalText = QueryUtils.prepareForQuery(text);
        // UserPermission.accessLevel
        queryPredicates.add(sp.matchStringField(accessLevel, finalText));

        // User.id and User.name
        Stream.of(id, name)
            .map( fieldName -> userSp.matchStringField(fieldName, finalText))
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
