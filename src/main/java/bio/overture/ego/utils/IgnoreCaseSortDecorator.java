package bio.overture.ego.utils;

import static java.util.stream.Collectors.toUnmodifiableList;
import static org.springframework.util.StringUtils.isEmpty;

import java.util.Map;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@RequiredArgsConstructor
public class IgnoreCaseSortDecorator implements Pageable {

  private static final Map<String, String> SORT_MAP =
      Map.of("name", "owner.name", "id", "owner.id", "mask", "accessLevel");

  @NonNull private final Pageable delegate;

  /** Decorated methods */
  @Override
  public Sort getSort() {
    val orders =
        delegate.getSort().stream()
            .map(o -> new Sort.Order(o.getDirection(), getSortField(o.getProperty())).ignoreCase())
            .collect(toUnmodifiableList());

    return Sort.by(orders);
  }

  private String getSortField(String sort) {
    val mapValue = SORT_MAP.get(sort);
    return isEmpty(mapValue) ? sort : mapValue;
  }

  /** Delegated methods */
  @Override
  public int getPageNumber() {
    return delegate.getPageNumber();
  }

  @Override
  public int getPageSize() {
    return delegate.getPageSize();
  }

  @Override
  public long getOffset() {
    return delegate.getOffset();
  }

  @Override
  public Pageable next() {
    return delegate.next();
  }

  @Override
  public Pageable previousOrFirst() {
    return delegate.previousOrFirst();
  }

  @Override
  public Pageable first() {
    return delegate.first();
  }

  @Override
  public boolean hasPrevious() {
    return delegate.hasPrevious();
  }
}
