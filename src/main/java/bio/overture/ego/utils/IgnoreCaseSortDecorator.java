package bio.overture.ego.utils;

import static java.util.stream.Collectors.toUnmodifiableList;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

@RequiredArgsConstructor
public class IgnoreCaseSortDecorator implements Pageable {

  @NonNull private final Pageable delegate;

  /** Decorated methods */
  @Override
  public Sort getSort() {
    val orders = delegate.getSort().stream().map(Order::ignoreCase).collect(toUnmodifiableList());
    return Sort.by(orders);
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
