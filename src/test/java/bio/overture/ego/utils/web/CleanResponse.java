package bio.overture.ego.utils.web;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class CleanResponse {
  @NonNull private final String statusCodeName;
  private final int statusCodeValue;
  private final Object body;
}
