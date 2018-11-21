package bio.overture.ego.service;

import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Token;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.utils.EntityGenerator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class TokenStoreServiceTest {
  @Autowired
  private EntityGenerator entityGenerator;
  
  @Autowired
  private TokenStoreService tokenStoreService;

  @Test
  public void testCreate() {
    val user   = entityGenerator.setupUser("Developer One");
    val token  = "191044a1-3ffd-4164-a6a0-0e1e666b28dc";
    val duration = 3600;

    val scopes = new HashSet<Scope>();
    val p1 = entityGenerator.setupPolicy("policy1,Admin One");
    scopes.add(new Scope(p1, AccessLevel.READ));
    val p2 = entityGenerator.setupPolicy("policy2,Admin One");
    scopes.add(new Scope(p2, AccessLevel.WRITE));

    val applications = new HashSet<Application>();
    val a1 = entityGenerator.setupApplication("id123", "Shhh! Don't tell!");
    applications.add(a1);

    val tokenObject = Token.builder().
        token(token).owner(user).
        applications(applications == null ? new HashSet<>():applications).
        expires(Date.from(Instant.now().plusSeconds(duration))).
        build();
    for (val s:scopes) {
      tokenObject.addScope(s);
    }

    val result = tokenStoreService.create(tokenObject);
    assertThat(result.getToken()).isEqualTo(token);

    val found = tokenStoreService.findByTokenString(token);
    assertThat(found).isEqualTo(result);
  }

}
