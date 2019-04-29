package bio.overture.ego.grpc;

import com.google.protobuf.StringValue;
import lombok.val;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class ProtoUtils {

  public static final StringValue DEFAULT_STRING = StringValue.of("");

  /* toProtoString
   * Use this to convert a potentially null value into a String for a protobuf value
   *
   * Will return Empty String if the value is null, otherwise the toString output for the Object
   */
  public static StringValue toProtoString(Object value) {
    return value == null ? DEFAULT_STRING : StringValue.of(value.toString());
  }

  public static Pageable getPageable(PagedRequest pagedRequest) {
    final int DEFAULT_LIMIT = 20;

    val limit = pagedRequest.getPageSize() == 0 ? DEFAULT_LIMIT : pagedRequest.getPageSize();

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
        // Sort results by creation time, ensure static order for the page_token to refer to
        return new Sort(Sort.Direction.ASC, "createdAt");
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
}
