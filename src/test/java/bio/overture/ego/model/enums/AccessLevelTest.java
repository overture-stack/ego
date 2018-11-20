package bio.overture.ego.model.enums;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static bio.overture.ego.model.enums.AccessLevel.READ;
import static bio.overture.ego.model.enums.AccessLevel.WRITE;
import static bio.overture.ego.model.enums.AccessLevel.DENY;
@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class AccessLevelTest {
  @Test
  public void testFromValue() {
    assertThat(AccessLevel.fromValue("read")).isEqualByComparingTo(AccessLevel.READ);
    assertThat(AccessLevel.fromValue("write")).isEqualByComparingTo(AccessLevel.WRITE);
    assertThat(AccessLevel.fromValue("deny")).isEqualByComparingTo(AccessLevel.DENY);
  }

  @Test
  public void testAllows() {
    allows(READ,  READ);
    allows(WRITE, READ);
    denies(DENY,  READ);

    denies(READ,  WRITE);
    allows(WRITE, WRITE);
    denies(DENY,  WRITE);

    denies(READ,  DENY);
    denies(WRITE, DENY);
    denies(DENY,  DENY);
  }

  public void allows(AccessLevel have, AccessLevel want) {
    assertTrue(AccessLevel.allows(have, want));
  }

  public void denies(AccessLevel have, AccessLevel want) {
    assertFalse(AccessLevel.allows(have, want));
  }
}
