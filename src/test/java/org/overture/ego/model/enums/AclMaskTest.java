package org.overture.ego.model.enums;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@Transactional
public class AclMaskTest {

  @Test
  public void testFromValue() {
    assertThat(AclMask.fromValue("read")).isEqualByComparingTo(AclMask.READ);
    assertThat(AclMask.fromValue("write")).isEqualByComparingTo(AclMask.WRITE);
    assertThat(AclMask.fromValue("deny")).isEqualByComparingTo(AclMask.DENY);
  }
}
