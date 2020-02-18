package bio.overture.ego.controller;

import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.model.dto.PolicyResponse;
import bio.overture.ego.model.entity.AbstractPermission;
import bio.overture.ego.model.entity.Identifiable;
import bio.overture.ego.model.entity.NameableEntity;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.service.AbstractPermissionService;
import bio.overture.ego.service.NamedService;
import bio.overture.ego.service.PolicyService;
import bio.overture.ego.utils.CollectionUtils;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.Streams;
import bio.overture.ego.utils.web.StringResponseOption;
import bio.overture.ego.utils.web.StringWebResource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static bio.overture.ego.model.enums.AccessLevel.DENY;
import static bio.overture.ego.model.enums.AccessLevel.READ;
import static bio.overture.ego.model.enums.AccessLevel.WRITE;
import static bio.overture.ego.utils.CollectionUtils.mapToImmutableSet;
import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static bio.overture.ego.utils.Collectors.toImmutableList;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentId;
import static bio.overture.ego.utils.EntityGenerator.generateNonExistentName;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newEnumMap;
import static com.google.common.collect.Maps.uniqueIndex;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static java.util.stream.Stream.concat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
public abstract class AbstractPermissionControllerTest2<
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
  protected void beforeTest() {
    // Initial setup of entities (run once)
    synchronized (this){
      if (!initialized)  {
        this.ownerMap = newEnumMap(AccessLevel.class);
        this.policyUT = createPolicy("test-policy-"+System.currentTimeMillis());
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
  }
  /**
   * Test listing owners for a policy without any request params returns all expected owners
   */
  @Test
  public void listUserForPolicy_noParam_Success(){
    val actualOwnerIds = getOwnersForPolicyRequest()
        .getAnd()
        .assertOk()
        .transformPageResultsToSet(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds = streamAllOwners()
        .map(O::getId)
        .map(UUID::toString)
        .collect(toUnmodifiableSet());
    assertTrue(actualOwnerIds.containsAll(expectedOwnerIds));
  }

  @Test
  public void listUserForPolicy_noParamWithLimit_Success(){
    getOwnersForPolicyRequest()
        .queryParam("limit", "4")
        .getAnd()
        .assertPageResultHasSize(PolicyResponse.class, 4);
  }

  /**
   * Test usage of the query request param for the /policies/{}/<ownerType> endpoint
   */
  @Test
  public void listPolicyUsers_nameQuery_Success(){
    // Assert querying of a group of names
    val query1 = "BOwner";
    val actualOwnerIds1 = getOwnersForPolicyRequest()
        .queryParam("query", query1)
        .getAnd()
        .assertOk()
        .transformPageResultsToSet(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds1 = streamAllOwners()
        .filter(x -> x.getName().toLowerCase().contains(query1.toLowerCase()))
        .map(O::getId)
        .map(UUID::toString)
        .collect(toUnmodifiableSet());
    assertTrue(actualOwnerIds1.containsAll(expectedOwnerIds1));


    // Assert querying of different owner of names
    val query2 = "Grape";
    val actualOwnerIds2 = getOwnersForPolicyRequest()
        .queryParam("query", query2)
        .getAnd()
        .assertOk()
        .transformPageResultsToSet(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds2 = streamAllOwners()
        .filter(x -> x.getName().toLowerCase().contains(query2.toLowerCase()))
        .map(O::getId)
        .map(UUID::toString)
        .collect(toUnmodifiableSet());
    assertTrue(actualOwnerIds2.containsAll(expectedOwnerIds2));

    // Assert case insensitive querying of the masks
    val query3 = "DeNy";
    val actualOwnerIds3 = getOwnersForPolicyRequest()
        .queryParam("query", query3)
        .getAnd()
        .assertOk()
        .transformPageResultsToSet(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds3 = mapToImmutableSet(ownersWithDeny, x -> x.getId().toString());
    assertTrue(actualOwnerIds3.containsAll(expectedOwnerIds3));
  }

  @Test
  public void listPolicyOwners_sortByNameAndDesc_Success(){
    val actualOwnerIds = getOwnersForPolicyRequest()
        .queryParam("sort", "name")
        .queryParam("sortOrder", "dEsc")
        .getAnd()
        .assertOk()
        .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds = streamAllOwners()
        .sorted(buildStringComparator(O::getName, false, true))
        .map(O::getId)
        .map(UUID::toString)
        .collect(toUnmodifiableList());

    assertEquals(expectedOwnerIds, actualOwnerIds);
  }

  @Test
  public void listPolicyOwners_sortByNameAndDescAndLimit_Success(){
    val actualOwnerIds = getOwnersForPolicyRequest()
        .queryParam("sort", "name")
        .queryParam("sortOrder", "dEsc")
        .queryParam("limit", 5)
        .getAnd()
        .assertOk()
        .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds = streamAllOwners()
        .sorted(buildStringComparator(O::getName, false, true))
        .map(O::getId)
        .map(UUID::toString)
        .limit(5)
        .collect(toUnmodifiableList());

    assertEquals(expectedOwnerIds, actualOwnerIds);
  }


  @Test
  public void listPolicyOwners_sortByNameAndAsc_Success(){
    val actualOwnerIds = getOwnersForPolicyRequest()
        .queryParam("sort", "name")
        .queryParam("sortOrder", "aSc")
        .getAnd()
        .assertOk()
        .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds = streamAllOwners()
        .sorted(buildStringComparator(O::getName, true, true))
        .map(O::getId)
        .map(UUID::toString)
        .collect(toUnmodifiableList());

    assertEquals(expectedOwnerIds, actualOwnerIds);
  }

  @Test
  public void listPolicyOwners_sortByNameAndAscAndLimit_Success(){
    val actualOwnerIds = getOwnersForPolicyRequest()
        .queryParam("sort", "name")
        .queryParam("sortOrder", "aSc")
        .queryParam("limit", 7)
        .getAnd()
        .assertOk()
        .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds = streamAllOwners()
        .sorted(buildStringComparator(O::getName, true, true))
        .map(O::getId)
        .map(UUID::toString)
        .limit(7)
        .collect(toUnmodifiableList());

    assertEquals(expectedOwnerIds, actualOwnerIds);
  }

  @Test
  public void listPolicyOwners_sortByIdAndDesc_Success(){
    val actualOwnerIds = getOwnersForPolicyRequest()
        .queryParam("sort", "id")
        .queryParam("sortOrder", "deSc")
        .getAnd()
        .assertOk()
        .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds = streamAllOwners()
        .sorted(buildStringComparator(o -> o.getId().toString(), false, true))
        .map(O::getId)
        .map(UUID::toString)
        .collect(toUnmodifiableList());

    assertEquals(expectedOwnerIds, actualOwnerIds);
  }

  @Test
  public void listPolicyOwners_sortByIdAndDescAndLimit_Success(){
    val actualOwnerIds = getOwnersForPolicyRequest()
        .queryParam("sort", "id")
        .queryParam("sortOrder", "deSc")
        .queryParam("limit", 6 )
        .getAnd()
        .assertOk()
        .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds = streamAllOwners()
        .sorted(buildStringComparator(o -> o.getId().toString(), false, true))
        .map(O::getId)
        .map(UUID::toString)
        .limit(6)
        .collect(toUnmodifiableList());

    assertEquals(expectedOwnerIds, actualOwnerIds);
  }

  @Test
  public void listPolicyOwners_sortByIdAndAsc_Success(){
    val actualOwnerIds = getOwnersForPolicyRequest()
        .queryParam("sort", "id")
        .queryParam("sortOrder", "asC")
        .getAnd()
        .assertOk()
        .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds = streamAllOwners()
        .sorted(buildStringComparator(o -> o.getId().toString(), true, true))
        .map(O::getId)
        .map(UUID::toString)
        .collect(toUnmodifiableList());

    assertEquals(expectedOwnerIds, actualOwnerIds);
  }

  @Test
  public void listPolicyOwners_sortByIdAndAscAndLimit_Success(){
    val actualOwnerIds = getOwnersForPolicyRequest()
        .queryParam("sort", "id")
        .queryParam("sortOrder", "asC")
        .queryParam("limit", 8)
        .getAnd()
        .assertOk()
        .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds = streamAllOwners()
        .sorted(buildStringComparator(o -> o.getId().toString(), true, true))
        .map(O::getId)
        .map(UUID::toString)
        .limit(8)
        .collect(toUnmodifiableList());

    assertEquals(expectedOwnerIds, actualOwnerIds);
  }

  @Test
  public void listPolicyOwners_sortByMaskAndDesc_Success(){
    val actualOwnerIds = getOwnersForPolicyRequest()
        .queryParam("sort", "mask")
        .queryParam("sortOrder", "deSC")
        .getAnd()
        .assertOk()
        .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val totalSize = actualOwnerIds.size();
    val actualFirstSetOwnerIds = Set.copyOf(actualOwnerIds.subList(0, totalSize/3));
    val actualSecondSetOwnerIds = Set.copyOf(actualOwnerIds.subList(totalSize/3, 2*(totalSize/3)));
    val actualThirdSetOwnerIds = Set.copyOf(actualOwnerIds.subList(2*(totalSize/3), totalSize));

    val expectedFirstSetOwnerIds = mapToImmutableSet(ownersWithDeny, x -> x.getId().toString());
    val expectedSecondSetOwnerIds = mapToImmutableSet(ownersWithWrite, x -> x.getId().toString());
    val expectedThirdSetOwnerIds = mapToImmutableSet(ownersWithRead, x -> x.getId().toString());

    assertEquals(expectedFirstSetOwnerIds, actualFirstSetOwnerIds);
    assertEquals(expectedSecondSetOwnerIds, actualSecondSetOwnerIds);
    assertEquals(expectedThirdSetOwnerIds, actualThirdSetOwnerIds);
  }

  @Test
  public void listPolicyOwners_sortByMaskAndDescAndLimit_Success(){
    val actualOwnerIds = getOwnersForPolicyRequest()
        .queryParam("sort", "mask")
        .queryParam("sortOrder", "deSC")
        .queryParam("limit", 5)
        .getAnd()
        .assertOk()
        .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val totalSize = actualOwnerIds.size();
    assertEquals(5, totalSize);
    val actualFirstSetOwnerIds = Set.copyOf(actualOwnerIds.subList(0, 3));
    val actualSecondSetOwnerIds = Set.copyOf(actualOwnerIds.subList(3, 5));

    val expectedFirstSetOwnerIds = mapToImmutableSet(ownersWithDeny, x -> x.getId().toString());
    val expectedSecondSetOwnerIds = ownersWithWrite.stream().limit(2)
        .map(Identifiable::getId)
        .map(UUID::toString)
        .collect(toUnmodifiableSet());

    assertEquals(expectedFirstSetOwnerIds, actualFirstSetOwnerIds);
    assertEquals(expectedSecondSetOwnerIds, actualSecondSetOwnerIds);
  }

  @Test
  public void listPolicyOwners_sortByMaskAndAsc_Success(){
    val actualOwnerIds = getOwnersForPolicyRequest()
        .queryParam("sort", "mask")
        .queryParam("sortOrder", "ASC")
        .getAnd()
        .assertOk()
        .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val totalSize = actualOwnerIds.size();
    val actualFirstSetOwnerIds = Set.copyOf(actualOwnerIds.subList(0, totalSize/3));
    val actualSecondSetOwnerIds = Set.copyOf(actualOwnerIds.subList(totalSize/3, 2*(totalSize/3)));
    val actualThirdSetOwnerIds = Set.copyOf(actualOwnerIds.subList(2*(totalSize/3), totalSize));

    val expectedFirstSetOwnerIds = mapToImmutableSet(ownersWithRead, x -> x.getId().toString());
    val expectedSecondSetOwnerIds = mapToImmutableSet(ownersWithWrite, x -> x.getId().toString());
    val expectedThirdSetOwnerIds = mapToImmutableSet(ownersWithDeny, x -> x.getId().toString());

    assertEquals(expectedFirstSetOwnerIds, actualFirstSetOwnerIds);
    assertEquals(expectedSecondSetOwnerIds, actualSecondSetOwnerIds);
    assertEquals(expectedThirdSetOwnerIds, actualThirdSetOwnerIds);
  }

  @Test
  public void listPolicyOwners_sortByMaskAndAscAndLimit_Success(){
    val actualOwnerIds = getOwnersForPolicyRequest()
        .queryParam("sort", "mask")
        .queryParam("sortOrder", "ASC")
        .queryParam("limit", 2)
        .getAnd()
        .assertOk()
        .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val totalSize = actualOwnerIds.size();
    assertEquals(2, totalSize);
    val actualFirstSetOwnerIds = Set.copyOf(actualOwnerIds.subList(0, totalSize));

    val expectedFirstSetOwnerIds = ownersWithRead.stream().limit(2)
        .map(Identifiable::getId)
        .map(UUID::toString)
        .collect(toUnmodifiableSet());

    assertEquals(expectedFirstSetOwnerIds, actualFirstSetOwnerIds);
  }

  @Test
  public void listPolicyOwners_queryOwnerAndSortByNameAndDesc_Success(){
    val actualOwnerIds1 = getOwnersForPolicyRequest()
        .queryParam("query", "aowner")
        .queryParam("sort", "name")
        .queryParam("sortOrder", "desc")
        .getAnd()
        .assertOk()
        .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds1 = streamAllOwners()
        .filter(o -> o.getName().toLowerCase().contains("aowner"))
        .sorted(buildStringComparator(NameableEntity::getName, false, true))
        .map(O::getId)
        .map(UUID::toString)
        .collect(toUnmodifiableList());

    assertEquals(expectedOwnerIds1, actualOwnerIds1);


    val actualOwnerIds2 = getOwnersForPolicyRequest()
        .queryParam("query", "apple")
        .queryParam("sort", "name")
        .queryParam("sortOrder", "desc")
        .getAnd()
        .assertOk()
        .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds2 = streamAllOwners()
        .filter(o -> o.getName().toLowerCase().contains("apple"))
        .sorted(buildStringComparator(NameableEntity::getName, false, true))
        .map(O::getId)
        .map(UUID::toString)
        .collect(toUnmodifiableList());

    assertEquals(expectedOwnerIds2, actualOwnerIds2);
  }

  @Test
  public void listPolicyOwners_queryOwnerAndSortByNameAndAsc_Success(){
    val actualOwnerIds1 = getOwnersForPolicyRequest()
        .queryParam("query", "cowner")
        .queryParam("sort", "name")
        .queryParam("sortOrder", "asc")
        .getAnd()
        .assertOk()
        .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds1 = streamAllOwners()
        .filter(o -> o.getName().toLowerCase().contains("cowner"))
        .sorted(buildStringComparator(NameableEntity::getName, true, true))
        .map(O::getId)
        .map(UUID::toString)
        .collect(toUnmodifiableList());

    assertEquals(expectedOwnerIds1, actualOwnerIds1);

    val actualOwnerIds2 = getOwnersForPolicyRequest()
        .queryParam("query", "orange")
        .queryParam("sort", "name")
        .queryParam("sortOrder", "asc")
        .getAnd()
        .assertOk()
        .transformPageResultsToList(PolicyResponse.class, PolicyResponse::getId);

    val expectedOwnerIds2 = streamAllOwners()
        .filter(o -> o.getName().toLowerCase().contains("orange"))
        .sorted(buildStringComparator(NameableEntity::getName, true, true))
        .map(O::getId)
        .map(UUID::toString)
        .collect(toUnmodifiableList());

    assertEquals(expectedOwnerIds2, actualOwnerIds2);
  }

  /** Necessary abstract methods for a generic abstract test */
  protected abstract List<O> setupOwners(String ... names);

  protected abstract void createPermissionsForOwners(UUID policyId, AccessLevel mask, Collection<O> owners);

  // Owner specific
  protected abstract Class<O> getOwnerType();

  // Permission specific
  protected abstract Class<P> getPermissionType();

  // Endpoints
  protected abstract String getAddPermissionsEndpoint(UUID ownerId);

  protected abstract String getOwnersForPolicyEndpoint(UUID policyId);

  protected abstract Policy createPolicy(String name);

  private Stream<O> streamAllOwners(){
    return Stream.of(ownersWithDeny, ownersWithRead, ownersWithWrite)
        .flatMap(Collection::stream);
  }

  private StringWebResource getAddPermissionRequest(UUID ownerId){
    return initStringRequest()
        .endpoint(getAddPermissionsEndpoint(ownerId));
  }

  private StringWebResource getOwnersForPolicyRequest(){
    return initStringRequest()
        .endpoint(getOwnersForPolicyEndpoint(policyUT.getId()));
  }

  private void createWritePermissionsForOwners(Policy p,
      Collection<O> owners){
    createPermissionsForOwners(p.getId(), WRITE, owners);
  }

  private void createReadPermissionsForOwners(Policy p,
      Collection<O> owners){
    createPermissionsForOwners(p.getId(), READ, owners);
  }

  private void createDenyPermissionsForOwners(Policy p,
      Collection<O> owners){
    createPermissionsForOwners(p.getId(), DENY, owners);
  }

  private static <T> Comparator<T> buildStringComparator(Function<T, String> function, boolean asc, boolean ignoreCase){
    return comparing(function, (x, y) -> {
      val left = ignoreCase ? x.toLowerCase() : x;
      val right = ignoreCase ? y.toLowerCase() : y;
      return asc ? left.compareTo(right) : right.compareTo(left);
    });
  }
}
