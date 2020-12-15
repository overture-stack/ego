package bio.overture.ego.service;

import static org.junit.Assert.*;

import bio.overture.ego.model.enums.ProviderType;
import bio.overture.ego.utils.EntityGenerator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
public class DefaultProviderServiceTest {

  @Autowired DefaultProviderService defaultProviderService;
  @Autowired private EntityGenerator entityGenerator;

  @Value("${spring.flyway.placeholders.default_provider}")
  private ProviderType configuredDefaultProvider;

  @Test
  public void defaultProviderConfigured_Success() {
    assertNotNull(configuredDefaultProvider);
    val defaultProviderResult = defaultProviderService.findById(configuredDefaultProvider);
    assertTrue(defaultProviderResult.isPresent());
    assertEquals(defaultProviderResult.get().getId(), configuredDefaultProvider);
  }

  @Test
  public void defaultProviderConfigured_Failure() {
    assertNotNull(configuredDefaultProvider);
    val mockedDefaultProvider =
        entityGenerator.randomEnumExcluding(ProviderType.class, configuredDefaultProvider);
    val provider = defaultProviderService.findById(mockedDefaultProvider);
    assertTrue(provider.isEmpty());
    assertNotEquals(provider, configuredDefaultProvider);
  }
}
