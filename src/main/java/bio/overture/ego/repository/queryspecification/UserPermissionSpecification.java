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

import static bio.overture.ego.model.enums.JavaFields.*;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toUnmodifiableList;
import static javax.persistence.criteria.JoinType.LEFT;
import static lombok.AccessLevel.PRIVATE;

import bio.overture.ego.model.entity.*;
import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.utils.QueryUtils;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.springframework.data.jpa.domain.Specification;

@Slf4j
public class UserPermissionSpecification extends SpecificationBase<UserPermission> {

  @SuppressWarnings("unchecked")
  public static Specification<UserPermission> withPolicy(@NonNull UUID policyId) {
    return (root, query, builder) -> {
      query.distinct(true);

      // [rtisma] Vlad said to do this:
      // https://discourse.hibernate.org/t/how-can-i-do-a-join-fetch-in-criteria-api/846/6
      // Calling fetch followed by a join results in redundant join sql statements.
      Fetch<UserPermission, Policy> userPermissionPolicyFetch = root.fetch(POLICY, LEFT);
      Join<UserPermission, Policy> userPermissionPolicyJoin =
          (Join<UserPermission, Policy>) userPermissionPolicyFetch;
      return builder.equal(userPermissionPolicyJoin.<Integer>get(ID), policyId);
    };
  }

  @SuppressWarnings("unchecked")
  public static Specification<UserPermission> containsText(@NonNull String text) {
    val finalText = QueryUtils.prepareForQuery(text);

    // [rtisma] Vlad said to do this:
    // https://discourse.hibernate.org/t/how-can-i-do-a-join-fetch-in-criteria-api/846/6
    return (root, query, builder) -> {
      //      Fetch<UserPermission, User> userPermissionFetch = root.fetch(OWNER, LEFT);
      //      Join<UserPermission, User> userPermissionJoin = (Join<UserPermission, User>)
      // userPermissionFetch;
      Join<UserPermission, User> userPermissionJoin = root.join(OWNER, LEFT);

      query.distinct(true);
      val predicates =
          Stream.of(
                  getQueryPredicates(builder, root, finalText, ACCESS_LEVEL),
                  getQueryPredicatesForJoin(builder, userPermissionJoin, finalText, ID, NAME))
              .flatMap(Arrays::stream)
              .toArray(Predicate[]::new);
      return builder.or(predicates);
    };
  }

  @SuppressWarnings("unchecked")
  public static Specification<UserPermission> filterByUser(@NonNull List<SearchFilter> filters) {
    return (root, query, builder) -> {
      query.distinct(true);
      Fetch<UserPermission, User> userFetch = root.fetch(OWNER, LEFT);
      Join<UserPermission, User> userJoin = (Join<UserPermission, User>) userFetch;
      return builder.and(
          filters.stream()
              .map(
                  f ->
                      filterByFieldForJoin(
                          builder, userJoin, f.getFilterField(), f.getFilterValue()))
              .toArray(Predicate[]::new));
    };
  }

  public static Specification<UserPermission> buildFilterSpecification(
      @NonNull UUID policyId, @NonNull List<SearchFilter> filters) {
    return buildQueryAndFilterSpecification(policyId, filters, null);
  }

  public static Predicate filterByField(
      @NonNull From from,
      @NonNull CriteriaBuilder builder,
      @NonNull String fieldName,
      String fieldValue) {
    val finalText = QueryUtils.prepareForQuery(fieldValue);

    // Cast "as" String so that we can search ENUM types
    return builder.like(builder.lower(from.get(fieldName).as(String.class)), finalText);
  }

  public static Collection<Predicate> buildFilterPredicates(
      From from, CriteriaBuilder builder, List<SearchFilter> filters) {
    return filters.stream()
        .map(f -> filterByField(from, builder, f.getFilterField(), f.getFilterValue()))
        .collect(toUnmodifiableList());
  }

  public static Predicate buildIdEqualPredicate(From from, CriteriaBuilder builder, UUID id) {
    return builder.equal(from.<Integer>get(ID), id);
  }

  public static Stream<Predicate> streamQueryPredicates(
      From from, CriteriaBuilder builder, String text, String... fieldNames) {
    val finalText = QueryUtils.prepareForQuery(text);
    return Arrays.stream(fieldNames)
        .map(
            fieldName ->
                builder.like(builder.lower(from.get(fieldName).as(String.class)), finalText));
  }

  public static Specification<UserPermission> buildQueryAndFilterSpecification_BETTER(
      @NonNull UUID policyId, @NonNull List<SearchFilter> filters) {
    return buildQueryAndFilterSpecification_BETTER(policyId, filters, null);
  }

  @RequiredArgsConstructor(access = PRIVATE)
  public static class Sp<X,Y>{
    @NonNull private final From<X,Y> from;
    @NonNull private final CriteriaBuilder builder;
    @NonNull private final CriteriaQuery<?> query;
    public static <X,Y> Sp<X,Y> of(From<X,Y> from, CriteriaBuilder builder, CriteriaQuery<?> query){
      return new Sp<>(from, builder, query);
    }

    @SuppressWarnings("unchecked")
    private <T> Sp<Y, T> leftJoinFetch(
        Class<T> joinClass, String joinField) {
      Fetch<Y, T> fetch = from.fetch(joinField, LEFT);
      val join = (Join<Y, T>) fetch;
      return new Sp<>(join, builder, query);
    }

    public void setDistinct(boolean distinct){
      query.distinct(distinct);
    }

    public Predicate and(Predicate ... andPredicates){
      return builder.and(andPredicates);
    }

    public Predicate or(Predicate ... orPredicates){
      return builder.or(orPredicates);
    }

    public Collection<Predicate> searchFilter(@NonNull List<SearchFilter> filters) {
      return filters.stream()
          .map(f -> filterByField(f.getFilterField(), f.getFilterValue()))
          .collect(toUnmodifiableList());
    }

    public Predicate filterByField( @NonNull String fieldName, String fieldValue) {
      val finalText = QueryUtils.prepareForQuery(fieldValue);

      // Cast "as" String so that we can search ENUM types
      return builder.like(builder.lower(from.get(fieldName).as(String.class)), finalText);
    }

    public <T> Predicate matchField(@NonNull String fieldName, @NonNull T fieldValue ) {
      return builder.equal(from.<T>get(fieldName), fieldValue);
    }

    public Predicate equalId(@NonNull UUID id) {
      return builder.equal(from.<Integer>get(ID), id);
    }

  }

  public static class SpecificationUtils{

    @SuppressWarnings("unchecked")
    private static <X, Y> Join<X, Y> leftJoinFetch(
        From<X, X> root, Class<Y> joinClass, String joinField) {
      Fetch<X, Y> fetch = root.fetch(joinField, LEFT);
      return (Join<X, Y>) fetch;
    }

    public static <X,Y> Collection<Predicate> buildFilterPredicates(
        From<X,Y> from, CriteriaBuilder builder, List<SearchFilter> filters) {
      return filters.stream()
          .map(f -> filterByField(from, builder, f.getFilterField(), f.getFilterValue()))
          .collect(toUnmodifiableList());
    }

    public static <X,Y> Predicate filterByField(
        @NonNull From<X,Y> from,
        @NonNull CriteriaBuilder builder,
        @NonNull String fieldName,
        String fieldValue) {
      val finalText = QueryUtils.prepareForQuery(fieldValue);

      // Cast "as" String so that we can search ENUM types
      return builder.like(builder.lower(from.get(fieldName).as(String.class)), finalText);
    }

    public static <X,Y, T> Predicate matchField(From<X,Y> from, CriteriaBuilder builder, String fieldName, T fieldValue ) {
      return builder.equal(from.<T>get(fieldName), fieldValue);
    }

  }

  public static Specification<UserPermission> buildQueryAndFilterSpecification_BETTER(
      @NonNull UUID policyId, @NonNull List<SearchFilter> filters, String text) {
    return (root, query, builder) -> {
      query.distinct(true);
      val policyJoin = leftJoinFetch(root, Policy.class, POLICY);
      val userJoin = leftJoinFetch(root, User.class, OWNER);
      val filterPredicates = Lists.<Predicate>newArrayList();
      filterPredicates.addAll(buildFilterPredicates(userJoin, builder, filters));
      filterPredicates.add(buildIdEqualPredicate(policyJoin, builder, policyId));
      val andPredicate = builder.and(filterPredicates.toArray(Predicate[]::new));
      val queryPredicates = Lists.<Predicate>newArrayList();
      if (!isNullOrEmpty(text)) {
        val finalText = QueryUtils.prepareForQuery(text);
        queryPredicates.add(
            builder.like(builder.lower(root.get(ACCESS_LEVEL).as(String.class)), finalText));
        Stream.of(ID, NAME)
            .map(
                fieldName ->
                    builder.like(
                        builder.lower(userJoin.get(fieldName).as(String.class)), finalText))
            .forEach(queryPredicates::add);
        val orPredicate = builder.or(queryPredicates.toArray(Predicate[]::new));
        return builder.and(andPredicate, orPredicate);
      }
      return builder.and(andPredicate);
    };
  }

  @SuppressWarnings("unchecked")
  private static <X, Y> Join<X, Y> leftJoinFetch(
      From<X, X> root, Class<Y> joinClass, String joinField) {
    Fetch<X, Y> fetch = root.fetch(joinField, LEFT);
    return (Join<X, Y>) fetch;
  }

  public static Specification<UserPermission> buildQueryAndFilterSpecification(
      @NonNull UUID policyId, @NonNull List<SearchFilter> filters, String text) {
    return (root, query, builder) -> {
      query.distinct(true);
      // [rtisma] Vlad said to do this:
      // https://discourse.hibernate.org/t/how-can-i-do-a-join-fetch-in-criteria-api/846/6
      // Calling fetch followed by a join results in redundant join sql statements.
      Fetch<UserPermission, Policy> userPermissionPolicyFetch = root.fetch(POLICY, LEFT);
      Join<UserPermission, Policy> userPermissionPolicyJoin =
          (Join<UserPermission, Policy>) userPermissionPolicyFetch;
      val andPredicates = Lists.<Predicate>newArrayList();
      andPredicates.add(builder.equal(userPermissionPolicyJoin.<Integer>get(ID), policyId));

      Fetch<UserPermission, User> userFetch = root.fetch(OWNER, LEFT);
      Join<UserPermission, User> userJoin = (Join<UserPermission, User>) userFetch;
      filters.stream()
          .map(f -> filterByFieldForJoin(builder, userJoin, f.getFilterField(), f.getFilterValue()))
          .forEach(andPredicates::add);
      val orQueryPredicates = Lists.<Predicate>newArrayList();
      if (!isNullOrEmpty(text)) {
        val finalText = QueryUtils.prepareForQuery(text);
        orQueryPredicates.add(
            builder.like(builder.lower(root.get(ACCESS_LEVEL).as(String.class)), finalText));
        Stream.of(ID, NAME)
            .map(
                fieldName ->
                    builder.like(
                        builder.lower(userJoin.get(fieldName).as(String.class)), finalText))
            .forEach(orQueryPredicates::add);
      }
      val queryPredicate = builder.or(orQueryPredicates.toArray(Predicate[]::new));
      val filterPredicate = builder.and(andPredicates.toArray(Predicate[]::new));
      return builder.and(filterPredicate, queryPredicate);
    };
  }

  public static void sdf(){
    val spUP = Sp.of((From<UserPermission,UserPermission>)null, null, null);
    val spUser = spUP.leftJoinFetch(User.class, OWNER);
    val spPolicy = spUP.leftJoinFetch(Policy.class, POLICY);

    spPolicy.



  }

}
