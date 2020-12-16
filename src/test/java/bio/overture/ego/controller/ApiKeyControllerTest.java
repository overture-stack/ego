package bio.overture.ego.controller;

import static bio.overture.ego.model.enums.AccessLevel.DENY;
import static bio.overture.ego.model.enums.AccessLevel.READ;
import static bio.overture.ego.model.enums.AccessLevel.WRITE;
import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static bio.overture.ego.model.enums.UserType.ADMIN;
import static java.util.Arrays.asList;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.Assert.*;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.*;
import bio.overture.ego.repository.TokenStoreRepository;
import bio.overture.ego.service.PolicyService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.service.UserPermissionService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.TestData;
import java.text.SimpleDateFormat;
import java.util.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApiKeyControllerTest extends AbstractControllerTest {

  @Autowired private PolicyService policyService;

  @Autowired private UserService userService;

  @Autowired private UserPermissionService userPermissionService;

  @Autowired private EntityGenerator entityGenerator;

  @Autowired private TokenService tokenService;

  @Autowired private TokenStoreRepository tokenStoreRepository;

  @Value("${logging.test.controller.enable}")
  private boolean enableLogging;

  private TestData test;

  private final String DESCRIPTION = "This is a Test Token";

  @Override
  protected boolean enableLogging() {
    return enableLogging;
  }

  @Override
  protected void beforeTest() {
    entityGenerator.setupTestUsers();
    entityGenerator.setupTestPolicies();
    test = new TestData(entityGenerator);
  }

  @Test
  public void issueApiKeyShouldRevokeRedundantApiKeys() {
    val user = entityGenerator.setupUser("Test User");
    val userId = user.getId();
    val standByUser = entityGenerator.setupUser("Test User2");
    entityGenerator.setupPolicies("aws-no-be-used", "collab-no-be-used");
    entityGenerator.addPermissions(user, entityGenerator.getScopes("aws.READ", "collab.READ"));

    val apiKeyRevoke =
        entityGenerator.setupApiKey(
            user, "token 1", false, 1000, "", entityGenerator.getScopes("collab.READ", "aws.READ"));

    val otherApiKey =
        entityGenerator.setupApiKey(
            standByUser,
            "apiKey not be affected",
            false,
            1000,
            "",
            entityGenerator.getScopes("collab.READ", "aws.READ"));

    val otherApiKey2 =
        entityGenerator.setupApiKey(
            user,
            "apiKey 2 not be affected",
            false,
            1000,
            "",
            entityGenerator.getScopes("collab.READ"));

    assertFalse(tokenService.getById(apiKeyRevoke.getId()).isRevoked());
    assertFalse(tokenService.getById(otherApiKey.getId()).isRevoked());
    assertFalse(tokenService.getById(otherApiKey2.getId()).isRevoked());

    val scopes = "collab.READ,aws.READ";
    val params = new LinkedMultiValueMap<String, Object>();
    params.add("user_id", userId.toString());
    params.add("scopes", scopes);
    params.add("description", DESCRIPTION);
    super.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    val response = initStringRequest().endpoint("o/api_key").body(params).post();
    val responseStatus = response.getStatusCode();

    assertEquals(responseStatus, HttpStatus.OK);
    assertTrue(tokenService.getById(apiKeyRevoke.getId()).isRevoked());
    assertFalse(tokenService.getById(otherApiKey.getId()).isRevoked());
    assertFalse(tokenService.getById(otherApiKey2.getId()).isRevoked());
  }

  @SneakyThrows
  @Test
  public void issueApiKeyExactScope() {
    // if scopes are exactly the same as user scopes, issue api key should be successful,

    val user = entityGenerator.setupUser("First User");
    val userId = user.getId();
    val study001 = policyService.getByName("Study001");
    val study001id = study001.getId();
    val study002 = policyService.getByName("Study002");
    val study002id = study002.getId();
    val study003 = policyService.getByName("Study003");
    val study003id = study003.getId();

    val permissions =
        asList(
            new PermissionRequest(study001id, READ),
            new PermissionRequest(study002id, WRITE),
            new PermissionRequest(study003id, DENY));

    userPermissionService.addPermissions(user.getId(), permissions);

    val scopes = "Study001.READ,Study002.WRITE";
    val params = new LinkedMultiValueMap<String, Object>();
    params.add("user_id", userId.toString());
    params.add("scopes", scopes);
    params.add("description", DESCRIPTION);

    super.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    val response = initStringRequest().endpoint("o/api_key").body(params).post();
    val statusCode = response.getStatusCode();

    assertEquals(statusCode, HttpStatus.OK);
    assertThatJson(response.getBody())
        .when(IGNORING_ARRAY_ORDER)
        .node("scope")
        .isEqualTo("[\"Study002.WRITE\",\"Study001.READ\"]")
        .node("description")
        .isEqualTo(DESCRIPTION);
  }

  @SneakyThrows
  @Test
  public void issueApiKeyWithExcessiveScope() {
    // If api key has scopes that user doesn't, api key won't be issued.

    val user = entityGenerator.setupUser("Second User");
    val userId = user.getId();
    val study001 = policyService.getByName("Study001");
    val study001id = study001.getId();
    val study002 = policyService.getByName("Study002");
    val study002id = study002.getId();

    val permissions =
        asList(new PermissionRequest(study001id, READ), new PermissionRequest(study002id, READ));

    userPermissionService.addPermissions(user.getId(), permissions);

    val scopes = "Study001.WRITE,Study002.WRITE";
    val params = new LinkedMultiValueMap<String, Object>();
    params.add("user_id", userId.toString());
    params.add("scopes", scopes);
    params.add("description", DESCRIPTION);

    super.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    val response = initStringRequest().endpoint("o/api_key").body(params).post();
    val statusCode = response.getStatusCode();
    assertEquals(statusCode, HttpStatus.INTERNAL_SERVER_ERROR);

    val jsonResponse = MAPPER.readTree(response.getBody());
    assertEquals(
        jsonResponse.get("error").asText(), HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
  }

  @SneakyThrows
  @Test
  public void issueApiKeyForLimitedScopes() {
    // if scopes are subset of user scopes, issue api key should be successful

    val user = entityGenerator.setupUser("User Two");
    val userId = user.getId();

    val study001 = policyService.getByName("Study001");
    val study001id = study001.getId();

    val study002 = policyService.getByName("Study002");
    val study002id = study002.getId();

    val study003 = policyService.getByName("Study003");
    val study003id = study003.getId();

    val permissions =
        asList(
            new PermissionRequest(study001id, READ),
            new PermissionRequest(study002id, WRITE),
            new PermissionRequest(study003id, READ));

    userPermissionService.addPermissions(user.getId(), permissions);

    val scopes = "Study001.READ,Study002.WRITE";
    val params = new LinkedMultiValueMap<String, Object>();
    params.add("user_id", userId.toString());
    params.add("scopes", scopes);
    params.add("description", DESCRIPTION);

    super.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    val response = initStringRequest().endpoint("o/api_key").body(params).post();
    val statusCode = response.getStatusCode();

    assertEquals(statusCode, HttpStatus.OK);
    assertThatJson(response.getBody())
        .when(IGNORING_ARRAY_ORDER)
        .node("scope")
        .isEqualTo("[\"Study002.WRITE\",\"Study001.READ\"]")
        .node("description")
        .isEqualTo(DESCRIPTION);
  }

  @SneakyThrows
  @Test
  public void issueTokenForInvalidScope() {
    // If requested scopes don't exist, should get 404

    val user = entityGenerator.setupUser("User One");
    val userId = user.getId();

    val study001 = policyService.getByName("Study001");
    val study001id = study001.getId();

    val study002 = policyService.getByName("Study002");
    val study002id = study002.getId();

    val study003 = policyService.getByName("Study003");
    val study003id = study003.getId();

    val permissions =
        asList(
            new PermissionRequest(study001id, READ),
            new PermissionRequest(study002id, WRITE),
            new PermissionRequest(study003id, READ));

    userPermissionService.addPermissions(user.getId(), permissions);

    val scopes = "Study001.READ,Invalid.WRITE";
    val params = new LinkedMultiValueMap<String, Object>();
    params.add("user_id", userId.toString());
    params.add("scopes", scopes);
    params.add("description", DESCRIPTION);

    super.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    val response = initStringRequest().endpoint("o/api_key").body(params).post();

    val statusCode = response.getStatusCode();
    assertEquals(statusCode, HttpStatus.NOT_FOUND);
    val jsonResponse = MAPPER.readTree(response.getBody());
    assertEquals(jsonResponse.get("error").asText(), HttpStatus.NOT_FOUND.getReasonPhrase());
  }

  @SneakyThrows
  @Test
  public void issueApiKeyForInvalidUser() {
    val userId = UUID.randomUUID();
    val scopes = "Study001.READ,Invalid.WRITE";
    val params = new LinkedMultiValueMap<String, Object>();
    params.add("user_id", userId.toString());
    params.add("scopes", scopes);
    params.add("description", DESCRIPTION);

    super.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    val response = initStringRequest().endpoint("o/api_key").body(params).post();

    val statusCode = response.getStatusCode();
    assertEquals(statusCode, HttpStatus.FORBIDDEN);

    val jsonResponse = MAPPER.readTree(response.getBody());
    assertEquals(jsonResponse.get("error").asText(), HttpStatus.FORBIDDEN.getReasonPhrase());
  }

  @SneakyThrows
  @Test
  public void checkRevokedApiKey() {
    val user = entityGenerator.setupUser("User Three");
    val apiKeyName = "601044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val scopes = test.getScopes("song.WRITE", "id.WRITE", "portal.WRITE");
    entityGenerator.setupApiKey(user, apiKeyName, true, 1000, "test token", scopes);

    val params = new LinkedMultiValueMap<String, Object>();
    params.add("apiKey", apiKeyName);
    super.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    super.getHeaders().set("Authorization", test.songAuth);

    val response = initStringRequest().endpoint("o/check_api_key").body(params).post();

    val statusCode = response.getStatusCode();
    assertEquals(statusCode, HttpStatus.UNAUTHORIZED);
  }

  @SneakyThrows
  @Test
  public void checkValidApiKey() {
    val user = entityGenerator.setupUser("User Three");
    val apiKeyName = "501044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val scopes = test.getScopes("song.WRITE", "id.WRITE", "portal.WRITE");
    entityGenerator.setupApiKey(user, apiKeyName, false, 1000, "test token", scopes);

    val params = new LinkedMultiValueMap<String, Object>();
    params.add("apiKey", apiKeyName);
    super.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    super.getHeaders().set("Authorization", test.songAuth);

    val response = initStringRequest().endpoint("o/check_api_key").body(params).post();

    val statusCode = response.getStatusCode();
    assertEquals(HttpStatus.MULTI_STATUS, statusCode);
  }

  @SneakyThrows
  @Test
  public void checkInvalidApiKey() {
    val randomApiKey = UUID.randomUUID().toString();
    val params = new LinkedMultiValueMap<String, Object>();
    params.add("apiKey", randomApiKey);

    super.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    super.getHeaders().set("Authorization", test.songAuth);

    val response = initStringRequest().endpoint("o/check_api_key").body(params).post();

    val statusCode = response.getStatusCode();
    assertEquals(HttpStatus.UNAUTHORIZED, statusCode);
  }

  @SneakyThrows
  @Test
  public void getUserScope() {
    val user = entityGenerator.setupUser("Third User");
    val userName = "ThirdUser@domain.com";

    val study001 = policyService.getByName("Study001");
    val study001id = study001.getId();

    val study002 = policyService.getByName("Study002");
    val study002id = study002.getId();

    val study003 = policyService.getByName("Study003");
    val study003id = study003.getId();

    val permissions =
        asList(
            new PermissionRequest(study001id, READ),
            new PermissionRequest(study002id, WRITE),
            new PermissionRequest(study003id, DENY));

    userPermissionService.addPermissions(user.getId(), permissions);

    val response = initStringRequest().endpoint("o/scopes?userName=%s", userName).get();

    val statusCode = response.getStatusCode();
    assertEquals(statusCode, HttpStatus.OK);
    assertThatJson(response.getBody())
        .when(IGNORING_ARRAY_ORDER)
        .node("scopes")
        .isEqualTo("[\"Study002.WRITE\",\"Study001.READ\",\"Study003.DENY\"]");
  }

  @SneakyThrows
  @Test
  public void getUserScopeInvalidUserName() {
    val userName = "randomUser@domain.com";
    val response = initStringRequest().endpoint("o/scopes?userName=%s", userName).get();

    val statusCode = response.getStatusCode();
    assertEquals(HttpStatus.NOT_FOUND, statusCode);
  }

  @SneakyThrows
  @Test
  public void listApiKey_findAllQuery_Success() {
    val testUser = entityGenerator.setupUser("List User");
    testUser.setType(ADMIN);
    testUser.setStatus(APPROVED);
    val testUserId = testUser.getId().toString();
    entityGenerator.addPermissions(
        testUser,
        test.getScopes(
            "song.READ", "collab.READ", "id.WRITE", "aws.WRITE", "song2.READ", "portal.READ"));

    val apiKeyResponse1 =
        createApiKeyPostRequestAnd(testUserId, "song.READ", "with scopes 1")
            .extractOneEntity(ApiKeyResponse.class);
    val apiKeyResponse2 =
        createApiKeyPostRequestAnd(testUserId, "id.WRITE", "with scopes 2")
            .extractOneEntity(ApiKeyResponse.class);
    val apiKeyResponse3 =
        createApiKeyPostRequestAnd(testUserId, "aws.WRITE", "with scopes 3")
            .extractOneEntity(ApiKeyResponse.class);
    val apiKeyResponse4 =
        createApiKeyPostRequestAnd(testUserId, "song2.READ", "with scopes 4")
            .extractOneEntity(ApiKeyResponse.class);
    val apiKeyResponse5 =
        createApiKeyPostRequestAnd(testUserId, "portal.READ", "with scopes 5")
            .extractOneEntity(ApiKeyResponse.class);

    val response =
        listApiKeysEndpointAnd()
            .queryParam("user_id", testUserId)
            .queryParam("offset", 0)
            .queryParam("limit", 20)
            .getAnd()
            .assertOk()
            .assertPageResultsOfType(ApiKeyResponse.class)
            .containsAll(
                Set.of(
                    apiKeyResponse1,
                    apiKeyResponse2,
                    apiKeyResponse3,
                    apiKeyResponse4,
                    apiKeyResponse5));

    val responseWithLimit =
        listApiKeysEndpointAnd()
            .queryParam("user_id", testUserId)
            .queryParam("offset", 0)
            .queryParam("limit", 3)
            .getAnd()
            .assertOk();

    responseWithLimit.assertPageResultsOfType(ApiKeyResponse.class).hasSize(3);

    val responseWithLimitJson = MAPPER.readTree(responseWithLimit.getResponse().getBody());
    assertEquals(5, responseWithLimitJson.get("count").asInt());
  }

  @Test
  @SneakyThrows
  public void findApiKey_findSomeQuery_Success() {
    val queryUser = entityGenerator.setupUser("Query User");
    val queryUserId = queryUser.getId().toString();

    entityGenerator.addPermissions(
        queryUser,
        test.getScopes("song.READ", "aws.WRITE", "aws2.WRITE", "song2.READ", "song3.READ"));

    val apiKeyResponse1 =
        createApiKeyPostRequestAnd(queryUserId, "aws.WRITE", "with scopes 3")
            .extractOneEntity(ApiKeyResponse.class);
    val apiKeyResponse2 =
        createApiKeyPostRequestAnd(queryUserId, "song2.READ", "with scopes 4")
            .extractOneEntity(ApiKeyResponse.class);
    val apiKeyResponse3 =
        createApiKeyPostRequestAnd(queryUserId, "song3.READ", "with scopes 5")
            .extractOneEntity(ApiKeyResponse.class);

    listApiKeysEndpointAnd()
        .queryParam("user_id", queryUserId)
        .queryParam("offset", 0)
        .queryParam("limit", 20)
        .queryParam("query", "abcdefghijklmnopqrstuvwxyz")
        .getAnd()
        .assertOk()
        .assertPageResultsOfType(ApiKeyResponse.class)
        .hasSize(0);

    val responseWithResults =
        listApiKeysEndpointAnd()
            .queryParam("user_id", queryUserId)
            .queryParam("offset", 0)
            .queryParam("limit", 20)
            .queryParam("query", apiKeyResponse3.getName())
            .getAnd()
            .assertOk();

    val responseWithResultsJson = MAPPER.readTree(responseWithResults.getResponse().getBody());
    assertEquals(1, responseWithResultsJson.get("resultSet").size());

    responseWithResults.assertPageResultsOfType(ApiKeyResponse.class).contains(apiKeyResponse3);
  }

  @SneakyThrows
  @Test
  public void listApiKeyEmptyApiKey() {
    val userId = test.adminUser.getId().toString();
    val response = initStringRequest().endpoint("o/api_key?user_id=%s", userId).get();

    val statusCode = response.getStatusCode();
    assertEquals(HttpStatus.OK, statusCode);

    assertEquals(response.getBody(), "{\"limit\":20,\"offset\":0,\"count\":0,\"resultSet\":[]}");
  }

  @SneakyThrows
  @Test
  public void apiKeyShouldHaveNonZeroExpiry() {
    val user = entityGenerator.setupUser("NonZero User");
    entityGenerator.setupSinglePolicy("NonZeroExpiryPolicy");
    entityGenerator.addPermissions(user, entityGenerator.getScopes("NonZeroExpiryPolicy.READ"));

    val scopes = "NonZeroExpiryPolicy.READ";
    val params = new LinkedMultiValueMap<String, Object>();
    params.add("user_id", user.getId().toString());
    params.add("scopes", scopes);
    params.add("description", DESCRIPTION);
    super.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    val response = initStringRequest().endpoint("o/api_key").body(params).post();
    val responseStatus = response.getStatusCode();

    assertEquals(HttpStatus.OK, responseStatus);

    val listResponse =
        initStringRequest().endpoint("o/api_key?user_id=%s", user.getId().toString()).get();
    val listStatusCode = listResponse.getStatusCode();
    assertEquals(HttpStatus.OK, responseStatus);

    val responseJson = MAPPER.readTree(listResponse.getBody());

    val expiryDate = responseJson.get("resultSet").get(0).get("expiryDate");
    val date1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse(expiryDate.asText());

    val seconds = date1.getTime() / 1000L - Calendar.getInstance().getTime().getTime() / 1000L;
    val secondsValue = seconds > 0 ? seconds : 0;

    assertNotEquals(0, secondsValue);
    assertTrue(secondsValue > 0);
  }
}
