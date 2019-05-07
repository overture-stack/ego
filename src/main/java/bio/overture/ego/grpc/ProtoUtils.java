package bio.overture.ego.grpc;

import static java.util.stream.Collectors.toList;

import com.google.protobuf.StringValue;
import com.google.protobuf.UInt32Value;
import java.util.Arrays;
import java.util.Optional;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class ProtoUtils {

  public static final StringValue DEFAULT_STRING = StringValue.of("");

  private static final String DEFAULT_SORT_FIELD = "createdAt";
  private static final int DEFAULT_LIMIT = 100;
  private static final int MAX_LIMIT = 1000;

  /* toProtoString
   * Use this to convert a potentially null value into a String for a protobuf value
   *
   * Will return Empty String if the value is null, otherwise the toString output for the Object
   */
  public static StringValue toProtoString(Object value) {
    return value == null ? DEFAULT_STRING : StringValue.of(value.toString());
  }

  public static PagedResponse createPagedResponse(Page page, int currentPageNum) {
    val pageBuilder =
        PagedResponse.newBuilder().setMaxResults(Long.valueOf(page.getTotalElements()).intValue());

    if (page.hasNext()) {
      val nextPage = UInt32Value.of(currentPageNum + 1);
      pageBuilder.setNextPage(nextPage);
    }

    return pageBuilder.build();
  }


  public static Pageable getPageable(PagedRequest pagedRequest, String sort) {

    val pageSize = pagedRequest.getPageSize();

    val limit = pageSize == 0 ? DEFAULT_LIMIT : pageSize > MAX_LIMIT ? MAX_LIMIT : pageSize;

    val pageNumber = pagedRequest.getPageNumber();

    return new Pageable() {

      @Override
      public int getPageNumber() {
        return 0;
      }

      @Override
      public int getPageSize() {
        return limit;
      }

      @Override
      public long getOffset() {
        return pageNumber * limit;
      }

      @Override
      public Sort getSort() {
        return parseSort(sort);
      }

      @Override
      public Pageable next() {
        return null;
      }

      @Override
      public Pageable previousOrFirst() {
        return null;
      }

      @Override
      public Pageable first() {
        return null;
      }

      @Override
      public boolean hasPrevious() {
        return false;
      }
    };
  }

  public static Sort parseSort(String sort) {
    if (sort.isEmpty()) {
      // Sort results by creation time, ensure static order for the page_token to refer to
      return new Sort(Sort.Direction.ASC, "createdAt");
    } else {
      val orders =
          Arrays.stream(sort.split(","))
              .map(ProtoUtils::parseSortOrder)
              .filter(optional -> optional.isPresent())
              .map(optional -> optional.get())
              .collect(toList());
      return Sort.by(orders);
    }
  }

  private static Optional<Sort.Order> parseSortOrder(String sort) {

    if (!sort.isEmpty()) {
      val split = sort.trim().split(" ");
      switch (split.length) {
        case 1:
          // Example: "id"
          if (sort.equalsIgnoreCase(Sort.Direction.DESC.name())) {
            // Special case, sort value is exactly "desc"
            return Optional.of(new Sort.Order(Sort.Direction.DESC, DEFAULT_SORT_FIELD));

          } else if (sort.equalsIgnoreCase(Sort.Direction.ASC.name())) {
            // Special case, sort value is exactly "asc"
            return Optional.of(new Sort.Order(Sort.Direction.ASC, DEFAULT_SORT_FIELD));

          } else {
            return Optional.of(new Sort.Order(Sort.Direction.ASC, split[0]));
          }

        case 2:
          // Example: "name desc"
          if (split[1].equalsIgnoreCase(Sort.Direction.DESC.name())) {
            return Optional.of(new Sort.Order(Sort.Direction.DESC, split[0]));

          } else if (split[1].equalsIgnoreCase(Sort.Direction.ASC.name())) {
            return Optional.of(new Sort.Order(Sort.Direction.ASC, split[0]));
          }
          break;

        default:
          // sort string length was 0 or longer than 2
          return Optional.empty();
      }
    }
    // Fall through - nothing matching expected formatting
    return Optional.empty();
  }
}
