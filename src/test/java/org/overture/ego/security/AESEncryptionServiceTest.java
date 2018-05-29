package org.overture.ego.security;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class AESEncryptionServiceTest {

  @Autowired
  private AESEncryptionService aesEncryptionService;

  @Test
  public void testEncryptDecrypt() {
    val encrypted = aesEncryptionService.encrypt("testSecretThing");
    val decrypted = aesEncryptionService.decrypt(encrypted);
    assertThat(decrypted).isEqualTo("testSecretThing");
  }
}
