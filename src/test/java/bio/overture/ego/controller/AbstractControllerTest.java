package bio.overture.ego.controller;

import bio.overture.ego.model.dto.CreateApplicationRequest;
import bio.overture.ego.model.dto.GroupRequest;
import bio.overture.ego.model.dto.MaskDTO;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.utils.web.ResponseOption;
import bio.overture.ego.utils.web.StringResponseOption;
import bio.overture.ego.utils.web.BasicWebResource;
import bio.overture.ego.utils.web.StringWebResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;

import java.util.Collection;
import java.util.UUID;

import static bio.overture.ego.utils.Converters.convertToIds;
import static bio.overture.ego.utils.Joiners.COMMA;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@Slf4j
public abstract class AbstractControllerTest {

  /** Constants */
  public static final ObjectMapper MAPPER = new ObjectMapper();

  private static final String ACCESS_TOKEN = "TestToken";

  /** Config */

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

  public <T, O extends ResponseOption<T, O>> BasicWebResource<T, O> initRequest(@NonNull Class<T> responseType) {
    return initRequest(responseType, this.headers);
  }

  public <T, O extends ResponseOption<T, O>> BasicWebResource<T, O> initRequest(@NonNull Class<T> responseType, HttpHeaders headers) {
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

  protected StringResponseOption addUsersToGroupPostRequestAnd(UUID groupId, Collection<UUID> userIds) {
    return initStringRequest().endpoint("/groups/%s/users", groupId).body(userIds).postAnd();
  }

  protected StringResponseOption addUsersToGroupPostRequestAnd(Group g, Collection<User> users) {
    val userIds = convertToIds(users);
    return addUsersToGroupPostRequestAnd(g.getId(), userIds);
  }

  protected StringResponseOption getApplicationsForGroupGetRequestAnd(Group g) {
    return initStringRequest().endpoint("/groups/%s/applications", g.getId()).getAnd();
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

  protected StringResponseOption deleteGroupDeleteRequestAnd(Group g) {
    return deleteGroupDeleteRequestAnd(g.getId());
  }

  protected StringResponseOption partialUpdateGroupPutRequestAnd(UUID groupId, GroupRequest updateRequest) {
    return initStringRequest().endpoint("/groups/%s", groupId).body(updateRequest).putAnd();
  }

  protected StringResponseOption getGroupEntityGetRequestAnd(Group g) {
    return initStringRequest().endpoint("/groups/%s", g.getId()).getAnd();
  }

  protected StringResponseOption createGroupPostRequestAnd(GroupRequest g) {
    return initStringRequest().endpoint("/groups").body(g).postAnd();
  }

  protected StringResponseOption getUserEntityGetRequestAnd(User u) {
    return initStringRequest().endpoint("/users/%s", u.getId()).getAnd();
  }

  protected StringResponseOption getApplicationEntityGetRequestAnd(Application a) {
    return initStringRequest().endpoint("/applications/%s", a.getId()).getAnd();
  }

  protected StringResponseOption getPolicyGetRequestAnd(Policy p) {
    return initStringRequest().endpoint("/policies/%s", p.getId()).getAnd();
  }

  protected StringResponseOption getGroupsForUserGetRequestAnd(User u) {
    return initStringRequest().endpoint("/users/%s/groups", u.getId()).getAnd();
  }

  protected StringResponseOption getGroupsForApplicationGetRequestAnd(Application a) {
    return initStringRequest().endpoint("/applications/%s/groups", a.getId()).getAnd();
  }

  protected  StringResponseOption deleteApplicationFromGroupDeleteRequestAnd(Group g, Application a){
    return initStringRequest().endpoint("/groups/%s/applications/%s", g.getId(), a.getId())
        .deleteAnd();
  }

  protected  StringResponseOption deleteApplicationsFromGroupDeleteRequestAnd(Group g, Collection<Application> apps){
    val appIdsToDelete = convertToIds(apps);
    return deleteApplicationsFromGroupDeleteRequestAnd(g.getId(), appIdsToDelete);
  }

  protected  StringResponseOption deleteApplicationsFromGroupDeleteRequestAnd(UUID groupId, Collection<UUID> appIds){
    return initStringRequest()
        .endpoint("/groups/%s/applications/%s", groupId, COMMA.join(appIds))
        .deleteAnd();
  }

  protected StringWebResource listGroupsEndpointAnd() {
    return initStringRequest().endpoint("/groups");
  }



}
