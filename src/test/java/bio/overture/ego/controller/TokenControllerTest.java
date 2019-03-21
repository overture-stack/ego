package bio.overture.ego.controller;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.utils.EntityGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(
        classes = AuthorizationServiceMain.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TokenControllerTest {

  /** Constants */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** State */
  @LocalServerPort private int port;

  private TestRestTemplate restTemplate = new TestRestTemplate();
  private HttpHeaders headers = new HttpHeaders();

  /** Dependencies */
  @Autowired private EntityGenerator entityGenerator;

  @Autowired private TokenService tokenService;

  private String createURLWithPort(String uri) {
    return "http://localhost:" + port + uri;
  }

  @Before
  public void setup() {
    headers.add("Authorization", "Bearer TestToken");
    headers.setContentType(MediaType.APPLICATION_JSON);
  }

  @Test
  public void issueTokenShouldRevokeRedundantTokens() {
    val user = entityGenerator.setupUser("Test User");
    val standByUser = entityGenerator.setupUser("Test User2");
    entityGenerator.setupPolicies("aws,no-be-used", "collab,no-be-used");
    entityGenerator.addPermissions(user, entityGenerator.getScopes("aws.READ", "collab.READ"));


    val tokenRevoke = entityGenerator.setupToken(user,
            "token 1",
            1000,
            entityGenerator.getScopes("collab.READ", "aws.READ"));

    val otherToken = entityGenerator.setupToken(standByUser,
            "token not be affected",
            1000,
            entityGenerator.getScopes("collab.READ", "aws.READ"));

    val otherToken2 = entityGenerator.setupToken(user,
            "token 2 not be affected",
            1000,
            entityGenerator.getScopes("collab.READ"));


    assertThat(tokenService.getById(tokenRevoke.getId()).isRevoked()).isFalse();
    assertThat(tokenService.getById(otherToken.getId()).isRevoked()).isFalse();
    assertThat(tokenService.getById(otherToken2.getId()).isRevoked()).isFalse();

    val entity = new HttpEntity<>(null, headers);
    val response =
            restTemplate.exchange(createURLWithPort("/o/token?user_id={userId}&scopes=collab.READ&scopes=aws.READ"), HttpMethod.POST, entity, String.class, user.getId().toString());
    val responseStatus = response.getStatusCode();
    assertThat(responseStatus).isEqualTo(HttpStatus.OK);

    assertThat(tokenService.getById(tokenRevoke.getId()).isRevoked()).isTrue();
    assertThat(tokenService.getById(otherToken.getId()).isRevoked()).isFalse();
    assertThat(tokenService.getById(otherToken2.getId()).isRevoked()).isFalse();
  }

}
