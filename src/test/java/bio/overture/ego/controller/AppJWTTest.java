package bio.overture.ego.controller;

import static bio.overture.ego.controller.AbstractPermissionControllerTest.createMaskJson;
import static bio.overture.ego.model.enums.AccessLevel.*;
import static bio.overture.ego.model.enums.AccessLevel.READ;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

import bio.overture.ego.AuthorizationServiceMain;
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
            .post();
    assertEquals(r1.getStatusCode(), OK);

    val r2 =
        initStringRequest()
            .endpoint("/policies/%s/permission/application/%s", policyId2, appId)
            .body(createMaskJson(WRITE.toString()))
            .post();
    assertEquals(r2.getStatusCode(), OK);

    val r3 =
        initStringRequest()
            .endpoint("/policies/%s/permission/application/%s", policyId3, appId)
            .body(createMaskJson(DENY.toString()))
            .post();
    assertEquals(r3.getStatusCode(), OK);

    // get app permissions from endpoint
    val appPermReq = initStringRequest().endpoint("/applications/%s/permissions", appId).getAnd();
    appPermReq.assertOk();
    appPermReq.assertPageResultHasSize(ApplicationPermission.class, 3);
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
    val tokenReq =
        initStringRequest().endpoint("/oauth/token").headers(tokenHeaders).body(params).post();
    assertEquals(tokenReq.getStatusCode(), OK);
    val tokenResponse = tokenReq.getBody();
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
    val assocGroupReq =
        initStringRequest().endpoint("/groups/%s/applications", groupId).body(asList(appId)).post();
    assertEquals(assocGroupReq.getStatusCode(), OK);

    val appPerm1 =
        initStringRequest()
            .endpoint("/policies/%s/permission/application/%s", policyId1, appId)
            .body(createMaskJson(READ.toString()))
            .post();

    val appPerm2 =
        initStringRequest()
            .endpoint("/policies/%s/permission/application/%s", policyId2, appId)
            .body(createMaskJson(READ.toString()))
            .post();

    val groupPerm1 =
        initStringRequest()
            .endpoint("/policies/%s/permission/group/%s", policyId1, groupId)
            .body(createMaskJson(WRITE.toString()))
            .post();

    // get app permissions
    val appPermsReq = initStringRequest().endpoint("/applications/%s/permissions", appId).getAnd();
    appPermsReq.assertOk();
    appPermsReq.assertPageResultHasSize(ApplicationPermission.class, 2);

    // get group permissions
    val groupPermsReq = initStringRequest().endpoint("/groups/%s/permissions", groupId).getAnd();
    groupPermsReq.assertOk();
    groupPermsReq.assertPageResultHasSize(GroupPermission.class, 1);

    // get resolved permissions for app
    val resolvedAppPermsReq =
        initStringRequest().endpoint("/applications/%s/groups/permissions", appId).getAnd();
    resolvedAppPermsReq.assertOk();

    val responseBody = resolvedAppPermsReq.getResponse().getBody();
    assertNotNull(responseBody);
    val responseJson = MAPPER.readTree(responseBody);

    // assert there are only 2 resolved permissions because of overlap
    assertTrue(responseJson.isArray());
    assertEquals(responseJson.size(), 2);

    val resolvedScopes =
        Streams.stream(responseJson)
            .map(
                x -> {
                  // this logic assumes result can be only App or Group permission type
                  val owner = x.get("ownerType").asText();
                  if (owner.equalsIgnoreCase("group")) {
                    return gson.fromJson(x.toString(), GroupPermission.class);
                  } else {
                    return gson.fromJson(x.toString(), ApplicationPermission.class);
                  }
                })
            .map(AbstractPermissionService::buildScope)
            .map(Scope::toString)
            .collect(toSet());

    val params = new LinkedMultiValueMap<String, Object>();
    params.add("grant_type", "client_credentials");
    params.add("client_id", app.getClientId());
    params.add("client_secret", app.getClientSecret());

    // get app jwt scopes
    val tokenReq =
        initStringRequest().endpoint("/oauth/token").headers(tokenHeaders).body(params).post();
    assertEquals(tokenReq.getStatusCode(), OK);
    val tokenResponse = tokenReq.getBody();
    val tokenJson = MAPPER.readTree(tokenResponse);
    val accessToken = tokenJson.get("access_token").asText();
    tokenService.isValidToken(accessToken);
    val accessTokenScope = tokenService.getAppAccessToken(accessToken).getScope();

    // assert jwt scope matches scopes from resolved permissions
    assertEquals(resolvedScopes, accessTokenScope);
  }
}
