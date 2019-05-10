package bio.overture.ego.grpc;

import static bio.overture.ego.grpc.ProtoUtils.*;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import bio.overture.ego.model.enums.JavaFields;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
public class ProtoUtilsTest {

  /** Proto String conversion convenience method */
  @Test
  public void toProtoStringNullValue() {
    val result = toProtoString(null);
    assertThat(result).isNotNull();
    assertThat(result.getValue()).isEqualTo(StringUtils.EMPTY);
  }

  @Test
  public void toProtoStringWithValue() {
    // using Sort for test object since we already have this in the test class for use in other
    // tests
    val testObject = Sort.by(new Sort.Order(Sort.Direction.ASC, "createdAt"));
    val result = toProtoString(testObject);
    assertThat(result).isNotNull();
    assertThat(result.getValue()).isEqualTo(testObject.toString());
  }

  /** Create Paged Response from Page */
  @Test
  public void createPagedResponseForEmptyPage() {
    val result = createPagedResponse(Page.empty(), 0);
    assertThat(result.hasNextPage()).isFalse();
    assertThat(result.getMaxResults()).isEqualTo(0);
  }

  @Test
  public void createPagedResponseForCompleteSet() {
    val dataList = Arrays.asList("1", "2", "3");
    val page = new PageImpl<String>(dataList);
    val result = createPagedResponse(page, 0);
    assertThat(result.hasNextPage()).isFalse();
    assertThat(result.getMaxResults()).isEqualTo(dataList.size());
  }

  @Test
  public void createPagedResponseForPartialSet() {
    val dataList = Arrays.asList("1", "2", "3");
    val pageable =
        getPageable(
            PagedRequest.newBuilder()
                .setPageNumber(0)
                .setPageSize(2)
                .setOrderBy(StringUtils.EMPTY)
                .build());

    val pageData = dataList.stream().limit(2).collect(toList());
    val page =
        new PageImpl<String>(pageData, pageable, Integer.valueOf(dataList.size()).longValue());

    val result = createPagedResponse(page, 0);

    assertThat(result.hasNextPage()).isTrue();
    assertThat(result.getNextPage().getValue()).isEqualTo(1);
    assertThat(result.getMaxResults()).isEqualTo(dataList.size());
  }

  @Test
  public void createPagedResponseForPartialSetWithDifferentPageNumber() {
    val dataList = Arrays.asList("1", "2", "3", "4", "5");
    val pageable =
        getPageable(
            PagedRequest.newBuilder()
                .setPageNumber(1)
                .setPageSize(2)
                .setOrderBy(StringUtils.EMPTY)
                .build());

    val pageData = dataList.stream().limit(2).collect(toList());
    val page =
        new PageImpl<String>(pageData, pageable, Integer.valueOf(dataList.size()).longValue());

    val result = createPagedResponse(page, 1);

    assertThat(result.hasNextPage()).isTrue();
    assertThat(result.getNextPage().getValue()).isEqualTo(2);
  }

  /** Pageable Resolution */
  @Test
  public void getPageableForEmptyInput() {
    val input =
        PagedRequest.newBuilder()
            .setPageNumber(0)
            .setPageSize(0)
            .setOrderBy(StringUtils.EMPTY)
            .build();
    val result = getPageable(input);

    assertThat(result.getSort())
        .isEqualTo(Sort.by(new Sort.Order(Sort.Direction.ASC, JavaFields.CREATEDAT)));
    assertThat(result.getOffset()).isEqualTo(0);
    assertThat(result.getPageSize())
        .isEqualTo(100); // default page size value (set in ProtoUtils.getPageable)
  }

  @Test
  public void getPageableForSpecificInput() {
    int page = 10;
    int size = 30;

    val input =
        PagedRequest.newBuilder()
            .setPageNumber(page)
            .setPageSize(size)
            .setOrderBy("id desc, lastLogin, name asc")
            .build();
    val result = getPageable(input);

    val expectedSort =
        Sort.by(
            new Sort.Order(Sort.Direction.DESC, "id"),
            new Sort.Order(Sort.Direction.ASC, "lastLogin"),
            new Sort.Order(Sort.Direction.ASC, "name"));

    assertThat(result.getSort()).isEqualTo(expectedSort);
    assertThat(result.getOffset()).isEqualTo(page * size);
    assertThat(result.getPageSize()).isEqualTo(30);
  }

  @Test
  public void getPageableWithSizeOverLimit() {
    int size = 9001;

    val input =
        PagedRequest.newBuilder()
            .setPageNumber(0)
            .setPageSize(size)
            .setOrderBy(StringUtils.EMPTY)
            .build();
    val result = getPageable(input);

    assertThat(result.getPageSize())
        .isEqualTo(1000); // default max page size value (set in ProtoUtils.getPageable)
  }

  /** Parse Sort Tests */
  @Test
  public void parseSortWithEmptyInput() {
    val sort = StringUtils.EMPTY;
    val result = parseSort(sort);

    val expected = Sort.by(new Sort.Order(Sort.Direction.ASC, "createdAt"));
    assertThat(result).isEqualTo(expected);
  }

  @Test
  public void parseSortWithDirectionOnly() {
    // Test asc
    val sortUp = "asc";
    val sortUpResult = parseSort(sortUp);

    val sortUpExpected = Sort.by(new Sort.Order(Sort.Direction.ASC, "createdAt"));
    assertThat(sortUpResult).isEqualTo(sortUpExpected);

    // Test desc
    val sortDown = "desc";
    val sortDownResult = parseSort(sortDown);

    val sortDownExpected = Sort.by(new Sort.Order(Sort.Direction.DESC, "createdAt"));
    assertThat(sortDownResult).isEqualTo(sortDownExpected);
  }

  @Test
  public void parseSortWithMultipleInputs() {
    val expected =
        Sort.by(
            new Sort.Order(Sort.Direction.DESC, "id"),
            new Sort.Order(Sort.Direction.ASC, "lastLogin"),
            new Sort.Order(Sort.Direction.ASC, "name"));

    // comma separated list with all direction indicators (asc, desc, no direction indicated)
    val sort = "id desc, lastLogin, name asc";
    val result = parseSort(sort);
    assertThat(result).isEqualTo(expected);

    // double check spacing variation, trailing commas, empty clauses
    val sortCompact = "id desc,lastLogin,,name asc,";
    val resultCompact = parseSort(sortCompact);
    assertThat(resultCompact).isEqualTo(expected);
  }
}
