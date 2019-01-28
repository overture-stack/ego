package bio.overture.ego.utils;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class CollectionUtils {

  public static <T, U> Set<U> mapToSet(Collection<T> collection, Function<T, U> mapper) {
    return collection.stream().map(mapper).collect(toSet());
  }

  public static <T, U> List<U> mapToList(Collection<T> collection, Function<T, U> mapper) {
    return collection.stream().map(mapper).collect(toList());
  }

  public static Set<String> setOf(String... strings) {
    return stream(strings).collect(toSet());
  }

  public static List<String> listOf(String... strings) {
    return asList(strings);
  }
}
