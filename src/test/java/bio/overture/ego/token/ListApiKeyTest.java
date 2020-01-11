package bio.overture.ego.token;

import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static org.junit.Assert.assertTrue;

import bio.overture.ego.model.dto.ApiKeyResponse;
import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.entity.ApiKey;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.TestData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@Transactional
@ActiveProfiles("test")
@Ignore
public class ListApiKeyTest {

  public static TestData test = null;
  @Autowired private EntityGenerator entityGenerator;
  @Autowired private TokenService tokenService;
  @Rule public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    test = new TestData(entityGenerator);
  }

  @Test
  public void testListApiKey() {
    val apiKeyString1 = "791044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val apiKeyString2 = "891044a1-3ffd-4164-a6a0-0e1e666b28dc";

    val scopes1 = test.getScopes("song.WRITE", "id.WRITE");
    val scopes2 = test.getScopes("song.READ", "id.READ");

    Set<String> scopeString1 = mapToSet(scopes1, Scope::toString);
    Set<String> scopeString2 = mapToSet(scopes2, Scope::toString);

    val userToken1 =
        entityGenerator.setupApiKey(
            test.regularUser, apiKeyString1, false, 1000, "Test token 1.", scopes1);
    val userToken2 =
        entityGenerator.setupApiKey(
            test.regularUser, apiKeyString2, false, 1000, "Test token 2.", scopes2);

    Set<ApiKey> apiKeys = new HashSet<>();
    apiKeys.add(userToken1);
    apiKeys.add(userToken2);
    test.regularUser.setTokens(apiKeys);

    val responseList = tokenService.listApiKey(test.regularUser.getId());

    List<ApiKeyResponse> expected = new ArrayList<>();
    expected.add(
        ApiKeyResponse.builder()
            .name(apiKeyString1)
            .scope(scopeString1)
            .exp(userToken1.getExpiryDate())
            .iss(userToken1.getIssueDate())
            .isRevoked(userToken1.isRevoked())
            .description("Test token 1.")
            .build());
    expected.add(
        ApiKeyResponse.builder()
            .name(apiKeyString2)
            .scope(scopeString2)
            .exp(userToken2.getExpiryDate())
            .iss(userToken2.getIssueDate())
            .isRevoked(userToken2.isRevoked())
            .description("Test token 2.")
            .build());

    assertTrue(responseList.stream().allMatch(expected::contains));
  }

  @Test
  public void testEmptyTokenList() {
    val apiKeys = tokenService.listApiKey(test.regularUser.getId());
    assertTrue(apiKeys.isEmpty());
  }
}
