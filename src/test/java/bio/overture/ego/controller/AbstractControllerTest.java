package bio.overture.ego.controller;

import bio.overture.ego.model.dto.GroupRequest;
import bio.overture.ego.model.dto.MaskDTO;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.utils.WebResource;
import bio.overture.ego.utils.WebResource.ResponseOption;
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
import org.springframework.http.ResponseEntity;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static bio.overture.ego.utils.Collectors.toImmutableList;
import static bio.overture.ego.utils.Collectors.toImmutableSet;
import static bio.overture.ego.utils.Converters.convertToIds;
import static bio.overture.ego.utils.Joiners.COMMA;
import static bio.overture.ego.utils.Streams.stream;
import static bio.overture.ego.utils.WebResource.createWebResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.OK;
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

  public WebResource<String> initStringRequest() {
    val out = initRequest(String.class);
    return enableLogging() ? out.prettyLogging() : out;
  }

  public WebResource<String> initStringRequest(HttpHeaders headers) {
    return initRequest(String.class, headers);
  }

  public <T> WebResource<T> initRequest(@NonNull Class<T> responseType) {
    return createWebResource(restTemplate, getServerUrl(), responseType).headers(this.headers);
  }

  public <T> WebResource<T> initRequest(@NonNull Class<T> responseType, HttpHeaders headers) {
    return createWebResource(restTemplate, getServerUrl(), responseType).headers(headers);
  }

  public String getServerUrl() {
    return "http://localhost:" + port;
  }

  @SneakyThrows
  protected static <T> List<T> extractPageResultSetFromResponse(ResponseEntity<String> r, Class<T> tClass) {
    assertThat(r.getStatusCode()).isEqualTo(OK);
    assertThat(r.getBody()).isNotNull();
    val page = MAPPER.readTree(r.getBody());
    assertThat(page).isNotNull();
    return stream(page.path("resultSet").iterator())
        .map(x -> MAPPER.convertValue(x, tClass))
        .collect(toImmutableList());
  }

  @SneakyThrows
  protected static <T> T extractOneEntityFromResponse(ResponseEntity<String> r, Class<T> tClass) {
    assertThat(r.getStatusCode()).isEqualTo(OK);
    assertThat(r.getBody()).isNotNull();
    return MAPPER.readValue(r.getBody(), tClass);
  }

  @SneakyThrows
  protected static <T> Set<T> extractManyEntitiesFromResponse(ResponseEntity<String> r, Class<T> tClass) {
    assertThat(r.getStatusCode()).isEqualTo(OK);
    assertThat(r.getBody()).isNotNull();
    return stream(MAPPER.readTree(r.getBody()).iterator())
        .map(x -> MAPPER.convertValue(x, tClass))
        .collect(toImmutableSet());
  }

  @SneakyThrows
  protected ResponseOption<String> addGroupPermissionToGroupPostRequestAnd(
      Group g, Policy p, AccessLevel mask) {
    val body = MaskDTO.builder().mask(mask).build();
    return initStringRequest()
        .endpoint("/policies/%s/permission/group/%s", p.getId(), g.getId())
        .body(body)
        .postAnd();
  }

  protected ResponseOption<String> addApplicationsToGroupPostRequestAnd(
      Group g, Collection<Application> applications) {
    val appIds = convertToIds(applications);
    return initStringRequest().endpoint("/groups/%s/applications", g.getId()).body(appIds).postAnd();
  }

  protected ResponseOption<String> deleteUsersFromGroupDeleteRequestAnd(
      Group g, Collection<User> users) {
    val userIds = convertToIds(users);
    return initStringRequest()
        .endpoint("/groups/%s/users/%s", g.getId(), COMMA.join(userIds))
        .deleteAnd();
  }

  protected ResponseOption<String> getGroupPermissionsForGroupGetRequestAnd(Group g) {
    return initStringRequest().endpoint("/groups/%s/permissions", g.getId()).getAnd();
  }

  protected ResponseOption<String> addUsersToGroupPostRequestAnd(Group g, Collection<User> users) {
    val userIds = convertToIds(users);
    return initStringRequest().endpoint("/groups/%s/users", g.getId()).body(userIds).postAnd();
  }

  protected ResponseOption<String> getApplicationsForGroupGetRequestAnd(Group g) {
    return initStringRequest().endpoint("/groups/%s/applications", g.getId()).getAnd();
  }

  protected ResponseOption<String> getUsersForGroupGetRequestAnd(Group g) {
    return initStringRequest().endpoint("/groups/%s/users", g.getId()).getAnd();
  }

  protected ResponseOption<String> deleteGroupDeleteRequestAnd(Group g) {
    return initStringRequest().endpoint("/groups/%s", g.getId()).deleteAnd();
  }

  protected ResponseOption<String> getGroupEntityGetRequestAnd(Group g) {
    return initStringRequest().endpoint("/groups/%s", g.getId()).getAnd();
  }

  protected ResponseOption<String> createGroupPostRequestAnd(GroupRequest g) {
    return initStringRequest().endpoint("/groups").body(g).postAnd();
  }

  protected ResponseOption<String> getUserEntityGetRequestAnd(User u) {
    return initStringRequest().endpoint("/users/%s", u.getId()).getAnd();
  }

  protected ResponseOption<String> getApplicationGetRequestAnd(Application a) {
    return initStringRequest().endpoint("/applications/%s", a.getId()).getAnd();
  }

  protected ResponseOption<String> getPolicyGetRequestAnd(Policy p) {
    return initStringRequest().endpoint("/policies/%s", p.getId()).getAnd();
  }

  protected ResponseOption<String> getGroupsForUserGetRequestAnd(User u) {
    return initStringRequest().endpoint("/users/%s/group", u.getId()).getAnd();
  }


}
