package bio.overture.ego.utils;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.NonNull;

public class Streams {

  public static <T> Stream<T> stream(@NonNull Iterator<T> iterator) {
    return stream(() -> iterator, false);
  }

  public static <T> Stream<T> stream(@NonNull Iterable<T> iterable) {
    return stream(iterable, false);
  }

  @SafeVarargs
  public static <T> Stream<T> stream(@NonNull T... values) {
    return ImmutableList.copyOf(values).stream();
  }

  /*
   * Helpers
   */
  private static <T> Stream<T> stream(Iterable<T> iterable, boolean inParallel) {
    return StreamSupport.stream(iterable.spliterator(), inParallel);
  }

}
