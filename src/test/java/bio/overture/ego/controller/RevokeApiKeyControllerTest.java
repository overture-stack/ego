package bio.overture.ego.controller;

import static bio.overture.ego.model.enums.ApplicationType.CLIENT;
import static bio.overture.ego.model.enums.ProviderType.GOOGLE;
import static bio.overture.ego.model.enums.UserType.USER;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
public class RevokeApiKeyControllerTest {

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
  public void revokeAnyApiKeyAsAdminUser() {
    // Admin users can revoke other users' api keys.

    val randomApiKeyName = randomUUID().toString();
    val adminTokenName = randomUUID().toString();
    val scopes = test.getScopes("song.READ");
    val randomScopes = test.getScopes("song.READ");

    val randomApiKey =
        entityGenerator.setupApiKey(
            test.regularUser, randomApiKeyName, false, 1000, "random token", randomScopes);
    entityGenerator.setupApiKey(test.adminUser, adminTokenName, false, 1000, "test token", scopes);

    assertFalse(randomApiKey.isRevoked());

    mockMvc
        .perform(
            MockMvcRequestBuilders.delete("/o/api_key")
                .param("apiKey", randomApiKeyName)
                .header(AUTHORIZATION, ACCESS_TOKEN))
        .andExpect(status().isOk());

    val revokedApiKey =
        tokenService
            .findByApiKeyString(randomApiKeyName)
            .orElseThrow(() -> new InvalidTokenException("ApiKey Not Found!"));
    assertTrue(revokedApiKey.isRevoked());
  }

  @WithMockCustomUser
  @SneakyThrows
  @Test
  public void revokeOwnApiKeyAsAdminUser() {
    // Admin users can revoke their own api keys.

    val apiKeyName = randomUUID().toString();
    val scopes = test.getScopes("song.READ", "collab.READ", "id.WRITE");
    val apiKey =
        entityGenerator.setupApiKey(test.adminUser, apiKeyName, false, 1000, "test token", scopes);

    assertFalse(apiKey.isRevoked());

    mockMvc
        .perform(
            MockMvcRequestBuilders.delete("/o/api_key")
                .param("apiKey", apiKeyName)
                .header(AUTHORIZATION, ACCESS_TOKEN))
        .andExpect(status().isOk());

    val revokedApiKey =
        tokenService
            .findByApiKeyString(apiKeyName)
            .orElseThrow(() -> new InvalidTokenException("ApiKey Not Found!"));
    assertTrue(revokedApiKey.isRevoked());
  }

  @WithMockCustomUser(firstName = "Regular", lastName = "User", type = USER)
  @SneakyThrows
  @Test
  public void revokeAnyApiKeyAsRegularUser() {
    // Regular user cannot revoke other people's api keys

    val apiKeyName = randomUUID().toString();
    val scopes = test.getScopes("id.WRITE");
    val apiKey =
        entityGenerator.setupApiKey(test.user1, apiKeyName, false, 1000, "test token", scopes);

    assertFalse(apiKey.isRevoked());

    mockMvc
        .perform(
            MockMvcRequestBuilders.delete("/o/api_key")
                .param("apiKey", apiKeyName)
                .header(AUTHORIZATION, ACCESS_TOKEN))
        .andExpect(status().isUnauthorized());
    val revokedApiKey =
        tokenService
            .findByApiKeyString(apiKeyName)
            .orElseThrow(() -> new InvalidTokenException("ApiKey Not Found!"));
    assertFalse(revokedApiKey.isRevoked());
  }

  @WithMockCustomUser(
      firstName = "Regular",
      lastName = "User",
      type = USER,
      providerType = GOOGLE,
      providerId = "regularUser0123")
  @SneakyThrows
  @Test
  public void revokeOwnApiKeyAsRegularUser() {
    // Regular users can only revoke api keys that belong to them.

    val apiKeyName = randomUUID().toString();
    val scopes = test.getScopes("song.READ");
    // creating a single mockUser that matches withMockCustomUser because users are not unique on email (previously name field)
    val mockUser = entityGenerator.setupUser("Regular User", USER, "regularUser0123", GOOGLE);
    entityGenerator.addPermissions(mockUser, test.getScopes("song.READ", "collab.READ"));
    val apiKey =
        entityGenerator.setupApiKey(mockUser, apiKeyName, false, 1000, "test token", scopes);

    assertFalse(apiKey.isRevoked());

    mockMvc
        .perform(
            MockMvcRequestBuilders.delete("/o/api_key")
                .param("apiKey", apiKeyName)
                .header(AUTHORIZATION, ACCESS_TOKEN))
        .andExpect(status().isOk());

    val revokedApiKey =
        tokenService
            .findByApiKeyString(apiKeyName)
            .orElseThrow(() -> new InvalidTokenException("ApiKey Not Found!"));
    assertTrue(revokedApiKey.isRevoked());
  }

  @WithMockCustomApplication
  @SneakyThrows
  @Test
  public void revokeAnyTokenAsAdminApp() {
    val apiKeyName = randomUUID().toString();
    val scopes = test.getScopes("song.READ");
    val apiKey =
        entityGenerator.setupApiKey(
            test.regularUser, apiKeyName, false, 1000, "test token", scopes);

    assertFalse(apiKey.isRevoked());

    mockMvc
        .perform(
            MockMvcRequestBuilders.delete("/o/api_key")
                .param("apiKey", apiKeyName)
                .header(AUTHORIZATION, ACCESS_TOKEN))
        .andExpect(status().isOk());

    val revokedApiKey =
        tokenService
            .findByApiKeyString(apiKeyName)
            .orElseThrow(() -> new InvalidTokenException("ApiKey Not Found!"));
    assertTrue(revokedApiKey.isRevoked());
  }

  @WithMockCustomApplication(
      name = "song",
      clientId = "song",
      clientSecret = "La la la!;",
      type = CLIENT)
  @SneakyThrows
  @Test
  public void revokeApiKeyAsClientApp() {
    val apiKeyName = randomUUID().toString();
    val scopes = test.getScopes("song.READ");
    val apiKey =
        entityGenerator.setupApiKey(
            test.regularUser, apiKeyName, false, 1000, "test token", scopes);

    assertFalse(apiKey.isRevoked());

    mockMvc
        .perform(
            MockMvcRequestBuilders.delete("/o/api_key")
                .param("apiKey", apiKeyName)
                .header(AUTHORIZATION, ACCESS_TOKEN))
        .andExpect(status().isBadRequest());

    val revokedApiKey =
        tokenService
            .findByApiKeyString(apiKeyName)
            .orElseThrow(() -> new InvalidTokenException("ApiKey Not Found!"));
    assertFalse(revokedApiKey.isRevoked());
  }
}
