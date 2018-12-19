package bio.overture.ego.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collector;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class Collectors {

  public static <T> Collector<T, ImmutableList.Builder<T>, ImmutableList<T>> toImmutableList() {
    return Collector.of(
        ImmutableList.Builder::new,
        ImmutableList.Builder::add,
        (b1, b2) -> b1.addAll(b2.build()),
        ImmutableList.Builder::build);
  }

  public static <T> Collector<T, ImmutableSet.Builder<T>, ImmutableSet<T>> toImmutableSet() {
    return Collector.of(
        ImmutableSet.Builder::new,
        ImmutableSet.Builder::add,
        (b1, b2) -> b1.addAll(b2.build()),
        ImmutableSet.Builder::build);
  }

  public static <T, K, V> Collector<T, ImmutableMap.Builder<K, V>, ImmutableMap<K, V>> toImmutableMap(
      @NonNull Function<? super T, ? extends K> keyMapper, @NonNull Function<? super T, ? extends V> valueMapper) {

    final BiConsumer<ImmutableMap.Builder<K, V>, T> accumulator =
        (builder, entry) -> builder.put(keyMapper.apply(entry), valueMapper.apply(entry));

    return Collector.of(
        ImmutableMap.Builder::new,
        accumulator,
        (b1, b2) -> b1.putAll(b2.build()),
        ImmutableMap.Builder::build);
  }

  public static <T, K> Collector<T, ImmutableMap.Builder<K, T>, ImmutableMap<K, T>> toImmutableMap(
      @NonNull Function<? super T, ? extends K> keyMapper) {
    return toImmutableMap(keyMapper, Function.identity());
  }

}
