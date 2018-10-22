package org.overture.ego.service;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.overture.ego.utils.EntityGenerator;
import org.springframework.beans.factory.annotation.Autowired;
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
public class TokenStoreServiceTest {
  @Autowired
  private EntityGenerator entityGenerator;
  
  @Autowired
  private TokenStoreService tokenStoreService;

  @Test
  public void testCreate() {
    val entity = entityGenerator.createSampleToken();
    val result = tokenStoreService.create(entity);

    assertThat(result.getToken()).isEqualTo(entity.getToken());

    val found = tokenStoreService.findByTokenString(entity.getToken());
    assertThat(found).isEqualTo(result);
  }
}
