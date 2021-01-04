package bio.overture.ego.utils;

import static java.util.Objects.nonNull;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class Strings {
  public static boolean isDefined(String s) {
    return nonNull(s) && !s.isBlank();
  }
}
