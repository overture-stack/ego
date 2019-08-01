package bio.overture.ego.grpc;

import static bio.overture.ego.grpc.ProtoUtils.*;
import static java.util.stream.Collectors.toList;

import bio.overture.ego.model.enums.JavaFields;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

@Slf4j
public class ProtoUtilsTest {

  /** Proto String conversion convenience method */
  @Test
  public void toProtoStringNullValue() {
    val result = toProtoString(null);
    Assert.assertNotNull(result);
    Assert.assertEquals(result.getValue(), StringUtils.EMPTY);
  }

  @Test
  public void toProtoStringWithValue() {
    // using Sort for test object since we already have this in the test class for use in other
    // tests
    val testObject = Sort.by(new Sort.Order(Sort.Direction.ASC, "createdAt"));
    val result = toProtoString(testObject);
    Assert.assertNotNull(result);
    Assert.assertEquals(result.getValue(), testObject.toString());
  }

  /** Create Paged Response from Page */
  @Test
  public void createPagedResponseForEmptyPage() {
    val result = createPagedResponse(Page.empty(), 0);
    Assert.assertFalse(result.hasNextPage());
    Assert.assertEquals(result.getMaxResults(), 0);
  }

  @Test
  public void createPagedResponseForCompleteSet() {
    val dataList = Arrays.asList("1", "2", "3");
    val page = new PageImpl<String>(dataList);
    val result = createPagedResponse(page, 0);
    Assert.assertFalse(result.hasNextPage());
    Assert.assertEquals(result.getMaxResults(), dataList.size());
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

    Assert.assertTrue(result.hasNextPage());
    Assert.assertEquals(result.getNextPage().getValue(), 1);
    Assert.assertEquals(result.getMaxResults(), dataList.size());
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

    Assert.assertTrue(result.hasNextPage());
    Assert.assertEquals(result.getNextPage().getValue(), 2);
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

    Assert.assertEquals(
        result.getSort(), Sort.by(new Sort.Order(Sort.Direction.ASC, JavaFields.CREATEDAT)));
    Assert.assertEquals(result.getOffset(), 0);
    Assert.assertEquals(
        result.getPageSize(), 100); // default page size value (set in ProtoUtils.getPageable)
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

    Assert.assertEquals(result.getSort(), expectedSort);
    Assert.assertEquals(result.getOffset(), page * size);
    Assert.assertEquals(result.getPageSize(), 30);
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

    Assert.assertEquals(
        result.getPageSize(), 1000); // default max page size value (set in ProtoUtils.getPageable)
  }

  /** Parse Sort Tests */
  @Test
  public void parseSortWithEmptyInput() {
    val sort = StringUtils.EMPTY;
    val result = parseSort(sort);

    val expected = Sort.by(new Sort.Order(Sort.Direction.ASC, "createdAt"));
    Assert.assertEquals(result, expected);
  }

  @Test
  public void parseSortWithDirectionOnly() {
    // Test asc
    val sortUp = "asc";
    val sortUpResult = parseSort(sortUp);

    val sortUpExpected = Sort.by(new Sort.Order(Sort.Direction.ASC, "createdAt"));
    Assert.assertEquals(sortUpResult, sortUpExpected);

    // Test desc
    val sortDown = "desc";
    val sortDownResult = parseSort(sortDown);

    val sortDownExpected = Sort.by(new Sort.Order(Sort.Direction.DESC, "createdAt"));
    Assert.assertEquals(sortDownResult, sortDownExpected);
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
    Assert.assertEquals(result, expected);

    // double check spacing variation, trailing commas, empty clauses
    val sortCompact = "id desc,lastLogin,,name asc,";
    val resultCompact = parseSort(sortCompact);
    Assert.assertEquals(resultCompact, expected);
  }
}
