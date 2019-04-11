package bio.overture.ego.utils;

import lombok.SneakyThrows;

public class Defaults<T> {
  T val;

  @SneakyThrows
  Defaults(T value) {
    val = value;
  }

  static <X> Defaults<X> create(X value) {
    return new Defaults<>(value);
  }

  T def(T value) {
    return value == null ? val : value;
  }
}
