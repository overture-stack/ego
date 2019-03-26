package bio.overture.ego.service.association;

import bio.overture.ego.model.search.SearchFilter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;

@Getter
@Builder
@AllArgsConstructor
public class FindRequest {

  @NonNull private final UUID id;
  @NonNull private final List<SearchFilter> filters;
  @NonNull private final Pageable pageable;
  private String query;

  public Optional<String> getQuery() {
    return Optional.ofNullable(query);
  }
}
