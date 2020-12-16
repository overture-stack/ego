package bio.overture.ego.controller;

import static bio.overture.ego.model.enums.AccessLevel.DENY;
import static bio.overture.ego.model.enums.AccessLevel.READ;
import static bio.overture.ego.model.enums.AccessLevel.WRITE;
import static bio.overture.ego.utils.CollectionUtils.mapToImmutableSet;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.AbstractPermission;
import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.entity.NameableEntity;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;

@Slf4j
public abstract class AbstractListNameableOwnerPermissionsForPolicyControllerTest<
        O extends NameableEntity<UUID>, P extends AbstractPermission<O>>
    extends AbstractListOwnerPermissionsForPolicyControllerTest<O, P> {

  @Test
  public void listPolicyUsers_nameQuery_Success() {
    // Assert querying of a group of names
    val query1 = "BOwner";
    val actualOwnerIds1 =
        getOwnersForPolicyRequest()
            .queryParam("query", query1)
            .getAnd()
            .assertOk()
            .transformPageResultsToSet(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds1 =
        streamAllOwners()
            .filter(x -> x.getName().toLowerCase().contains(query1.toLowerCase()))
            .map(O::getId)
            .map(UUID::toString)
            .collect(toUnmodifiableSet());
    assertTrue(actualOwnerIds1.containsAll(expectedOwnerIds1));

    // Assert querying of different owner of names
    val query2 = "Grape";
    val actualOwnerIds2 =
        getOwnersForPolicyRequest()
            .queryParam("query", query2)
            .getAnd()
            .assertOk()
            .transformPageResultsToSet(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds2 =
        streamAllOwners()
            .filter(x -> x.getName().toLowerCase().contains(query2.toLowerCase()))
            .map(O::getId)
            .map(UUID::toString)
            .collect(toUnmodifiableSet());
    assertTrue(actualOwnerIds2.containsAll(expectedOwnerIds2));

    // Assert case insensitive querying of the masks
    val query3 = "DeNy";
    val actualOwnerIds3 =
        getOwnersForPolicyRequest()
            .queryParam("query", query3)
            .getAnd()
            .assertOk()
            .transformPageResultsToSet(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds3 = mapToImmutableSet(ownersWithDeny, x -> x.getId().toString());
    assertTrue(actualOwnerIds3.containsAll(expectedOwnerIds3));
  }

  @Test
  public void listPolicyOwners_sortByNameAndDesc_Success() {
    val actualOwnerIds =
        getOwnersForPolicyRequest()
            .queryParam("sort", "name")
            .queryParam("sortOrder", "dEsc")
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds =
        streamAllOwners()
            .sorted(buildStringComparator(O::getName, false, true))
            .map(O::getId)
            .map(UUID::toString)
            .collect(toUnmodifiableList());

    assertEquals(expectedOwnerIds, actualOwnerIds);
  }

  @Test
  public void listPolicyOwners_sortByNameAndDescAndLimitOffset_Success() {
    // Assert limit with zero offset
    val actualOwnerIds1 =
        getOwnersForPolicyRequest()
            .queryParam("sort", "name")
            .queryParam("sortOrder", "dEsc")
            .queryParam("offset", 0)
            .queryParam("limit", 5)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds1 =
        streamAllOwners()
            .sorted(buildStringComparator(O::getName, false, true))
            .map(O::getId)
            .map(UUID::toString)
            .limit(5)
            .collect(toUnmodifiableList());
    assertEquals(expectedOwnerIds1, actualOwnerIds1);

    // Assert non-zero offset
    val actualOwnerIds2 =
        getOwnersForPolicyRequest()
            .queryParam("sort", "name")
            .queryParam("sortOrder", "dEsc")
            .queryParam("offset", 2)
            .queryParam("limit", 5)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);
    assertDifferenceHasSize(actualOwnerIds1, actualOwnerIds2, 2);
    assertDifferenceHasSize(actualOwnerIds2, actualOwnerIds1, 2);
    assertIntersectionHasSize(actualOwnerIds2, actualOwnerIds1, 3);
  }

  @Test
  public void listPolicyOwners_sortByNameAndAsc_Success() {
    val actualOwnerIds =
        getOwnersForPolicyRequest()
            .queryParam("sort", "name")
            .queryParam("sortOrder", "aSc")
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds =
        streamAllOwners()
            .sorted(buildStringComparator(O::getName, true, true))
            .map(O::getId)
            .map(UUID::toString)
            .collect(toUnmodifiableList());

    assertEquals(expectedOwnerIds, actualOwnerIds);
  }

  @Test
  public void listPolicyOwners_sortByNameAndAscAndLimitOffset_Success() {
    // Assert limit with zero offset
    val actualOwnerIds1 =
        getOwnersForPolicyRequest()
            .queryParam("sort", "name")
            .queryParam("sortOrder", "aSc")
            .queryParam("offset", 0)
            .queryParam("limit", 7)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds1 =
        streamAllOwners()
            .sorted(buildStringComparator(O::getName, true, true))
            .map(O::getId)
            .map(UUID::toString)
            .limit(7)
            .collect(toUnmodifiableList());
    assertEquals(expectedOwnerIds1, actualOwnerIds1);

    // Assert non-zero offset
    val actualOwnerIds2 =
        getOwnersForPolicyRequest()
            .queryParam("sort", "name")
            .queryParam("sortOrder", "aSc")
            .queryParam("offset", 2)
            .queryParam("limit", 7)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);
    assertDifferenceHasSize(actualOwnerIds1, actualOwnerIds2, 2);
    assertDifferenceHasSize(actualOwnerIds2, actualOwnerIds1, 2);
    assertIntersectionHasSize(actualOwnerIds2, actualOwnerIds1, 5);
  }

  @Test
  public void listPolicyOwners_queryOwnerAndSortByNameAndDesc_Success() {
    // Assert query for a name returns with correct PolicyResponses
    val actualOwnerIds1 =
        getOwnersForPolicyRequest()
            .queryParam("query", "aowner")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "desc")
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds1 =
        streamAllOwners()
            .filter(o -> o.getName().toLowerCase().contains("aowner"))
            .sorted(buildStringComparator(NameableEntity::getName, false, true))
            .map(O::getId)
            .map(UUID::toString)
            .collect(toUnmodifiableList());
    assertEquals(expectedOwnerIds1, actualOwnerIds1);

    // Assert query for a different name returns with correct PolicyResponses
    val actualOwnerIds2 =
        getOwnersForPolicyRequest()
            .queryParam("query", "apple")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "desc")
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds2 =
        streamAllOwners()
            .filter(o -> o.getName().toLowerCase().contains("apple"))
            .sorted(buildStringComparator(NameableEntity::getName, false, true))
            .map(O::getId)
            .map(UUID::toString)
            .collect(toUnmodifiableList());
    assertEquals(expectedOwnerIds2, actualOwnerIds2);

    // Assert query for a mask value returns with correct PolicyResponses
    val actualOwnerIds3 =
        getOwnersForPolicyRequest()
            .queryParam("query", "write")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "asc")
            .getAnd()
            .assertOk()
            .transformPageResultsToSet(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds3 =
        ownerMap.get(WRITE).stream()
            .map(Identifiable::getId)
            .map(UUID::toString)
            .collect(toUnmodifiableSet());
    assertEquals(expectedOwnerIds3, actualOwnerIds3);
  }

  @Test
  public void listPolicyOwners_queryOwnerAndSortByNameAndDescAndLimit_Success() {
    // Assert query for a name returns with correct PolicyResponses and limit
    val actualOwnerIds1 =
        getOwnersForPolicyRequest()
            .queryParam("query", "aowner")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "desc")
            .queryParam("limit", 2)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds1 =
        streamAllOwners()
            .filter(o -> o.getName().toLowerCase().contains("aowner"))
            .sorted(buildStringComparator(NameableEntity::getName, false, true))
            .map(O::getId)
            .map(UUID::toString)
            .limit(2)
            .collect(toUnmodifiableList());
    assertEquals(expectedOwnerIds1, actualOwnerIds1);

    // Assert query for a different name returns with correct PolicyResponses with limit
    val actualOwnerIds2 =
        getOwnersForPolicyRequest()
            .queryParam("query", "apple")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "desc")
            .queryParam("limit", 2)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds2 =
        streamAllOwners()
            .filter(o -> o.getName().toLowerCase().contains("apple"))
            .sorted(buildStringComparator(NameableEntity::getName, false, true))
            .map(O::getId)
            .map(UUID::toString)
            .limit(2)
            .collect(toUnmodifiableList());
    assertEquals(expectedOwnerIds2, actualOwnerIds2);

    // Assert query for a mask value returns with correct PolicyResponses with limit
    val actualOwnerIds3 =
        getOwnersForPolicyRequest()
            .queryParam("query", "write")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "asc")
            .queryParam("limit", 2)
            .getAnd()
            .assertOk()
            .transformPageResultsToSet(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds3 =
        ownerMap.get(WRITE).stream()
            .map(Identifiable::getId)
            .map(UUID::toString)
            .limit(2)
            .collect(toUnmodifiableSet());
    assertEquals(expectedOwnerIds3, actualOwnerIds3);
  }

  @Test
  public void listPolicyOwners_queryOwnerAndSortByNameAndAsc_Success() {
    // Assert query for a name returns with correct PolicyResponses
    val actualOwnerIds1 =
        getOwnersForPolicyRequest()
            .queryParam("query", "cowner")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "asc")
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds1 =
        streamAllOwners()
            .filter(o -> o.getName().toLowerCase().contains("cowner"))
            .sorted(buildStringComparator(NameableEntity::getName, true, true))
            .map(O::getId)
            .map(UUID::toString)
            .collect(toUnmodifiableList());
    assertEquals(expectedOwnerIds1, actualOwnerIds1);

    // Assert query for a different name returns with correct PolicyResponses
    val actualOwnerIds2 =
        getOwnersForPolicyRequest()
            .queryParam("query", "orange")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "asc")
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds2 =
        streamAllOwners()
            .filter(o -> o.getName().toLowerCase().contains("orange"))
            .sorted(buildStringComparator(NameableEntity::getName, true, true))
            .map(O::getId)
            .map(UUID::toString)
            .collect(toUnmodifiableList());
    assertEquals(expectedOwnerIds2, actualOwnerIds2);

    // Assert query for a mask value returns with correct PolicyResponses
    val actualOwnerIds3 =
        getOwnersForPolicyRequest()
            .queryParam("query", "read")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "asc")
            .getAnd()
            .assertOk()
            .transformPageResultsToSet(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds3 =
        ownerMap.get(READ).stream()
            .map(Identifiable::getId)
            .map(UUID::toString)
            .collect(toUnmodifiableSet());
    assertEquals(expectedOwnerIds3, actualOwnerIds3);
  }

  @Test
  public void listPolicyOwners_queryOwnerAndSortByNameAndAscAndLimit_Success() {
    // Assert query for a name returns with correct PolicyResponses with limit
    val actualOwnerIds1 =
        getOwnersForPolicyRequest()
            .queryParam("query", "cowner")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "asc")
            .queryParam("limit", 2)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds1 =
        streamAllOwners()
            .filter(o -> o.getName().toLowerCase().contains("cowner"))
            .sorted(buildStringComparator(NameableEntity::getName, true, true))
            .map(O::getId)
            .map(UUID::toString)
            .limit(2)
            .collect(toUnmodifiableList());
    assertEquals(expectedOwnerIds1, actualOwnerIds1);

    // Assert query for a different name returns with correct PolicyResponses with limit
    val actualOwnerIds2 =
        getOwnersForPolicyRequest()
            .queryParam("query", "orange")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "asc")
            .queryParam("limit", 1)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds2 =
        streamAllOwners()
            .filter(o -> o.getName().toLowerCase().contains("orange"))
            .sorted(buildStringComparator(NameableEntity::getName, true, true))
            .map(O::getId)
            .map(UUID::toString)
            .limit(1)
            .collect(toUnmodifiableList());
    assertEquals(expectedOwnerIds2, actualOwnerIds2);

    // Assert query for a mask value returns with correct PolicyResponses with limit
    val actualOwnerIds3 =
        getOwnersForPolicyRequest()
            .queryParam("query", "deny")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "asc")
            .queryParam("limit", 1)
            .getAnd()
            .assertOk()
            .transformPageResultsToSet(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds3 =
        ownerMap.get(DENY).stream()
            .map(Identifiable::getId)
            .map(UUID::toString)
            .limit(1)
            .collect(toUnmodifiableSet());
    assertEquals(expectedOwnerIds3, actualOwnerIds3);
  }

  @Test
  public void listPolicyOwners_queryOwnerAndSortByMaskAndAsc_Success() {
    // Assert query for a name returns with correct PolicyResponses
    val actualOwnerIds1 =
        getOwnersForPolicyRequest()
            .queryParam("query", "cowner")
            .queryParam("sort", "mask")
            .queryParam("sortOrder", "asc")
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds1 =
        Stream.of(READ, WRITE, DENY)
            .map(ownerMap::get)
            .flatMap(Collection::stream)
            .filter(x -> x.getName().toLowerCase().contains("cowner"))
            .map(Identifiable::getId)
            .map(UUID::toString)
            .collect(toUnmodifiableList());
    assertEquals(expectedOwnerIds1, actualOwnerIds1);
  }

  @Test
  public void listPolicyOwners_queryOwnerAndSortByMaskAndAscAndLimitOffset_Success() {
    // Assert query for a name returns with correct PolicyResponses
    val actualOwnerIds1 =
        getOwnersForPolicyRequest()
            .queryParam("query", "cowner")
            .queryParam("sort", "mask")
            .queryParam("sortOrder", "asc")
            .queryParam("offset", 0)
            .queryParam("limit", 2)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds1 =
        Stream.of(READ, WRITE, DENY)
            .map(ownerMap::get)
            .flatMap(Collection::stream)
            .filter(x -> x.getName().toLowerCase().contains("cowner"))
            .map(Identifiable::getId)
            .map(UUID::toString)
            .limit(2)
            .collect(toUnmodifiableList());
    assertEquals(expectedOwnerIds1, actualOwnerIds1);

    // Assert non-zero offset
    val actualOwnerIds2 =
        getOwnersForPolicyRequest()
            .queryParam("query", "cowner")
            .queryParam("sort", "mask")
            .queryParam("sortOrder", "asc")
            .queryParam("offset", 1)
            .queryParam("limit", 3)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);
    assertDifferenceHasSize(actualOwnerIds1, actualOwnerIds2, 1);
    assertDifferenceHasSize(actualOwnerIds2, actualOwnerIds1, 1);
    assertIntersectionHasSize(actualOwnerIds1, actualOwnerIds1, 2);
  }

  @Test
  public void listPolicyOwners_queryOwnerAndSortByMaskAndDesc_Success() {
    // Assert query for a name returns with correct PolicyResponses
    val actualOwnerIds1 =
        getOwnersForPolicyRequest()
            .queryParam("query", "aowner")
            .queryParam("sort", "mask")
            .queryParam("sortOrder", "desc")
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds1 =
        Stream.of(DENY, WRITE, READ)
            .map(ownerMap::get)
            .flatMap(Collection::stream)
            .filter(x -> x.getName().toLowerCase().contains("aowner"))
            .map(Identifiable::getId)
            .map(UUID::toString)
            .collect(toUnmodifiableList());
    assertEquals(expectedOwnerIds1, actualOwnerIds1);
  }

  @Test
  public void listPolicyOwners_queryOwnerAndSortByMaskAndDescAndLimitOffset_Success() {
    // Assert query for a name returns with correct PolicyResponses
    val actualOwnerIds1 =
        getOwnersForPolicyRequest()
            .queryParam("query", "aowner")
            .queryParam("sort", "mask")
            .queryParam("sortOrder", "desc")
            .queryParam("offset", 0)
            .queryParam("limit", 2)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds1 =
        Stream.of(DENY, WRITE, READ)
            .map(ownerMap::get)
            .flatMap(Collection::stream)
            .filter(x -> x.getName().toLowerCase().contains("aowner"))
            .map(Identifiable::getId)
            .map(UUID::toString)
            .limit(2)
            .collect(toUnmodifiableList());
    assertEquals(expectedOwnerIds1, actualOwnerIds1);

    // Assert non-zero offset
    val actualOwnerIds2 =
        getOwnersForPolicyRequest()
            .queryParam("query", "aowner")
            .queryParam("sort", "mask")
            .queryParam("sortOrder", "desc")
            .queryParam("offset", 1)
            .queryParam("limit", 2)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);
    assertDifferenceHasSize(actualOwnerIds1, actualOwnerIds2, 1);
    assertDifferenceHasSize(actualOwnerIds2, actualOwnerIds1, 1);
    assertIntersectionHasSize(actualOwnerIds2, actualOwnerIds1, 1);
  }

  @Test
  public void listPolicyOwners_queryOwnerAndSortByNameAndDescAndLimitOffset_Success() {
    // Assert query for a name returns with correct PolicyResponses and limit
    val actualOwnerIds1 =
        getOwnersForPolicyRequest()
            .queryParam("query", "aowner")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "desc")
            .queryParam("offset", 0)
            .queryParam("limit", 2)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    // Assert non-zero offset
    val actualOwnerIds1_offset =
        getOwnersForPolicyRequest()
            .queryParam("query", "aowner")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "desc")
            .queryParam("offset", 1)
            .queryParam("limit", 2)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);
    assertDifferenceHasSize(actualOwnerIds1, actualOwnerIds1_offset, 1);
    assertDifferenceHasSize(actualOwnerIds1_offset, actualOwnerIds1, 1);
    assertIntersectionHasSize(actualOwnerIds1_offset, actualOwnerIds1, 1);

    // Assert query for a different name returns with correct PolicyResponses with limit
    val actualOwnerIds2 =
        getOwnersForPolicyRequest()
            .queryParam("query", "apple")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "desc")
            .queryParam("offset", 0)
            .queryParam("limit", 2)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    // Assert non-zero offset
    val actualOwnerIds2_offset =
        getOwnersForPolicyRequest()
            .queryParam("query", "apple")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "desc")
            .queryParam("offset", 1)
            .queryParam("limit", 3)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);
    assertDifferenceHasSize(actualOwnerIds2, actualOwnerIds2_offset, 1);
    assertDifferenceHasSize(actualOwnerIds2_offset, actualOwnerIds2, 1);
    assertIntersectionHasSize(actualOwnerIds2_offset, actualOwnerIds2, 1);

    // Assert query for a mask value returns with correct PolicyResponses with limit
    val actualOwnerIds3 =
        getOwnersForPolicyRequest()
            .queryParam("query", "write")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "asc")
            .queryParam("offset", 0)
            .queryParam("limit", 2)
            .getAnd()
            .assertOk()
            .transformPageResultsToSet(PolicyResponse.class, PolicyResponse::getId);

    // Assert non-zero offset
    val actualOwnerIds3_offset =
        getOwnersForPolicyRequest()
            .queryParam("query", "write")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "asc")
            .queryParam("offset", 1)
            .queryParam("limit", 1)
            .getAnd()
            .assertOk()
            .transformPageResultsToSet(PolicyResponse.class, PolicyResponse::getId);
    assertDifferenceHasSize(actualOwnerIds3, actualOwnerIds3_offset, 1);
    assertDifferenceHasSize(actualOwnerIds3_offset, actualOwnerIds3, 0);
    assertIntersectionHasSize(actualOwnerIds3_offset, actualOwnerIds3, 1);
  }

  @Test
  public void listPolicyOwners_queryOwnerAndSortByNameAndAscAndLimitOffset_Success() {
    // Assert query for a name returns with correct PolicyResponses with limit
    val actualOwnerIds1 =
        getOwnersForPolicyRequest()
            .queryParam("query", "cowner")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "asc")
            .queryParam("offset", 0)
            .queryParam("limit", 2)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    // Assert non-zero offset
    val actualOwnerIds1_offset =
        getOwnersForPolicyRequest()
            .queryParam("query", "cowner")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "asc")
            .queryParam("offset", 1)
            .queryParam("limit", 2)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);
    assertDifferenceHasSize(actualOwnerIds1, actualOwnerIds1_offset, 1);
    assertDifferenceHasSize(actualOwnerIds1_offset, actualOwnerIds1, 1);
    assertIntersectionHasSize(actualOwnerIds1_offset, actualOwnerIds1, 1);

    // Assert query for a different name returns with correct PolicyResponses with limit
    val actualOwnerIds2 =
        getOwnersForPolicyRequest()
            .queryParam("query", "orange")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "asc")
            .queryParam("offset", 0)
            .queryParam("limit", 1)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    // Assert non-zero offset
    val actualOwnerIds2_offset =
        getOwnersForPolicyRequest()
            .queryParam("query", "orange")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "asc")
            .queryParam("offset", 1)
            .queryParam("limit", 1)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);
    assertDifferenceHasSize(actualOwnerIds2, actualOwnerIds2_offset, 1);
    assertDifferenceHasSize(actualOwnerIds2_offset, actualOwnerIds2, 1);
    assertIntersectionHasSize(actualOwnerIds2_offset, actualOwnerIds2, 0);

    // Assert query for a mask value returns with correct PolicyResponses with limit
    val actualOwnerIds3 =
        getOwnersForPolicyRequest()
            .queryParam("query", "deny")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "asc")
            .queryParam("offset", 0)
            .queryParam("limit", 3)
            .getAnd()
            .assertOk()
            .transformPageResultsToSet(PolicyResponse.class, PolicyResponse::getId);

    // Assert non-zero offset
    val actualOwnerIds3_offset =
        getOwnersForPolicyRequest()
            .queryParam("query", "deny")
            .queryParam("sort", "name")
            .queryParam("sortOrder", "asc")
            .queryParam("offset", 2)
            .queryParam("limit", 3)
            .getAnd()
            .assertOk()
            .transformPageResultsToSet(PolicyResponse.class, PolicyResponse::getId);
    assertDifferenceHasSize(actualOwnerIds3, actualOwnerIds3_offset, 2);
    assertDifferenceHasSize(actualOwnerIds3_offset, actualOwnerIds3, 0);
    assertIntersectionHasSize(actualOwnerIds3_offset, actualOwnerIds3, 1);
  }
}
