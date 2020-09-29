package bio.overture.ego.controller;

import static bio.overture.ego.controller.AbstractPermissionControllerTest.createMaskJson;
import static bio.overture.ego.model.dto.Scope.explicitScopes;
import static bio.overture.ego.model.enums.AccessLevel.READ;
import static bio.overture.ego.model.enums.AccessLevel.WRITE;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.model.enums.UserType;
import bio.overture.ego.service.AbstractPermissionService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.utils.EntityGenerator;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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

@Slf4j
@ActiveProfiles({"test"})
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserJwtTest extends AbstractControllerTest {

  /** Dependencies */
  @Autowired private TokenService tokenService;

  @Autowired private EntityGenerator entityGenerator;

  private HttpHeaders tokenHeaders = new HttpHeaders();

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
  public void userHasReadPermission_userHasReadScopeForPolicy_Success() {
    val readUser = entityGenerator.setupUser("Read User", UserType.ADMIN);
    val policy = entityGenerator.setupSinglePolicy("song");

    val userId = readUser.getId();
    val policyId = policy.getId();

    // add user read permission
    val r1 =
        initStringRequest()
            .endpoint("/policies/%s/permission/user/%s", policyId, userId)
            .body(createMaskJson(READ.toString()))
            .postAnd()
            .assertOk();

    // get user permissions from endpoint
    val appPermResponse =
        initStringRequest()
            .endpoint("/users/%s/permissions", userId)
            .getAnd()
            .assertOk()
            .assertPageResultHasSize(UserPermission.class, 1)
            .extractPageResults(UserPermission.class);

    val scopes =
        explicitScopes(
                appPermResponse.stream()
                    .map(AbstractPermissionService::buildScope)
                    .collect(toSet()))
            .stream()
            .map(Scope::toString)
            .collect(toSet());

    val expectedScopes = new HashSet<String>(Collections.singletonList("song.READ"));
    assertEquals(expectedScopes, scopes);
  }

  @Test
  @SneakyThrows
  public void userHasWritePermission_userHasReadAndWriteForPolicy_Success() {
    val writeUser = entityGenerator.setupUser("Write User", UserType.ADMIN);
    val policy = entityGenerator.setupSinglePolicy("song");

    val userId = writeUser.getId();
    val policyId = policy.getId();

    // add user read permission
    val r1 =
        initStringRequest()
            .endpoint("/policies/%s/permission/user/%s", policyId, userId)
            .body(createMaskJson(WRITE.toString()))
            .postAnd()
            .assertOk();

    // get user permissions from endpoint
    val appPermResponse =
        initStringRequest()
            .endpoint("/users/%s/permissions", userId)
            .getAnd()
            .assertOk()
            .assertPageResultHasSize(UserPermission.class, 1)
            .extractPageResults(UserPermission.class);

    val scopes =
        explicitScopes(
                appPermResponse.stream()
                    .map(AbstractPermissionService::buildScope)
                    .collect(toSet()))
            .stream()
            .map(Scope::toString)
            .collect(toSet());

    val expectedScopes = new HashSet<String>(Arrays.asList("song.READ", "song.WRITE"));
    assertEquals(expectedScopes, scopes);
  }
}
