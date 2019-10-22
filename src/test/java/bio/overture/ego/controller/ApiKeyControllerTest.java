package bio.overture.ego.controller;

import static bio.overture.ego.model.enums.AccessLevel.DENY;
import static bio.overture.ego.model.enums.AccessLevel.READ;
import static bio.overture.ego.model.enums.AccessLevel.WRITE;
import static java.util.Arrays.asList;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.junit.Assert.*;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.PermissionRequest;
import bio.overture.ego.service.PolicyService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.service.UserPermissionService;
import bio.overture.ego.service.UserService;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.TestData;
import java.util.UUID;
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
  public void issueTokenShouldRevokeRedundantTokens() {
    val user = entityGenerator.setupUser("Test User");
    val userId = user.getId();
    val standByUser = entityGenerator.setupUser("Test User2");
    entityGenerator.setupPolicies("aws,no-be-used", "collab,no-be-used");
    entityGenerator.addPermissions(user, entityGenerator.getScopes("aws.READ", "collab.READ"));

    val tokenRevoke =
        entityGenerator.setupToken(
            user, "token 1", false, 1000, "", entityGenerator.getScopes("collab.READ", "aws.READ"));

    val otherToken =
        entityGenerator.setupToken(
            standByUser,
            "token not be affected",
            false,
            1000,
            "",
            entityGenerator.getScopes("collab.READ", "aws.READ"));

    val otherToken2 =
        entityGenerator.setupToken(
            user,
            "token 2 not be affected",
            false,
            1000,
            "",
            entityGenerator.getScopes("collab.READ"));

    assertFalse(tokenService.getById(tokenRevoke.getId()).isRevoked());
    assertFalse(tokenService.getById(otherToken.getId()).isRevoked());
    assertFalse(tokenService.getById(otherToken2.getId()).isRevoked());

    val scopes = "collab.READ,aws.READ";
    val params = new LinkedMultiValueMap<String, Object>();
    params.add("user_id", userId.toString());
    params.add("scopes", scopes);
    params.add("description", DESCRIPTION);
    super.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    val response = initStringRequest().endpoint("o/api_key").body(params).post();
    val responseStatus = response.getStatusCode();

    assertEquals(responseStatus, HttpStatus.OK);
    assertTrue(tokenService.getById(tokenRevoke.getId()).isRevoked());
    assertFalse(tokenService.getById(otherToken.getId()).isRevoked());
    assertFalse(tokenService.getById(otherToken2.getId()).isRevoked());
  }

  @SneakyThrows
  @Test
  public void issueTokenExactScope() {
    // if scopes are exactly the same as user scopes, issue token should be successful,

    val user = userService.getByName("FirstUser@domain.com");
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
  public void issueTokenWithExcessiveScope() {
    // If token has scopes that user doesn't, token won't be issued.

    val user = userService.getByName("SecondUser@domain.com");
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
  public void issueTokenForLimitedScopes() {
    // if scopes are subset of user scopes, issue token should be successful

    val user = userService.getByName("UserTwo@domain.com");
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

    val user = userService.getByName("UserOne@domain.com");
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
  public void issueTokenForInvalidUser() {
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
  public void checkRevokedToken() {
    val user = userService.getByName("UserThree@domain.com");
    val tokenName = "601044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val scopes = test.getScopes("song.WRITE", "id.WRITE", "portal.WRITE");
    entityGenerator.setupToken(user, tokenName, true, 1000, "test token", scopes);

    val params = new LinkedMultiValueMap<String, Object>();
    params.add("apiKey", tokenName);
    super.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    super.getHeaders().set("Authorization", test.songAuth);

    val response = initStringRequest().endpoint("o/check_api_key").body(params).post();

    val statusCode = response.getStatusCode();
    assertEquals(statusCode, HttpStatus.UNAUTHORIZED);
  }

  @SneakyThrows
  @Test
  public void checkValidToken() {
    val user = userService.getByName("UserThree@domain.com");
    val tokenName = "501044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val scopes = test.getScopes("song.WRITE", "id.WRITE", "portal.WRITE");
    entityGenerator.setupToken(user, tokenName, false, 1000, "test token", scopes);

    val params = new LinkedMultiValueMap<String, Object>();
    params.add("apiKey", tokenName);
    super.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    super.getHeaders().set("Authorization", test.songAuth);

    val response = initStringRequest().endpoint("o/check_api_key").body(params).post();

    val statusCode = response.getStatusCode();
    assertEquals(statusCode, HttpStatus.MULTI_STATUS);
  }

  @SneakyThrows
  @Test
  public void checkInvalidToken() {
    val randomToken = UUID.randomUUID().toString();
    val params = new LinkedMultiValueMap<String, Object>();
    params.add("apiKey", randomToken);

    super.getHeaders().setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    super.getHeaders().set("Authorization", test.songAuth);

    val response = initStringRequest().endpoint("o/check_api_key").body(params).post();

    val statusCode = response.getStatusCode();
    assertEquals(statusCode, HttpStatus.UNAUTHORIZED);
  }

  @SneakyThrows
  @Test
  public void getUserScope() {
    val user = userService.getByName("ThirdUser@domain.com");
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
    assertEquals(statusCode, HttpStatus.NOT_FOUND);
  }

  @SneakyThrows
  @Test
  public void listToken() {
    val user = entityGenerator.setupUser("List Token");
    val userId = user.getId().toString();

    val tokenString1 = "791044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val tokenString2 = "891044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val tokenString3 = "491044a1-3ffd-4164-a6a0-0e1e666b28dc";

    val scopes1 = test.getScopes("song.READ");
    val scopes2 = test.getScopes("collab.READ");
    val scopes3 = test.getScopes("id.WRITE");

    entityGenerator.setupToken(user, tokenString1, false, 1000, "test token 1", scopes1);
    entityGenerator.setupToken(user, tokenString2, false, 1000, "test token 2", scopes2);
    entityGenerator.setupToken(user, tokenString3, true, 1000, "revoked token 3", scopes3);

    val response = initStringRequest().endpoint("o/api_key?user_id=%s", userId).get();

    val statusCode = response.getStatusCode();
    assertEquals(statusCode, HttpStatus.OK);

    // Result should only have unrevoked tokens, ignoring the "exp" field.
    val expected =
        "[{\"accessToken\":\"891044a1-3ffd-4164-a6a0-0e1e666b28dc\","
            + "\"scope\":[\"collab.READ\"],"
            + "\"exp\":\"${json-unit.ignore}\","
            + "\"description\":\"test token 2\"},"
            + "{\"accessToken\":\"791044a1-3ffd-4164-a6a0-0e1e666b28dc\","
            + "\"scope\":[\"song.READ\"],"
            + "\"exp\":\"${json-unit.ignore}\","
            + "\"description\":\"test token 1\"}]";
    assertThatJson(response.getBody()).when(IGNORING_ARRAY_ORDER).isEqualTo(expected);
  }

  @SneakyThrows
  @Test
  public void listTokenEmptyToken() {
    val userId = test.adminUser.getId().toString();
    val response = initStringRequest().endpoint("o/api_key?user_id=%s", userId).get();

    val statusCode = response.getStatusCode();
    assertEquals(statusCode, HttpStatus.OK);
    assertEquals(response.getBody(), "[]");
  }

  @SneakyThrows
  @Test
  public void tokenShouldHaveNonZeroExpiry() {
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

    assertEquals(responseStatus, HttpStatus.OK);

    val listResponse =
        initStringRequest().endpoint("o/api_key?user_id=%s", user.getId().toString()).get();
    val listStatusCode = listResponse.getStatusCode();
    assertEquals(listStatusCode, HttpStatus.OK);

    log.info(listResponse.getBody());
    val responseJson = MAPPER.readTree(listResponse.getBody());
    val exp = responseJson.get(0).get("exp").asInt();
    assertTrue(exp != 0);
    assertTrue(exp > 0);
  }
}
