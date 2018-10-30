package org.overture.ego.model.enums;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.overture.ego.model.enums.PolicyMask;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.overture.ego.model.enums.PolicyMask.READ;
import static org.overture.ego.model.enums.PolicyMask.WRITE;
import static org.overture.ego.model.enums.PolicyMask.DENY;
@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class PolicyMaskTest {
  @Test
  public void testFromValue() {
    assertThat(PolicyMask.fromValue("read")).isEqualByComparingTo(PolicyMask.READ);
    assertThat(PolicyMask.fromValue("write")).isEqualByComparingTo(PolicyMask.WRITE);
    assertThat(PolicyMask.fromValue("deny")).isEqualByComparingTo(PolicyMask.DENY);
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

  public void allows(PolicyMask have, PolicyMask want) {
    assertTrue(PolicyMask.allows(have, want));
  }

  public void denies(PolicyMask have, PolicyMask want) {
    assertFalse(PolicyMask.allows(have, want));
  }
}
