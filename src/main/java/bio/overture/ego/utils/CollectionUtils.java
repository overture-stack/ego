package bio.overture.ego.utils;

import static bio.overture.ego.utils.Collectors.toImmutableList;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.IntStream.range;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.val;

public class CollectionUtils {

  public static <T, U> Set<U> mapToSet(Collection<T> collection, Function<T, U> mapper) {
    return collection.stream().map(mapper).collect(toSet());
  }

  public static <T, U> List<U> mapToList(Collection<T> collection, Function<T, U> mapper) {
    return collection.stream().map(mapper).collect(toList());
  }

  public static <T> Set<T> findDuplicates(Collection<T> collection) {
    val exitingSet = Sets.<T>newHashSet();
    val duplicateSet = Sets.<T>newHashSet();
    collection.forEach(
        x -> {
          if (exitingSet.contains(x)) {
            duplicateSet.add(x);
          } else {
            exitingSet.add(x);
          }
        });
    return duplicateSet;
  }

  public static Set<String> setOf(String... strings) {
    return stream(strings).collect(toSet());
  }

  public static List<String> listOf(String... strings) {
    return asList(strings);
  }

  public static <T> Set<T> difference(Collection<T> left, Collection<T> right) {
    return Sets.difference(ImmutableSet.copyOf(left), ImmutableSet.copyOf(right));
  }

  public static <T> Set<T> intersection(Collection<T> left, Collection<T> right) {
    return Sets.intersection(ImmutableSet.copyOf(left), ImmutableSet.copyOf(right));
  }

  public static <T> List<T> repeatedCallsOf(@NonNull Supplier<T> callback, int numberOfCalls) {
    return range(0, numberOfCalls).boxed().map(x -> callback.get()).collect(toImmutableList());
  }

  public static <T> Set<T> concatToSet(@NonNull Collection<T>... collections) {
    return stream(collections).flatMap(Collection::stream).collect(toImmutableSet());
  }

  public static <T> List<T> concatToList(@NonNull Collection<T>... collections) {
    return stream(collections).flatMap(Collection::stream).collect(toImmutableList());
  }
}
