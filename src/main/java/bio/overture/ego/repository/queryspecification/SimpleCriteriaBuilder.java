package bio.overture.ego.repository.queryspecification;

import bio.overture.ego.model.search.SearchFilter;
import bio.overture.ego.utils.QueryUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Fetch;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static bio.overture.ego.model.enums.JavaFields.ID;
import static java.util.stream.Collectors.toUnmodifiableList;
import static javax.persistence.criteria.JoinType.LEFT;
import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
public class SimpleCriteriaBuilder<X,Y>{
  @NonNull private final From<X,Y> from;
  @NonNull private final CriteriaBuilder builder;
  @NonNull private final CriteriaQuery<?> query;
  public static <X,Y> SimpleCriteriaBuilder<X,Y> of(From<X,Y> from, CriteriaBuilder builder, CriteriaQuery<?> query){
    return new SimpleCriteriaBuilder<>(from, builder, query);
  }

  // [rtisma] Vlad said to do this:
  // https://discourse.hibernate.org/t/how-can-i-do-a-join-fetch-in-criteria-api/846/6
  // Calling from.fetch followed by a from.join results in redundant join sql statements.
  @SuppressWarnings("unchecked")
  public <T> SimpleCriteriaBuilder<Y, T> leftJoinFetch(
      Class<T> joinClass, String joinField) {
    Fetch<Y, T> fetch = from.fetch(joinField, LEFT);
    val join = (Join<Y, T>) fetch;
    return new SimpleCriteriaBuilder<>(join, builder, query);
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

  public <T> Predicate matchField(@NonNull String fieldName, @NonNull Class<T> valueType, @NonNull T fieldValue ) {
    return builder.equal(from.<T>get(fieldName).as(valueType), fieldValue);
  }

  public Predicate matchStringField(@NonNull String fieldName,@NonNull String fieldValue ) {
    return builder.equal(from.<String>get(fieldName).as(String.class), fieldValue);
  }

  public Predicate equalId(@NonNull UUID id) {
    return builder.equal(from.<Integer>get(ID), id);
  }

}
