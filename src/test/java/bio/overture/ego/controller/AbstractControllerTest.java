package bio.overture.ego.controller;

import static bio.overture.ego.utils.CollectionUtils.difference;
import static bio.overture.ego.utils.CollectionUtils.intersection;
import static bio.overture.ego.utils.Converters.convertToIds;
import static bio.overture.ego.utils.Joiners.COMMA;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import bio.overture.ego.model.dto.*;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.utils.web.BasicWebResource;
import bio.overture.ego.utils.web.ResponseOption;
import bio.overture.ego.utils.web.StringResponseOption;
import bio.overture.ego.utils.web.StringWebResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;

@Slf4j
public abstract class AbstractControllerTest {

  /** Constants */
  public static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String ACCESS_TOKEN = "TestToken";

  /** State */
  @LocalServerPort private int port;

  private TestRestTemplate restTemplate = new TestRestTemplate();
  @Getter private HttpHeaders headers = new HttpHeaders();

  @Before
  public void setup() {
    headers.add(AUTHORIZATION, "Bearer " + ACCESS_TOKEN);
    headers.setContentType(APPLICATION_JSON);
    beforeTest();
  }

  public static <T> Set<T> assertDifferenceHasSize(
      @NonNull Collection<T> left, @NonNull Collection<T> right, int expectedDifferenceSize) {
    val diff = difference(left, right);
    assertEquals(expectedDifferenceSize, diff.size());
    return diff;
  }

  public static <T> Set<T> assertIntersectionHasSize(
      @NonNull Collection<T> left, @NonNull Collection<T> right, int expectedIntersectionSize) {
    val intersection = intersection(left, right);
    assertEquals(expectedIntersectionSize, intersection.size());
    return intersection;
  }

  /** Additional setup before each test */
  protected abstract void beforeTest();

  protected abstract boolean enableLogging();

  public StringWebResource initStringRequest() {
    val out = initStringRequest(this.headers);
    return enableLogging() ? out.prettyLogging() : out;
  }

  public StringWebResource initStringRequest(HttpHeaders headers) {
    return new StringWebResource(restTemplate, getServerUrl()).headers(headers);
  }

  public <T, O extends ResponseOption<T, O>> BasicWebResource<T, O> initRequest(
      @NonNull Class<T> responseType) {
    return initRequest(responseType, this.headers);
  }

  public <T, O extends ResponseOption<T, O>> BasicWebResource<T, O> initRequest(
      @NonNull Class<T> responseType, HttpHeaders headers) {
    return new BasicWebResource<T, O>(restTemplate, getServerUrl(), responseType).headers(headers);
  }

  public String getServerUrl() {
    return "http://localhost:" + port;
  }

  @SneakyThrows
  protected StringResponseOption addGroupPermissionToGroupPostRequestAnd(
      Group g, Policy p, AccessLevel mask) {
    val body = MaskDTO.builder().mask(mask).build();
    return initStringRequest()
        .endpoint("/policies/%s/permission/group/%s", p.getId(), g.getId())
        .body(body)
        .postAnd();
  }

  protected StringResponseOption addApplicationsToGroupPostRequestAnd(
      Group g, Collection<Application> applications) {
    val appIds = convertToIds(applications);
    return addApplicationsToGroupPostRequestAnd(g.getId(), appIds);
  }

  protected StringResponseOption addApplicationsToGroupPostRequestAnd(
      UUID groupId, Collection<UUID> applicationIds) {
    return initStringRequest()
        .endpoint("/groups/%s/applications", groupId)
        .body(applicationIds)
        .postAnd();
  }

  protected StringResponseOption deleteUsersFromGroupDeleteRequestAnd(
      UUID groupId, Collection<UUID> userIds) {
    return initStringRequest()
        .endpoint("/groups/%s/users/%s", groupId, COMMA.join(userIds))
        .deleteAnd();
  }

  protected StringResponseOption deleteUsersFromGroupDeleteRequestAnd(
      Group g, Collection<User> users) {
    val userIds = convertToIds(users);
    return deleteUsersFromGroupDeleteRequestAnd(g.getId(), userIds);
  }

  protected StringResponseOption createApplicationPostRequestAnd(CreateApplicationRequest r) {
    return initStringRequest().endpoint("/applications").body(r).postAnd();
  }

  protected StringResponseOption getGroupPermissionsForGroupGetRequestAnd(Group g) {
    return initStringRequest().endpoint("/groups/%s/permissions", g.getId()).getAnd();
  }

  protected StringResponseOption addUsersToGroupPostRequestAnd(
      UUID groupId, Collection<UUID> userIds) {
    return initStringRequest().endpoint("/groups/%s/users", groupId).body(userIds).postAnd();
  }

  protected StringResponseOption addUsersToGroupPostRequestAnd(Group g, Collection<User> users) {
    val userIds = convertToIds(users);
    return addUsersToGroupPostRequestAnd(g.getId(), userIds);
  }

  protected StringResponseOption getApplicationsForUserGetRequestAnd(UUID userId) {
    return initStringRequest().endpoint("/users/%s/applications", userId).getAnd();
  }

  protected StringResponseOption getApplicationsForUserGetRequestAnd(User u) {
    return getApplicationsForUserGetRequestAnd(u.getId());
  }

  protected StringWebResource getUsersForApplicationEndpoint(UUID appId) {
    return initStringRequest().endpoint("/applications/%s/users", appId);
  }

  protected StringResponseOption getUsersForApplicationGetRequestAnd(UUID appId) {
    return getUsersForApplicationEndpoint(appId).getAnd();
  }

  protected StringResponseOption getUsersForApplicationGetRequestAnd(Application a) {
    return getUsersForApplicationGetRequestAnd(a.getId());
  }

  protected StringResponseOption getApplicationsForGroupGetRequestAnd(Group g) {
    return initStringRequest().endpoint("/groups/%s/applications", g.getId()).getAnd();
  }

  protected StringResponseOption addGroupsToUserPostRequestAnd(
      UUID userId, Collection<UUID> groupIds) {
    return initStringRequest().endpoint("/users/%s/groups", userId).body(groupIds).postAnd();
  }

  protected StringResponseOption addGroupsToUserPostRequestAnd(User u, Collection<Group> groups) {
    return addGroupsToUserPostRequestAnd(u.getId(), convertToIds(groups));
  }

  protected StringResponseOption addApplicationsToUserPostRequestAnd(
      UUID userId, Collection<UUID> appIds) {
    return initStringRequest().endpoint("/users/%s/applications", userId).body(appIds).postAnd();
  }

  protected StringResponseOption addApplicationsToUserPostRequestAnd(
      User u, Collection<Application> apps) {
    return addApplicationsToUserPostRequestAnd(u.getId(), convertToIds(apps));
  }

  protected StringResponseOption getUsersForGroupGetRequestAnd(UUID groupId) {
    return initStringRequest().endpoint("/groups/%s/users", groupId).getAnd();
  }

  protected StringResponseOption getUsersForGroupGetRequestAnd(Group g) {
    return getUsersForGroupGetRequestAnd(g.getId());
  }

  protected StringResponseOption deleteGroupDeleteRequestAnd(UUID groupId) {
    return initStringRequest().endpoint("/groups/%s", groupId).deleteAnd();
  }

  protected StringResponseOption deleteUserDeleteRequestAnd(UUID userId) {
    return initStringRequest().endpoint("/users/%s", userId).deleteAnd();
  }

  protected StringResponseOption deleteGroupDeleteRequestAnd(Group g) {
    return deleteGroupDeleteRequestAnd(g.getId());
  }

  protected StringResponseOption deleteUserDeleteRequestAnd(User g) {
    return deleteUserDeleteRequestAnd(g.getId());
  }

  protected StringResponseOption partialUpdateUserPatchRequestAnd(
      UUID userId, UpdateUserRequest updateRequest) {
    return initStringRequest().endpoint("/users/%s", userId).body(updateRequest).patchAnd();
  }

  protected StringResponseOption partialUpdateGroupPutRequestAnd(
      UUID groupId, GroupRequest updateRequest) {
    return initStringRequest().endpoint("/groups/%s", groupId).body(updateRequest).putAnd();
  }

  protected StringResponseOption partialUpdateApplicationPutRequestAnd(
      UUID applicationId, UpdateApplicationRequest updateRequest) {
    return initStringRequest()
        .endpoint("/applications/%s", applicationId)
        .body(updateRequest)
        .putAnd();
  }

  protected StringResponseOption getGroupEntityGetRequestAnd(Group g) {
    return initStringRequest().endpoint("/groups/%s", g.getId()).getAnd();
  }

  protected StringResponseOption createGroupPostRequestAnd(GroupRequest g) {
    return initStringRequest().endpoint("/groups").body(g).postAnd();
  }

  protected StringResponseOption createApiKeyPostRequestAnd(
      String userId, String scopes, String description) {
    return initStringRequest()
        .endpoint("/o/api_key")
        .queryParam("user_id", userId)
        .queryParam("scopes", scopes)
        .queryParam("description", description)
        .postAnd();
  }

  protected StringResponseOption getUserEntityGetRequestAnd(UUID userId) {
    return initStringRequest().endpoint("/users/%s", userId).getAnd();
  }

  protected StringResponseOption getUserEntityGetRequestAnd(User u) {
    return getUserEntityGetRequestAnd(u.getId());
  }

  protected StringResponseOption getApplicationEntityGetRequestAnd(UUID appId) {
    return initStringRequest().endpoint("/applications/%s", appId).getAnd();
  }

  protected StringResponseOption getApplicationEntityGetRequestAnd(Application a) {
    return getApplicationEntityGetRequestAnd(a.getId());
  }

  protected StringResponseOption getPolicyGetRequestAnd(Policy p) {
    return initStringRequest().endpoint("/policies/%s", p.getId()).getAnd();
  }

  protected StringResponseOption getGroupsForUserGetRequestAnd(UUID userId) {
    return initStringRequest().endpoint("/users/%s/groups", userId).getAnd();
  }

  protected StringResponseOption getGroupsForUserGetRequestAnd(User u) {
    return getGroupsForUserGetRequestAnd(u.getId());
  }

  protected StringWebResource getGroupsForApplicationEndpoint(UUID appId) {
    return initStringRequest().endpoint("/applications/%s/groups", appId);
  }

  protected StringResponseOption getGroupsForApplicationGetRequestAnd(UUID appId) {
    return getGroupsForApplicationEndpoint(appId).getAnd();
  }

  protected StringResponseOption getGroupsForApplicationGetRequestAnd(Application a) {
    return getGroupsForApplicationGetRequestAnd(a.getId());
  }

  protected StringResponseOption deleteApplicationFromGroupDeleteRequestAnd(
      Group g, Application a) {
    return initStringRequest()
        .endpoint("/groups/%s/applications/%s", g.getId(), a.getId())
        .deleteAnd();
  }

  protected StringResponseOption deleteApplicationDeleteRequestAnd(Application a) {
    return deleteApplicationDeleteRequestAnd(a.getId());
  }

  protected StringResponseOption deleteApplicationDeleteRequestAnd(UUID applicationId) {
    return initStringRequest().endpoint("/applications/%s", applicationId).deleteAnd();
  }

  protected StringResponseOption deleteApplicationsFromGroupDeleteRequestAnd(
      Group g, Collection<Application> apps) {
    val appIdsToDelete = convertToIds(apps);
    return deleteApplicationsFromGroupDeleteRequestAnd(g.getId(), appIdsToDelete);
  }

  protected StringResponseOption deleteApplicationsFromUserDeleteRequestAnd(
      User user, Collection<Application> apps) {
    val appIdsToDelete = convertToIds(apps);
    return deleteApplicationsFromUserDeleteRequestAnd(user.getId(), appIdsToDelete);
  }

  protected StringResponseOption deleteApplicationsFromUserDeleteRequestAnd(
      UUID userId, Collection<UUID> appIds) {
    return initStringRequest()
        .endpoint("/users/%s/applications/%s", userId, COMMA.join(appIds))
        .deleteAnd();
  }

  protected StringResponseOption deleteGroupsFromUserDeleteRequestAnd(
      User u, Collection<Group> groups) {
    val groupIds = convertToIds(groups);
    return deleteGroupsFromUserDeleteRequestAnd(u.getId(), groupIds);
  }

  protected StringResponseOption deleteGroupsFromUserDeleteRequestAnd(
      UUID userId, Collection<UUID> groupIds) {
    return initStringRequest()
        .endpoint("/users/%s/groups/%s", userId, COMMA.join(groupIds))
        .deleteAnd();
  }

  protected StringResponseOption deleteApplicationsFromGroupDeleteRequestAnd(
      UUID groupId, Collection<UUID> appIds) {
    return initStringRequest()
        .endpoint("/groups/%s/applications/%s", groupId, COMMA.join(appIds))
        .deleteAnd();
  }

  protected StringWebResource listUsersEndpointAnd() {
    return initStringRequest().endpoint("/users");
  }

  protected StringWebResource listGroupsEndpointAnd() {
    return initStringRequest().endpoint("/groups");
  }

  protected StringWebResource listApplicationsEndpointAnd() {
    return initStringRequest().endpoint("/applications");
  }

  protected StringWebResource refreshTokenEndpointAnd(String refreshId, HttpHeaders headers) {
    val refreshCookie = String.format("refreshId=%s;", refreshId);
    headers.add("Cookie", refreshCookie);
    return initStringRequest().endpoint("/oauth/refresh").headers(headers);
  }

  protected StringResponseOption createRefreshTokenEndpointAnd(
      String refreshId, HttpHeaders headers) {
    return refreshTokenEndpointAnd(refreshId, headers).postAnd();
  }

  protected StringWebResource egoTokenEndpointAnd(String clientId) {
    return initStringRequest().endpoint(String.format("/oauth/ego-token?client_id=%s", clientId));
  }

  protected StringResponseOption createRefreshTokenOnLoginEndpointAnd(String clientId) {
    return egoTokenEndpointAnd(clientId).postAnd();
  }

  protected StringResponseOption deleteRefreshTokenEndpointAnd(
      String refreshId, HttpHeaders headers) {
    return refreshTokenEndpointAnd(refreshId, headers).deleteAnd();
  }

  protected StringWebResource listApiKeysEndpointAnd() {
    return initStringRequest().endpoint("/o/api_key");
  }

  protected StringResponseOption getApplicationPermissionsForApplicationGetRequestAnd(
      Application application) {
    return initStringRequest()
        .endpoint("/applications/%s/permissions", application.getId())
        .getAnd();
  }

  protected StringResponseOption addApplicationPermissionToApplicationPostRequestAnd(
      Application application, Policy policy, AccessLevel mask) {
    val body = MaskDTO.builder().mask(mask).build();
    return initStringRequest()
        .endpoint("/policies/%s/permission/application/%s", policy.getId(), application.getId())
        .body(body)
        .postAnd();
  }
}
