package bio.overture.ego.token;

import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.dto.TokenResponse;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Token;
import bio.overture.ego.model.exceptions.NotFoundException;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.utils.EntityGenerator;
import bio.overture.ego.utils.TestData;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static org.junit.Assert.assertTrue;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@Transactional
@ActiveProfiles("test")
public class ListTokenTest {

  public static TestData test = null;
  @Autowired private EntityGenerator entityGenerator;
  @Autowired private TokenService tokenService;
  @Rule public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    test = new TestData(entityGenerator);
  }

  @Test
  public void testListToken() {
    val tokenString1 = "791044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val tokenString2 = "891044a1-3ffd-4164-a6a0-0e1e666b28dc";

    val scopes1 = test.getScopes("song.WRITE", "id.WRITE");
    val scopes2 = test.getScopes("song.READ", "id.READ");

    Set<String> scopeString1 = mapToSet(scopes1, Scope::toString);
    Set<String> scopeString2 = mapToSet(scopes2, Scope::toString);

    val applications = new HashSet<Application>();
    applications.add(test.score);

    val userToken1 =
        entityGenerator.setupToken(test.regularUser, tokenString1, 1000, scopes1, applications);
    userToken1.setDescription("Test token 1.");
    val userToken2 =
        entityGenerator.setupToken(test.regularUser, tokenString2, 1000, scopes2, applications);
    userToken2.setDescription("Test token 2.");

    Set<Token> tokens = new HashSet<>();
    tokens.add(userToken1);
    tokens.add(userToken2);
    test.regularUser.setTokens(tokens);

    val responseList = tokenService.listToken(test.regularUser.getId());

    List<TokenResponse> expected = new ArrayList<>();
    expected.add(
        TokenResponse.builder()
            .accessToken(tokenString1)
            .scope(scopeString1)
            .exp(userToken1.getSecondsUntilExpiry())
            .description( "Test token 1.")
            .build()
    );
    expected.add(
        TokenResponse.builder()
            .accessToken(tokenString2)
            .scope(scopeString2)
            .exp(userToken2.getSecondsUntilExpiry())
            .description( "Test token 2.")
            .build()
    );

    assertTrue((responseList.stream().allMatch(expected::contains)));
  }

  @Test
  public void testEmptyTokenList() {
    exception.expect(NotFoundException.class);
    exception.expectMessage("User is not associated with any token.");
    tokenService.listToken(test.regularUser.getId());
  }
}
