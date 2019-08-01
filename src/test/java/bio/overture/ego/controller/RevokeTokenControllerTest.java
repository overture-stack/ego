package bio.overture.ego.controller;

import static bio.overture.ego.model.enums.ApplicationType.CLIENT;
import static bio.overture.ego.model.enums.UserType.USER;
import static java.util.UUID.randomUUID;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bio.overture.ego.AuthorizationServiceMain;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.TestData;
import bio.overture.ego.utils.WithMockCustomApplication;
import bio.overture.ego.utils.WithMockCustomUser;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@Slf4j
@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
    classes = AuthorizationServiceMain.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration
@Transactional
public class RevokeTokenControllerTest {

  @Autowired private TokenService tokenService;

  @Autowired private EntityGenerator entityGenerator;

  @Autowired private WebApplicationContext webApplicationContext;

  private MockMvc mockMvc;

  private TestData test;

  private static final String ACCESS_TOKEN = "TestToken";

  @Before
  public void initTest() {
    test = new TestData(entityGenerator);
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(springSecurity())
            .alwaysDo(print())
            .build();
  }

  @WithMockCustomUser
  @SneakyThrows
  @Test
  public void revokeAnyTokenAsAdminUser() {
    // Admin users can revoke other users' tokens.

    val randomTokenName = randomUUID().toString();
    val adminTokenName = randomUUID().toString();
    val scopes = test.getScopes("song.READ");
    val randomScopes = test.getScopes("song.READ");

    val randomToken =
        entityGenerator.setupToken(
            test.regularUser, randomTokenName, false, 1000, "random token", randomScopes);
    entityGenerator.setupToken(test.adminUser, adminTokenName, false, 1000, "test token", scopes);

    Assert.assertFalse(randomToken.isRevoked());

    mockMvc
        .perform(
            MockMvcRequestBuilders.delete("/o/token")
                .param("token", randomTokenName)
                .header(AUTHORIZATION, ACCESS_TOKEN))
        .andExpect(status().isOk());

    val revokedToken =
        tokenService
            .findByTokenString(randomTokenName)
            .orElseThrow(() -> new InvalidTokenException("Token Not Found!"));
    Assert.assertTrue(revokedToken.isRevoked());
  }

  @WithMockCustomUser
  @SneakyThrows
  @Test
  public void revokeOwnTokenAsAdminUser() {
    // Admin users can revoke their own tokens.

    val tokenName = randomUUID().toString();
    val scopes = test.getScopes("song.READ", "collab.READ", "id.WRITE");
    val token =
        entityGenerator.setupToken(test.adminUser, tokenName, false, 1000, "test token", scopes);

    Assert.assertFalse(token.isRevoked());

    mockMvc
        .perform(
            MockMvcRequestBuilders.delete("/o/token")
                .param("token", tokenName)
                .header(AUTHORIZATION, ACCESS_TOKEN))
        .andExpect(status().isOk());

    val revokedToken =
        tokenService
            .findByTokenString(tokenName)
            .orElseThrow(() -> new InvalidTokenException("Token Not Found!"));
    Assert.assertTrue(revokedToken.isRevoked());
  }

  @WithMockCustomUser(firstName = "Regular", lastName = "User", type = USER)
  @SneakyThrows
  @Test
  public void revokeAnyTokenAsRegularUser() {
    // Regular user cannot revoke other people's token

    val tokenName = randomUUID().toString();
    val scopes = test.getScopes("id.WRITE");
    val token =
        entityGenerator.setupToken(test.user1, tokenName, false, 1000, "test token", scopes);

    Assert.assertFalse(token.isRevoked());

    mockMvc
        .perform(
            MockMvcRequestBuilders.delete("/o/token")
                .param("token", tokenName)
                .header(AUTHORIZATION, ACCESS_TOKEN))
        .andExpect(status().isUnauthorized());
    val revokedToken =
        tokenService
            .findByTokenString(tokenName)
            .orElseThrow(() -> new InvalidTokenException("Token Not Found!"));
    Assert.assertFalse(revokedToken.isRevoked());
  }

  @WithMockCustomUser(firstName = "Regular", lastName = "User", type = USER)
  @SneakyThrows
  @Test
  public void revokeOwnTokenAsRegularUser() {
    // Regular users can only revoke tokens that belong to them.

    val tokenName = randomUUID().toString();
    val scopes = test.getScopes("song.READ");
    val token =
        entityGenerator.setupToken(test.regularUser, tokenName, false, 1000, "test token", scopes);

    Assert.assertFalse(token.isRevoked());

    mockMvc
        .perform(
            MockMvcRequestBuilders.delete("/o/token")
                .param("token", tokenName)
                .header(AUTHORIZATION, ACCESS_TOKEN))
        .andExpect(status().isOk());

    val revokedToken =
        tokenService
            .findByTokenString(tokenName)
            .orElseThrow(() -> new InvalidTokenException("Token Not Found!"));
    Assert.assertTrue(revokedToken.isRevoked());
  }

  @WithMockCustomApplication
  @SneakyThrows
  @Test
  public void revokeAnyTokenAsAdminApp() {
    val tokenName = randomUUID().toString();
    val scopes = test.getScopes("song.READ");
    val token =
        entityGenerator.setupToken(test.regularUser, tokenName, false, 1000, "test token", scopes);

    Assert.assertFalse(token.isRevoked());

    mockMvc
        .perform(
            MockMvcRequestBuilders.delete("/o/token")
                .param("token", tokenName)
                .header(AUTHORIZATION, ACCESS_TOKEN))
        .andExpect(status().isOk());

    val revokedToken =
        tokenService
            .findByTokenString(tokenName)
            .orElseThrow(() -> new InvalidTokenException("Token Not Found!"));
    Assert.assertTrue(revokedToken.isRevoked());
  }

  @WithMockCustomApplication(
      name = "song",
      clientId = "song",
      clientSecret = "La la la!;",
      type = CLIENT)
  @SneakyThrows
  @Test
  public void revokeTokenAsClientApp() {
    val tokenName = randomUUID().toString();
    val scopes = test.getScopes("song.READ");
    val token =
        entityGenerator.setupToken(test.regularUser, tokenName, false, 1000, "test token", scopes);

    Assert.assertFalse(token.isRevoked());

    mockMvc
        .perform(
            MockMvcRequestBuilders.delete("/o/token")
                .param("token", tokenName)
                .header(AUTHORIZATION, ACCESS_TOKEN))
        .andExpect(status().isBadRequest());

    val revokedToken =
        tokenService
            .findByTokenString(tokenName)
            .orElseThrow(() -> new InvalidTokenException("Token Not Found!"));
    Assert.assertFalse(revokedToken.isRevoked());
  }
}
