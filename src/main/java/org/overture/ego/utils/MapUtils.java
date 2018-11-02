package org.overture.ego.utils;

import org.overture.ego.model.params.ScopeName;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MapUtils {
  public static <T,U> Set<U> mapToSet(Collection<T> collection, Function<T,U> mapper) {
    return collection.stream().map(mapper).collect(Collectors.toSet());
  }
  public static <T,U> List<U> mapToList(Collection<T> collection, Function<T,U> mapper) {
    return collection.stream().map(mapper).collect(Collectors.toList());
  }
  public static Set<String> setOf(String... strings) {
    return new HashSet<>(Arrays.asList(strings));
  }

  public static List<String> listOf(String... strings) { return Arrays.asList(strings);}


}
