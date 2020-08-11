package bio.overture.ego.controller;

import static bio.overture.ego.controller.AbstractPermissionControllerTest.createMaskJson;
import static bio.overture.ego.model.enums.AccessLevel.*;
import static bio.overture.ego.model.enums.AccessLevel.READ;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.ResolvedPermissionResponse;
import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.entity.ApplicationPermission;
import bio.overture.ego.model.entity.GroupPermission;
import bio.overture.ego.model.enums.ApplicationType;
import bio.overture.ego.service.*;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.Streams;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;

@Slf4j
@ActiveProfiles({"test"})
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AppJWTTest extends AbstractControllerTest {

  /** Dependencies */
  @Autowired private TokenService tokenService;

  @Autowired private EntityGenerator entityGenerator;

  private HttpHeaders tokenHeaders = new HttpHeaders();
  private Gson gson = new Gson();

  @Value("${logging.test.controller.enable}")
  private boolean enableLogging;

  @Override
  protected boolean enableLogging() {
    return enableLogging;
  }

  @Override
  protected void beforeTest() {
    val adminUser = entityGenerator.setupUser("Admin App");
    val bearerToken = tokenService.generateUserToken(adminUser);
    tokenHeaders.add(AUTHORIZATION, "Bearer " + bearerToken);
    tokenHeaders.setContentType(APPLICATION_FORM_URLENCODED);
  }

  @Test
  @SneakyThrows
  public void applicationPermsOnly_appJwtHasAllResolvedScopes_Success() {
    val app = entityGenerator.setupApplication("TestApp", "testsecret", ApplicationType.CLIENT);
    val policies = entityGenerator.setupPolicies("SONG", "SCORE", "DACO");

    val appId = app.getId();
    val policyId1 = policies.get(0).getId();
    val policyId2 = policies.get(1).getId();
    val policyId3 = policies.get(2).getId();

    // add app permissions
    val r1 =
        initStringRequest()
            .endpoint("/policies/%s/permission/application/%s", policyId1, appId)
            .body(createMaskJson(READ.toString()))
            .postAnd()
            .assertOk();

    val r2 =
        initStringRequest()
            .endpoint("/policies/%s/permission/application/%s", policyId2, appId)
            .body(createMaskJson(WRITE.toString()))
            .postAnd()
            .assertOk();

    val r3 =
        initStringRequest()
            .endpoint("/policies/%s/permission/application/%s", policyId3, appId)
            .body(createMaskJson(DENY.toString()))
            .postAnd()
            .assertOk();

    // get app permissions from endpoint
    val appPermReq =
        initStringRequest()
            .endpoint("/applications/%s/permissions", appId)
            .getAnd()
            .assertOk()
            .assertPageResultHasSize(ApplicationPermission.class, 3);

    val responseBody = appPermReq.getResponse().getBody();
    assertNotNull(responseBody);
    val responseJson = MAPPER.readTree(responseBody);
    val results = responseJson.get("resultSet");

    assertEquals(responseJson.get("count").asInt(), 3);
    assertTrue(results.isArray());

    val scopes =
        Streams.stream(results)
            .map(x -> gson.fromJson(x.toString(), ApplicationPermission.class))
            .map(AbstractPermissionService::buildScope)
            .map(Scope::toString)
            .collect(toSet());

    val params = new LinkedMultiValueMap<String, Object>();
    params.add("grant_type", "client_credentials");
    params.add("client_id", app.getClientId());
    params.add("client_secret", app.getClientSecret());

    // get app jwt scopes
    val tokenResponse =
        initStringRequest()
            .endpoint("/oauth/token")
            .headers(tokenHeaders)
            .body(params)
            .postAnd()
            .assertOk()
            .assertHasBody()
            .getResponse()
            .getBody();

    val tokenJson = MAPPER.readTree(tokenResponse);
    val accessToken = tokenJson.get("access_token").asText();
    tokenService.isValidToken(accessToken);
    val accessTokenScope = tokenService.getAppAccessToken(accessToken).getScope();

    // assert jwt scope matches scopes from app permissions
    assertEquals(scopes, accessTokenScope);
  }

  @Test
  @SneakyThrows
  public void applicationAndGroupPerms_appJwtHasAllResolvedScopes_Success() {
    val app =
        entityGenerator.setupApplication("TestCombinedApp", "testsecret", ApplicationType.CLIENT);
    val group = entityGenerator.setupGroup("Test Group");
    val policies = entityGenerator.setupPolicies("SONG", "SCORE");

    val appId = app.getId();
    val groupId = group.getId();
    val policyId1 = policies.get(0).getId();
    val policyId2 = policies.get(1).getId();

    // associate app with group
    initStringRequest()
        .endpoint("/groups/%s/applications", groupId)
        .body(asList(appId))
        .postAnd()
        .assertOk();

    initStringRequest()
        .endpoint("/policies/%s/permission/application/%s", policyId1, appId)
        .body(createMaskJson(READ.toString()))
        .postAnd()
        .assertOk();

    initStringRequest()
        .endpoint("/policies/%s/permission/application/%s", policyId2, appId)
        .body(createMaskJson(READ.toString()))
        .postAnd()
        .assertOk();

    initStringRequest()
        .endpoint("/policies/%s/permission/group/%s", policyId1, groupId)
        .body(createMaskJson(WRITE.toString()))
        .postAnd()
        .assertOk();

    // get app permissions
    initStringRequest()
        .endpoint("/applications/%s/permissions", appId)
        .getAnd()
        .assertOk()
        .assertPageResultHasSize(ApplicationPermission.class, 2);

    // get group permissions
    initStringRequest()
        .endpoint("/groups/%s/permissions", groupId)
        .getAnd()
        .assertOk()
        .assertPageResultHasSize(GroupPermission.class, 1);

    // get resolved permissions for app
    val resolvedPerms =
        initStringRequest()
            .endpoint("/applications/%s/groups/permissions", appId)
            .getAnd()
            .assertOk()
            .assertHasBody()
            .extractManyEntities(ResolvedPermissionResponse.class);

    // assert there are only 2 resolved permissions because of overlap
    assertNotNull(resolvedPerms);
    assertEquals(resolvedPerms.size(), 2);

    val resolvedScopes =
        resolvedPerms.stream()
            .map(
                x -> {
                  val acl = x.getAccessLevel();
                  val policy = x.getPolicy();
                  return Scope.createScope(policy, acl).toString();
                })
            .collect(toSet());

    val params = new LinkedMultiValueMap<String, Object>();
    params.add("grant_type", "client_credentials");
    params.add("client_id", app.getClientId());
    params.add("client_secret", app.getClientSecret());

    // get app jwt scopes
    val tokenResponse =
        initStringRequest()
            .endpoint("/oauth/token")
            .headers(tokenHeaders)
            .body(params)
            .postAnd()
            .assertOk()
            .assertHasBody()
            .getResponse()
            .getBody();

    val tokenJson = MAPPER.readTree(tokenResponse);
    val accessToken = tokenJson.get("access_token").asText();
    tokenService.isValidToken(accessToken);
    val accessTokenScope = tokenService.getAppAccessToken(accessToken).getScope();

    // assert jwt scope matches scopes from resolved permissions
    assertEquals(resolvedScopes, accessTokenScope);
  }
}
