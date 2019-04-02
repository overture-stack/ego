package bio.overture.ego.utils.web;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import static bio.overture.ego.utils.Joiners.COMMA;
import static java.lang.String.format;

@Value
@Builder
public class QueryParam {
  @NonNull private final String key;
  @NonNull private final Object value;

  public static QueryParam createQueryParam(String key, Object... values) {
    return new QueryParam(key, COMMA.join(values));
  }

  @Override
  public String toString() {
    return format("%s=%s", key, value);
  }
}
