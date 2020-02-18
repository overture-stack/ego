package bio.overture.ego.controller;

import static bio.overture.ego.model.enums.AccessLevel.DENY;
import static bio.overture.ego.model.enums.AccessLevel.READ;
import static bio.overture.ego.model.enums.AccessLevel.WRITE;
import static bio.overture.ego.utils.CollectionUtils.mapToImmutableSet;
import static com.google.common.collect.Maps.newEnumMap;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.AbstractPermission;
import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.entity.NameableEntity;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.utils.web.StringWebResource;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;

@Slf4j
public abstract class AbstractListOwnerPermissionsForPolicyControllerTest<
        O extends NameableEntity<UUID>, P extends AbstractPermission<O>>
    extends AbstractControllerTest {

  /** State */
  protected Map<AccessLevel, List<O>> ownerMap;

  protected List<O> ownersWithRead;
  protected List<O> ownersWithWrite;
  protected List<O> ownersWithDeny;
  protected Policy policyUT;
  private boolean initialized = false;

  @Override
  protected synchronized void beforeTest() {
    // Initial setup of entities (run once)
    if (!initialized) {
      this.ownerMap = newEnumMap(AccessLevel.class);
      this.policyUT = createPolicy("test-policy-" + System.currentTimeMillis());
      this.ownersWithWrite = setupOwners("AOwner Apple", "BOwner Grape", "COwner Orange");
      this.ownersWithRead = setupOwners("AOwner Grape", "BOwner Apple", "COwner Grape");
      this.ownersWithDeny = setupOwners("AOwner Orange", "BOwner Orange", "COwner Apple");
      ownerMap.put(WRITE, ownersWithWrite);
      ownerMap.put(READ, ownersWithRead);
      ownerMap.put(DENY, ownersWithDeny);
      createDenyPermissionsForOwners(policyUT, ownersWithDeny);
      createReadPermissionsForOwners(policyUT, ownersWithRead);
      createWritePermissionsForOwners(policyUT, ownersWithWrite);
      initialized = true;
    }
  }
  /**
   * /policies/{policyId}/owners Test listing owners for a policy without any request params returns
   * all expected owners
   */
  @Test
  public void listUserForPolicy_noParam_Success() {
    val actualOwnerIds =
        getOwnersForPolicyRequest()
            .getAnd()
            .assertOk()
            .transformPageResultsToSet(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds =
        streamAllOwners().map(O::getId).map(UUID::toString).collect(toUnmodifiableSet());
    assertTrue(actualOwnerIds.containsAll(expectedOwnerIds));
  }

  /**
   * /policies/{policyId}/owners?limit=<>&offset=<></> Test listing owners for a policy with only a
   * limit
   */
  @Test
  public void listUserForPolicy_noParamWithLimitOffset_Success() {
    // Assert limit with zero offset
    val actualOwnerIds1 =
        getOwnersForPolicyRequest()
            .queryParam("offset", 0)
            .queryParam("limit", 4)
            .getAnd()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);
    assertEquals(4, actualOwnerIds1.size());

    // Assert non-zero offset
    val actualOwnerIds2 =
        getOwnersForPolicyRequest()
            .queryParam("offset", 2)
            .queryParam("limit", 4)
            .getAnd()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);
    assertEquals(4, actualOwnerIds2.size());
    assertDifferenceHasSize(actualOwnerIds1, actualOwnerIds2, 2);
    assertDifferenceHasSize(actualOwnerIds2, actualOwnerIds1, 2);
    assertIntersectionHasSize(actualOwnerIds1, actualOwnerIds2, 2);
  }

  /**
   * /policies/{policyId}/owners?query=<> Test usage of the query request param for the
   * /policies/{}/<ownerType> endpoint
   */
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

  /** */
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
  public void listPolicyOwners_sortByIdAndDesc_Success() {
    val actualOwnerIds =
        getOwnersForPolicyRequest()
            .queryParam("sort", "id")
            .queryParam("sortOrder", "deSc")
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds =
        streamAllOwners()
            .sorted(buildStringComparator(o -> o.getId().toString(), false, true))
            .map(O::getId)
            .map(UUID::toString)
            .collect(toUnmodifiableList());

    assertEquals(expectedOwnerIds, actualOwnerIds);
  }

  @Test
  public void listPolicyOwners_sortByIdAndDescAndLimitOffset_Success() {
    // Assert limit with zero offset
    val actualOwnerIds1 =
        getOwnersForPolicyRequest()
            .queryParam("sort", "id")
            .queryParam("sortOrder", "deSc")
            .queryParam("offset", 0)
            .queryParam("limit", 6)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds1 =
        streamAllOwners()
            .sorted(buildStringComparator(o -> o.getId().toString(), false, true))
            .map(O::getId)
            .map(UUID::toString)
            .limit(6)
            .collect(toUnmodifiableList());
    assertEquals(expectedOwnerIds1, actualOwnerIds1);

    // Assert non-zero offset
    val actualOwnerIds2 =
        getOwnersForPolicyRequest()
            .queryParam("sort", "id")
            .queryParam("sortOrder", "deSc")
            .queryParam("offset", 2)
            .queryParam("limit", 6)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);
    assertDifferenceHasSize(actualOwnerIds1, actualOwnerIds2, 2);
    assertDifferenceHasSize(actualOwnerIds2, actualOwnerIds1, 2);
    assertIntersectionHasSize(actualOwnerIds2, actualOwnerIds1, 4);
  }

  @Test
  public void listPolicyOwners_sortByIdAndAsc_Success() {
    val actualOwnerIds =
        getOwnersForPolicyRequest()
            .queryParam("sort", "id")
            .queryParam("sortOrder", "asC")
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds =
        streamAllOwners()
            .sorted(buildStringComparator(o -> o.getId().toString(), true, true))
            .map(O::getId)
            .map(UUID::toString)
            .collect(toUnmodifiableList());

    assertEquals(expectedOwnerIds, actualOwnerIds);
  }

  @Test
  public void listPolicyOwners_sortByIdAndAscAndLimitOffset_Success() {
    // Assert limit with zero offset
    val actualOwnerIds1 =
        getOwnersForPolicyRequest()
            .queryParam("sort", "id")
            .queryParam("sortOrder", "asC")
            .queryParam("offset", 0)
            .queryParam("limit", 8)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds1 =
        streamAllOwners()
            .sorted(buildStringComparator(o -> o.getId().toString(), true, true))
            .map(O::getId)
            .map(UUID::toString)
            .limit(8)
            .collect(toUnmodifiableList());
    assertEquals(expectedOwnerIds1, actualOwnerIds1);

    // Assert non-zero offset
    val actualOwnerIds2 =
        getOwnersForPolicyRequest()
            .queryParam("sort", "id")
            .queryParam("sortOrder", "asC")
            .queryParam("offset", 4)
            .queryParam("limit", 8)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);
    assertEquals(5, actualOwnerIds2.size());
    assertDifferenceHasSize(actualOwnerIds1, actualOwnerIds2, 4);
    assertDifferenceHasSize(actualOwnerIds2, actualOwnerIds1, 1);
    assertIntersectionHasSize(actualOwnerIds1, actualOwnerIds2, 4);
  }

  @Test
  public void listPolicyOwners_sortByMaskAndDesc_Success() {
    val actualOwnerIds =
        getOwnersForPolicyRequest()
            .queryParam("sort", "mask")
            .queryParam("sortOrder", "deSC")
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val totalSize = actualOwnerIds.size();
    val actualFirstSetOwnerIds = Set.copyOf(actualOwnerIds.subList(0, totalSize / 3));
    val actualSecondSetOwnerIds =
        Set.copyOf(actualOwnerIds.subList(totalSize / 3, 2 * (totalSize / 3)));
    val actualThirdSetOwnerIds = Set.copyOf(actualOwnerIds.subList(2 * (totalSize / 3), totalSize));

    val expectedFirstSetOwnerIds = mapToImmutableSet(ownersWithDeny, x -> x.getId().toString());
    val expectedSecondSetOwnerIds = mapToImmutableSet(ownersWithWrite, x -> x.getId().toString());
    val expectedThirdSetOwnerIds = mapToImmutableSet(ownersWithRead, x -> x.getId().toString());

    assertEquals(expectedFirstSetOwnerIds, actualFirstSetOwnerIds);
    assertEquals(expectedSecondSetOwnerIds, actualSecondSetOwnerIds);
    assertEquals(expectedThirdSetOwnerIds, actualThirdSetOwnerIds);
  }

  @Test
  public void listPolicyOwners_sortByMaskAndDescAndLimitOffset_Success() {
    // Assert limit with zero offset
    val actualOwnerIds1 =
        getOwnersForPolicyRequest()
            .queryParam("sort", "mask")
            .queryParam("sortOrder", "deSC")
            .queryParam("offset", 0)
            .queryParam("limit", 5)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val totalSize = actualOwnerIds1.size();
    assertEquals(5, totalSize);
    val actualFirstSetOwnerIds = Set.copyOf(actualOwnerIds1.subList(0, 3));
    val actualSecondSetOwnerIds = Set.copyOf(actualOwnerIds1.subList(3, 5));

    assertEquals(3, actualFirstSetOwnerIds.size());
    assertEquals(2, actualSecondSetOwnerIds.size());

    val expectedOwnerWithDenyUserIds = mapToImmutableSet(ownersWithDeny, x -> x.getId().toString());
    val expectedOwnerWithWriteUserIds =
        mapToImmutableSet(ownersWithWrite, x -> x.getId().toString());
    assertEquals(expectedOwnerWithDenyUserIds, actualFirstSetOwnerIds);
    assertTrue(expectedOwnerWithWriteUserIds.containsAll(actualSecondSetOwnerIds));

    // Assert non-zero offset
    val actualOwnerIds2 =
        getOwnersForPolicyRequest()
            .queryParam("sort", "mask")
            .queryParam("sortOrder", "deSC")
            .queryParam("offset", 2)
            .queryParam("limit", 5)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);
    assertEquals(5, actualOwnerIds2.size());
    assertDifferenceHasSize(actualOwnerIds1, actualOwnerIds2, 2);
    assertDifferenceHasSize(actualOwnerIds2, actualOwnerIds1, 2);
    assertIntersectionHasSize(actualOwnerIds1, actualOwnerIds2, 3);
  }

  @Test
  public void listPolicyOwners_sortByMaskAndAsc_Success() {
    val actualOwnerIds =
        getOwnersForPolicyRequest()
            .queryParam("sort", "mask")
            .queryParam("sortOrder", "ASC")
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val totalSize = actualOwnerIds.size();
    val actualFirstSetOwnerIds = Set.copyOf(actualOwnerIds.subList(0, totalSize / 3));
    val actualSecondSetOwnerIds =
        Set.copyOf(actualOwnerIds.subList(totalSize / 3, 2 * (totalSize / 3)));
    val actualThirdSetOwnerIds = Set.copyOf(actualOwnerIds.subList(2 * (totalSize / 3), totalSize));

    val expectedFirstSetOwnerIds = mapToImmutableSet(ownersWithRead, x -> x.getId().toString());
    val expectedSecondSetOwnerIds = mapToImmutableSet(ownersWithWrite, x -> x.getId().toString());
    val expectedThirdSetOwnerIds = mapToImmutableSet(ownersWithDeny, x -> x.getId().toString());

    assertEquals(expectedFirstSetOwnerIds, actualFirstSetOwnerIds);
    assertEquals(expectedSecondSetOwnerIds, actualSecondSetOwnerIds);
    assertEquals(expectedThirdSetOwnerIds, actualThirdSetOwnerIds);
  }

  @Test
  public void listPolicyOwners_sortByMaskAndAscAndLimitOffset_Success() {
    // Assert limit with zero offset
    val actualOwnerIds1 =
        getOwnersForPolicyRequest()
            .queryParam("sort", "mask")
            .queryParam("sortOrder", "ASC")
            .queryParam("offset", 0)
            .queryParam("limit", 2)
            .getAnd()
            .assertOk()
            .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val totalSize = actualOwnerIds1.size();
    assertEquals(2, totalSize);
    val actualFirstSetOwnerIds = Set.copyOf(actualOwnerIds1.subList(0, totalSize));

    val expectedOwnerWithReadUserIds = mapToImmutableSet(ownersWithRead, o -> o.getId().toString());
    assertTrue(expectedOwnerWithReadUserIds.containsAll(actualFirstSetOwnerIds));

    // Assert non-zero offset
    val actualOwnerIds2 =
        getOwnersForPolicyRequest()
            .queryParam("sort", "mask")
            .queryParam("sortOrder", "ASC")
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

  /** Necessary abstract methods for a generic abstract test */
  protected abstract List<O> setupOwners(String... names);

  private void createPermissionsForOwners(
      UUID policyId, AccessLevel mask, Collection<? extends Identifiable<UUID>> owners) {
    owners.forEach(
        u ->
            initStringRequest()
                .endpoint(getAddPermissionsEndpoint(u.getId()))
                .body(List.of(PermissionRequest.builder().mask(mask).policyId(policyId).build()))
                .postAnd()
                .assertOk());
  }

  // Endpoints
  protected abstract String getAddPermissionsEndpoint(UUID ownerId);

  protected abstract String getOwnersForPolicyEndpoint(UUID policyId);

  protected abstract Policy createPolicy(String name);

  private Stream<O> streamAllOwners() {
    return Stream.of(ownersWithDeny, ownersWithRead, ownersWithWrite).flatMap(Collection::stream);
  }

  private StringWebResource getOwnersForPolicyRequest() {
    return initStringRequest().endpoint(getOwnersForPolicyEndpoint(policyUT.getId()));
  }

  private void createWritePermissionsForOwners(Policy p, Collection<O> owners) {
    createPermissionsForOwners(p.getId(), WRITE, owners);
  }

  private void createReadPermissionsForOwners(Policy p, Collection<O> owners) {
    createPermissionsForOwners(p.getId(), READ, owners);
  }

  private void createDenyPermissionsForOwners(Policy p, Collection<O> owners) {
    createPermissionsForOwners(p.getId(), DENY, owners);
  }

  private static <T> Comparator<T> buildStringComparator(
      Function<T, String> function, boolean asc, boolean ignoreCase) {
    return comparing(
        function,
        (x, y) -> {
          val left = ignoreCase ? x.toLowerCase() : x;
          val right = ignoreCase ? y.toLowerCase() : y;
          return asc ? left.compareTo(right) : right.compareTo(left);
        });
  }
}
